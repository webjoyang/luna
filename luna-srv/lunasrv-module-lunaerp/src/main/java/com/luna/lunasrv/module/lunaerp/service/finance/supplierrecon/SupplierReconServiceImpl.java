package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.LineType;
import com.luna.lunasrv.module.lunaerp.enums.finance.PricingBasis;
import com.luna.lunasrv.module.lunaerp.enums.finance.ReconStatus;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier.SupplierService;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 供应商对账单 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class SupplierReconServiceImpl implements SupplierReconService {

    private static final String RECON_NO_PREFIX = "SR";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 10;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    @Resource
    private SupplierReconMapper supplierReconMapper;

    @Resource
    private SupplierReconItemMapper supplierReconItemMapper;

    @Resource
    private SupplierReconSourceService supplierReconSourceService;

    @Resource
    private SupplierService supplierService;

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

    @Override
    public List<SupplierReconSourceItemRespVO> preview(SupplierReconPreviewReqVO reqVO) {
        if (reqVO == null || reqVO.getSupplierId() == null) {
            throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
        }
        SupplierRespVO supplier = supplierService.getSupplier(reqVO.getSupplierId());
        if (supplier == null) {
            throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
        }
        return supplierReconSourceService.getAvailableCandidateItems(
                reqVO.getSupplierId(), reqVO.getPeriodStart(), reqVO.getPeriodEnd());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SUPPLIER_RECON", subType = "CREATE", bizNo = "{{#result}}",
            success = "生成供应商对账单【{{#result}}】")
    public Long createSupplierRecon(SupplierReconCreateReqVO createReqVO) {
        if (createReqVO == null || createReqVO.getSupplierId() == null) {
            throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
        }
        SupplierRespVO supplier = supplierService.getSupplier(createReqVO.getSupplierId());
        if (supplier == null) {
            throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
        }

        if (CollUtil.isEmpty(createReqVO.getItems())) {
            throw exception(SUPPLIER_RECON_NO_SOURCE_ITEMS);
        }

        // 1. 优先对所请求的源单行逐指纹比对占用：防止可用候选池变空时 004 被 003 提前截断
        Set<String> requestedFingerprints = createReqVO.getItems().stream()
                .filter(it -> !LineType.ADJUST.getValue().equalsIgnoreCase(it.getLineType()))
                .map(it -> supplierReconSourceService.buildFingerprint(
                        it.getSourceRefType(), it.getSourceRefId(), it.getSourceRefItemId()))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (!requestedFingerprints.isEmpty()) {
            Map<String, String> occupied = supplierReconSourceService.findOccupiedFingerprints(requestedFingerprints);
            if (!occupied.isEmpty()) {
                String occupiedReconNo = occupied.values().iterator().next();
                throw exception(SUPPLIER_RECON_SOURCE_REFERENCED, occupiedReconNo);
            }
        }

        // 2. 服务端以同一取数服务重取，区间内无可对账明细时报 003（预览给空态，生成拒绝生成空单）
        List<SupplierReconSourceItemRespVO> availableCandidates = supplierReconSourceService.getAvailableCandidateItems(
                createReqVO.getSupplierId(), createReqVO.getPeriodStart(), createReqVO.getPeriodEnd());
        if (CollUtil.isEmpty(availableCandidates)) {
            throw exception(SUPPLIER_RECON_NO_SOURCE_ITEMS);
        }

        Map<String, SupplierReconSourceItemRespVO> candidateMap = availableCandidates.stream()
                .filter(c -> c.getFingerprint() != null)
                .collect(Collectors.toMap(SupplierReconSourceItemRespVO::getFingerprint, Function.identity(), (a, b) -> a));

        // 3. 逐行比对与权威金额计算
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SupplierReconItemDO> itemDOs = new ArrayList<>();

        for (SupplierReconItemSaveVO itemVO : createReqVO.getItems()) {
            SupplierReconItemDO itemDO = new SupplierReconItemDO();

            if (LineType.ADJUST.getValue().equalsIgnoreCase(itemVO.getLineType())) {
                applyAdjustLine(itemDO, itemVO.getLineAmount(), itemVO.getAdjustReason(), itemVO.getRemark());
                totalAmount = totalAmount.add(itemDO.getLineAmount());
            } else {
                itemDO.setLineType(itemVO.getLineType());
                String fingerprint = supplierReconSourceService.buildFingerprint(
                        itemVO.getSourceRefType(), itemVO.getSourceRefId(), itemVO.getSourceRefItemId());
                SupplierReconSourceItemRespVO candidateItem = candidateMap.get(fingerprint);
                if (candidateItem == null) {
                    Map<String, String> occupied = supplierReconSourceService.findOccupiedFingerprints(Set.of(fingerprint));
                    String occupiedReconNo = !occupied.isEmpty() ? occupied.get(fingerprint) : "其他单据";
                    throw exception(SUPPLIER_RECON_SOURCE_REFERENCED, occupiedReconNo);
                }

                // 权威复制源快照
                populateSnapshotFromCandidate(candidateItem, itemDO);

                // 计价基准与权威数量
                String basis = StringUtils.hasText(itemVO.getPricingBasis())
                        ? itemVO.getPricingBasis() : candidateItem.getPricingBasis();
                BigDecimal billQty = deriveBillQty(candidateItem, basis);
                BigDecimal unitPrice = itemVO.getUnitPrice() != null ? itemVO.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal lineAmount = calculateLineAmount(billQty, unitPrice);

                itemDO.setPricingBasis(basis);
                itemDO.setBillQty(billQty);
                itemDO.setUnitPrice(unitPrice);
                itemDO.setLineAmount(lineAmount);
                itemDO.setRemark(itemVO.getRemark());
                totalAmount = totalAmount.add(lineAmount);
            }
            itemDOs.add(itemDO);
        }

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        // 3. 按 ADR-0006 取号（SR+YYYYMMDD+3位流水，遇冲突顺延重试）
        String reconNo = generateReconNoWithRetry(LocalDate.now());

        // 4. 保存主表
        SupplierReconDO recon = new SupplierReconDO();
        recon.setReconNo(reconNo);
        recon.setSupplierId(createReqVO.getSupplierId());
        recon.setSupplierName(supplier.getCompanyName());
        recon.setPeriodStart(createReqVO.getPeriodStart());
        recon.setPeriodEnd(createReqVO.getPeriodEnd());
        recon.setStatus(ReconStatus.DRAFT.getValue());
        recon.setTotalAmount(totalAmount);
        recon.setRemark(createReqVO.getRemark());
        supplierReconMapper.insert(recon);

        // 5. 批量保存明细行
        for (SupplierReconItemDO itemDO : itemDOs) {
            itemDO.setReconId(recon.getId());
        }
        supplierReconItemMapper.insertBatch(itemDOs);

        return recon.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SUPPLIER_RECON", subType = "UPDATE", bizNo = "{{#updateReqVO.id}}",
            success = "更新草稿对账单【{{#updateReqVO.id}}】")
    public void updateSupplierRecon(SupplierReconUpdateReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
        SupplierReconDO recon = supplierReconMapper.selectById(updateReqVO.getId());
        validateReconExistsAndTenant(recon);

        // 状态守卫：仅 DRAFT 允许编辑
        if (!ReconStatus.DRAFT.getValue().equals(recon.getStatus())) {
            throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
        }

        // 查询当前单据现存明细，严格防止跨单越权修改/删除
        List<SupplierReconItemDO> dbItems = supplierReconItemMapper.selectList(
                new LambdaQueryWrapperX<SupplierReconItemDO>().eq(SupplierReconItemDO::getReconId, recon.getId()));
        Map<Long, SupplierReconItemDO> dbItemMap = dbItems.stream()
                .collect(Collectors.toMap(SupplierReconItemDO::getId, Function.identity()));

        // 1. 显式删除指定行（仅属于本单据的行）
        if (CollUtil.isNotEmpty(updateReqVO.getDeletedItemIds())) {
            List<Long> validDeleteIds = updateReqVO.getDeletedItemIds().stream()
                    .filter(dbItemMap::containsKey)
                    .toList();
            if (!validDeleteIds.isEmpty()) {
                supplierReconItemMapper.deleteBatchIds(validDeleteIds);
            }
        }

        // 2. 更新或新增明细行，并服务端重算金额
        if (CollUtil.isNotEmpty(updateReqVO.getItems())) {
            for (SupplierReconItemSaveVO itemVO : updateReqVO.getItems()) {
                if (itemVO.getId() != null) {
                    SupplierReconItemDO existingItem = dbItemMap.get(itemVO.getId());
                    if (existingItem == null) {
                        throw exception(SUPPLIER_RECON_NOT_EXISTS);
                    }
                    if (LineType.ADJUST.getValue().equalsIgnoreCase(existingItem.getLineType())
                            || LineType.ADJUST.getValue().equalsIgnoreCase(itemVO.getLineType())) {
                        applyAdjustLine(existingItem, itemVO.getLineAmount(), itemVO.getAdjustReason(), itemVO.getRemark());
                    } else {
                        // 取数行单价与基准变更，bill_qty 随基准切换重算
                        String basis = itemVO.getPricingBasis();
                        if (StringUtils.hasText(basis)) {
                            existingItem.setPricingBasis(basis);
                        }
                        if (itemVO.getUnitPrice() != null) {
                            existingItem.setUnitPrice(itemVO.getUnitPrice());
                        }
                        BigDecimal billQty = deriveBillQty(existingItem.getLineType(), existingItem.getPricingBasis(),
                                existingItem.getQtyKg() != null ? existingItem.getQtyKg() : itemVO.getQtyKg(),
                                existingItem.getQtyM() != null ? existingItem.getQtyM() : itemVO.getQtyM(),
                                existingItem.getBillQty());
                        existingItem.setBillQty(billQty);
                        existingItem.setLineAmount(calculateLineAmount(existingItem.getBillQty(), existingItem.getUnitPrice()));
                        if (itemVO.getRemark() != null) {
                            existingItem.setRemark(itemVO.getRemark());
                        }
                    }
                    supplierReconItemMapper.updateById(existingItem);
                } else {
                    // 新增行守卫：仅允许新增 ADJUST 调整项行，严禁随意凭空新增源单明细
                    if (!LineType.ADJUST.getValue().equalsIgnoreCase(itemVO.getLineType())) {
                        throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
                    }
                    SupplierReconItemDO newItem = new SupplierReconItemDO();
                    newItem.setReconId(recon.getId());
                    applyAdjustLine(newItem, itemVO.getLineAmount(), itemVO.getAdjustReason(), itemVO.getRemark());
                    supplierReconItemMapper.insert(newItem);
                }
            }
        }

        // 3. 服务端权威重算对账总金额 total_amount
        List<SupplierReconItemDO> survivingItems = supplierReconItemMapper.selectList(
                new LambdaQueryWrapperX<SupplierReconItemDO>().eq(SupplierReconItemDO::getReconId, recon.getId()));
        BigDecimal newTotal = survivingItems.stream()
                .map(SupplierReconItemDO::getLineAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        recon.setTotalAmount(newTotal);
        if (updateReqVO.getRemark() != null) {
            recon.setRemark(updateReqVO.getRemark());
        }
        supplierReconMapper.updateById(recon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SUPPLIER_RECON", subType = "RE_FETCH", bizNo = "{{#id}}",
            success = "草稿对账单重新取数【{{#id}}】")
    public void reFetchSupplierRecon(Long id) {
        if (id == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
        SupplierReconDO recon = supplierReconMapper.selectById(id);
        validateReconExistsAndTenant(recon);

        // 状态守卫：仅 DRAFT 允许重取
        if (!ReconStatus.DRAFT.getValue().equals(recon.getStatus())) {
            throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
        }

        // 1. 查询现有行，识别需替换的取数行（PROCESS 与 PURCHASE）及保留的 ADJUST 调整项
        List<SupplierReconItemDO> existingItems = supplierReconItemMapper.selectList(
                new LambdaQueryWrapperX<SupplierReconItemDO>().eq(SupplierReconItemDO::getReconId, id));

        List<Long> deleteSourceItemIds = new ArrayList<>();
        BigDecimal adjustTotal = BigDecimal.ZERO;
        for (SupplierReconItemDO item : existingItems) {
            if (LineType.ADJUST.getValue().equalsIgnoreCase(item.getLineType())) {
                if (item.getLineAmount() != null) {
                    adjustTotal = adjustTotal.add(item.getLineAmount());
                }
            } else {
                deleteSourceItemIds.add(item.getId());
            }
        }

        // 2. 替换取数行：删除旧的取数行
        if (!deleteSourceItemIds.isEmpty()) {
            supplierReconItemMapper.deleteBatchIds(deleteSourceItemIds);
        }

        // 3. 重新取数并插入新候选行
        List<SupplierReconSourceItemRespVO> candidates = supplierReconSourceService.getAvailableCandidateItems(
                recon.getSupplierId(), recon.getPeriodStart(), recon.getPeriodEnd());

        BigDecimal sourceTotal = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(candidates)) {
            List<SupplierReconItemDO> newItems = new ArrayList<>(candidates.size());
            for (SupplierReconSourceItemRespVO cand : candidates) {
                SupplierReconItemDO itemDO = convertCandidateToItemDO(cand, id);
                newItems.add(itemDO);
                if (itemDO.getLineAmount() != null) {
                    sourceTotal = sourceTotal.add(itemDO.getLineAmount());
                }
            }
            supplierReconItemMapper.insertBatch(newItems);
        }

        // 4. 重算总金额并更新
        BigDecimal newTotal = sourceTotal.add(adjustTotal).setScale(2, RoundingMode.HALF_UP);
        recon.setTotalAmount(newTotal);
        supplierReconMapper.updateById(recon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierRecon(Long id) {
        if (id == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
        SupplierReconDO recon = supplierReconMapper.selectById(id);
        validateReconExistsAndTenant(recon);

        // 状态守卫：仅 DRAFT 允许软删进回收站
        if (!ReconStatus.DRAFT.getValue().equals(recon.getStatus())) {
            throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
        }

        // 软删除主表与明细行
        supplierReconMapper.deleteById(id);
        supplierReconItemMapper.delete(new LambdaQueryWrapperX<SupplierReconItemDO>()
                .eq(SupplierReconItemDO::getReconId, id));
    }

    @Override
    public SupplierReconRespVO getSupplierRecon(Long id) {
        if (id == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
        SupplierReconDO recon = supplierReconMapper.selectById(id);
        validateReconExistsAndTenant(recon);

        List<SupplierReconItemDO> items = supplierReconItemMapper.selectList(
                new LambdaQueryWrapperX<SupplierReconItemDO>()
                        .eq(SupplierReconItemDO::getReconId, id)
                        .orderByAsc(SupplierReconItemDO::getId));

        SupplierReconRespVO respVO = BeanUtils.toBean(recon, SupplierReconRespVO.class);
        respVO.setItems(BeanUtils.toBean(items, SupplierReconItemRespVO.class));

        // 实时聚合已申请金额（排除 REJECTED 和 CANCELLED）
        respVO.setAppliedAmount(computeAppliedAmount(id));

        return respVO;
    }

    @Override
    public PageResult<SupplierReconRespVO> getSupplierReconPage(SupplierReconPageReqVO reqVO) {
        LambdaQueryWrapper<SupplierReconDO> wrapper = new LambdaQueryWrapperX<SupplierReconDO>()
                .eqIfPresent(SupplierReconDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(SupplierReconDO::getStatus, reqVO.getStatus())
                .geIfPresent(SupplierReconDO::getPeriodStart, reqVO.getPeriodStart())
                .leIfPresent(SupplierReconDO::getPeriodEnd, reqVO.getPeriodEnd())
                .and(StringUtils.hasText(reqVO.getKeyword()), w -> w
                        .like(SupplierReconDO::getReconNo, reqVO.getKeyword().trim())
                        .or()
                        .like(SupplierReconDO::getSupplierName, reqVO.getKeyword().trim()));

        if (CollUtil.isEmpty(reqVO.getSortingFields())) {
            wrapper.orderByDesc(SupplierReconDO::getId);
        }

        PageResult<SupplierReconDO> pageResult = supplierReconMapper.selectPage(reqVO, wrapper);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty();
        }

        List<Long> reconIds = pageResult.getList().stream().map(SupplierReconDO::getId).toList();
        Map<Long, BigDecimal> appliedAmountMap = batchComputeAppliedAmounts(reconIds);

        List<SupplierReconRespVO> voList = pageResult.getList().stream().map(recon -> {
            SupplierReconRespVO vo = BeanUtils.toBean(recon, SupplierReconRespVO.class);
            vo.setAppliedAmount(appliedAmountMap.getOrDefault(recon.getId(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    private BigDecimal computeAppliedAmount(Long reconId) {
        return batchComputeAppliedAmounts(List.of(reconId))
                .getOrDefault(reconId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private Map<Long, BigDecimal> batchComputeAppliedAmounts(Collection<Long> reconIds) {
        if (CollUtil.isEmpty(reconIds)) {
            return Collections.emptyMap();
        }
        List<PaymentApplyDO> applies = paymentApplyMapper.selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .inOrEmpty(PaymentApplyDO::getReconId, reconIds)
                .notIn(PaymentApplyDO::getApprovalStatus, Arrays.asList("REJECTED", "CANCELLED")));
        if (CollUtil.isEmpty(applies)) {
            return Collections.emptyMap();
        }

        Map<Long, BigDecimal> map = new HashMap<>();
        for (PaymentApplyDO apply : applies) {
            if (apply.getReconId() != null && apply.getAmount() != null) {
                map.merge(apply.getReconId(), apply.getAmount(), BigDecimal::add);
            }
        }
        map.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));
        return map;
    }

    private void validateAdjustLine(BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0 || !StringUtils.hasText(reason)) {
            throw exception(SUPPLIER_RECON_ADJUST_LINE_INVALID);
        }
    }

    private void applyAdjustLine(SupplierReconItemDO itemDO, BigDecimal amount, String adjustReason, String remark) {
        validateAdjustLine(amount, adjustReason);
        itemDO.setLineType(LineType.ADJUST.getValue());
        itemDO.setLineAmount(amount.setScale(2, RoundingMode.HALF_UP));
        itemDO.setBillQty(BigDecimal.ZERO);
        itemDO.setUnitPrice(BigDecimal.ZERO);
        itemDO.setAdjustReason(adjustReason.trim());
        if (remark != null) {
            itemDO.setRemark(remark);
        }
    }

    private BigDecimal calculateLineAmount(BigDecimal billQty, BigDecimal unitPrice) {
        BigDecimal qty = billQty != null ? billQty : BigDecimal.ZERO;
        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        return qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    private void populateSnapshotFromCandidate(SupplierReconSourceItemRespVO cand, SupplierReconItemDO target) {
        target.setSourceRefType(cand.getSourceRefType());
        target.setSourceRefId(cand.getSourceRefId());
        target.setSourceRefItemId(cand.getSourceRefItemId());
        target.setInspectionOrderId(cand.getInspectionOrderId());
        target.setInspectionOrderNo(cand.getInspectionOrderNo());
        target.setInspectionType(cand.getInspectionType());
        target.setStockInId(cand.getStockInId());
        target.setStockInNo(cand.getStockInNo());
        target.setStockInItemId(cand.getStockInItemId());
        target.setYarnOrderId(cand.getYarnOrderId());
        target.setYarnOrderNo(cand.getYarnOrderNo());
        target.setProductId(cand.getProductId());
        target.setProductCode(cand.getProductCode());
        target.setProductName(cand.getProductName());
        target.setBatchNo(cand.getBatchNo());
        target.setQtyKg(cand.getQtyKg());
        target.setQtyM(cand.getQtyM());
        target.setQtyUnit(cand.getQtyUnit());
    }

    private BigDecimal deriveBillQty(String lineType, String pricingBasis, BigDecimal qtyKg, BigDecimal qtyM, BigDecimal fallbackQty) {
        if (LineType.PROCESS.getValue().equalsIgnoreCase(lineType)) {
            if (PricingBasis.KG.getValue().equalsIgnoreCase(pricingBasis)) {
                return qtyKg != null ? qtyKg.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            } else {
                return qtyM != null ? qtyM.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            }
        }
        return fallbackQty != null ? fallbackQty.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal deriveBillQty(SupplierReconSourceItemRespVO cand, String pricingBasis) {
        return deriveBillQty(cand.getLineType(), pricingBasis, cand.getQtyKg(), cand.getQtyM(), cand.getBillQty());
    }

    private SupplierReconItemDO convertCandidateToItemDO(SupplierReconSourceItemRespVO cand, Long reconId) {
        SupplierReconItemDO itemDO = new SupplierReconItemDO();
        itemDO.setReconId(reconId);
        itemDO.setLineType(cand.getLineType());
        populateSnapshotFromCandidate(cand, itemDO);
        itemDO.setPricingBasis(cand.getPricingBasis());
        itemDO.setBillQty(cand.getBillQty());
        itemDO.setUnitPrice(cand.getUnitPrice() != null ? cand.getUnitPrice() : BigDecimal.ZERO);
        itemDO.setLineAmount(cand.getLineAmount() != null ? cand.getLineAmount() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return itemDO;
    }

    private void validateReconExistsAndTenant(SupplierReconDO recon) {
        if (recon == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && recon.getTenantId() != null && !currentTenantId.equals(recon.getTenantId())) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
    }

    private String generateReconNo(LocalDate date) {
        return generateReconNoWithRetry(date);
    }

    private String generateReconNoWithRetry(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = RECON_NO_PREFIX + DATE_FORMATTER.format(targetDate);
        List<SupplierReconDO> candidates = supplierReconMapper.selectListByReconNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        if (CollUtil.isNotEmpty(candidates)) {
            for (SupplierReconDO candidate : candidates) {
                String no = candidate.getReconNo();
                if (no == null || no.length() <= prefix.length()) {
                    continue;
                }
                String suffix = no.substring(prefix.length());
                Matcher matcher = NO_SUFFIX_NUMBER_PATTERN.matcher(suffix);
                if (!matcher.find()) {
                    continue;
                }
                String digits = matcher.group();
                try {
                    long num = Long.parseLong(digits);
                    if (num > maxNum) {
                        maxNum = num;
                        maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                    }
                } catch (NumberFormatException ignore) {
                    continue;
                }
            }
        }
        for (int i = 1; i <= MAX_GENERATE_RETRY; i++) {
            long seq = maxNum + i;
            String candidate = prefix + String.format("%0" + maxDigits + "d", seq);
            if (supplierReconMapper.selectByReconNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
    }

}
