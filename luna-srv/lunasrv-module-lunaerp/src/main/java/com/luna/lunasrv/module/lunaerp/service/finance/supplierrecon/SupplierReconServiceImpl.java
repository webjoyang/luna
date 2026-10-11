package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.PricingBasisEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconLineTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconSourceRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconStatusEnum;
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
 * 财务管理 - 供应商对账服务实现
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class SupplierReconServiceImpl implements SupplierReconService {

    private static final String RECON_NO_PREFIX = "SR";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NO_MIN_DIGITS = 3;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    private static final int MAX_GENERATE_RETRY = 5;

    @Resource
    private SupplierReconMapper supplierReconMapper;

    @Resource
    private SupplierReconItemMapper supplierReconItemMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SupplierReconSourceService supplierReconSourceService;

    @Resource
    private PaymentApplyMapper paymentApplyMapper;

    @Override
    public PageResult<SupplierReconRespVO> getReconPage(SupplierReconPageReqVO pageReqVO) {
        PageResult<SupplierReconDO> pageResult = supplierReconMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        List<Long> reconIds = pageResult.getList().stream().map(SupplierReconDO::getId).toList();
        List<PaymentApplyDO> applies = paymentApplyMapper.selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .in(PaymentApplyDO::getReconId, reconIds)
                .notIn(PaymentApplyDO::getApprovalStatus, Arrays.asList("REJECTED", "CANCELLED")));

        Map<Long, BigDecimal> appliedMap = new HashMap<>();
        for (PaymentApplyDO apply : applies) {
            if (apply.getReconId() != null && apply.getAmount() != null) {
                appliedMap.merge(apply.getReconId(), apply.getAmount(), BigDecimal::add);
            }
        }

        List<SupplierReconRespVO> respList = pageResult.getList().stream().map(recon -> {
            BigDecimal appliedAmount = appliedMap.getOrDefault(recon.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            return convertToRespVO(recon, Collections.emptyList(), appliedAmount);
        }).toList();

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public SupplierReconRespVO getRecon(Long id) {
        SupplierReconDO recon = supplierReconMapper.selectById(id);
        if (recon == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }

        List<SupplierReconItemDO> items = supplierReconItemMapper.selectListByReconId(id);
        List<SupplierReconItemRespVO> itemVOs = items.stream().map(this::convertItemToRespVO).toList();

        BigDecimal appliedAmount = calculateAppliedAmount(id);

        return convertToRespVO(recon, itemVOs, appliedAmount);
    }

    @Override
    public SupplierReconCandidatesRespVO previewRecon(SupplierReconPreviewReqVO previewReqVO) {
        SupplierDO supplier = supplierMapper.selectById(previewReqVO.getSupplierId());
        if (supplier == null) {
            throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
        }
        return supplierReconSourceService.getCandidateItems(
                previewReqVO.getSupplierId(), previewReqVO.getPeriodStart(), previewReqVO.getPeriodEnd());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SUPPLIER_RECON", subType = "CREATE", bizNo = "{{#_ret}}", success = "创建供应商对账单【{{#_ret}}】")
    public Long createRecon(SupplierReconSaveReqVO createReqVO) {
        // 1. 供应商校验
        SupplierDO supplier = supplierMapper.selectById(createReqVO.getSupplierId());
        if (supplier == null) {
            throw exception(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS);
        }

        // 2. 判空
        if (CollUtil.isEmpty(createReqVO.getItems())) {
            throw exception(SUPPLIER_RECON_NO_SOURCE_ITEMS);
        }

        // 3. 服务端同一取数服务重取候选明细
        SupplierReconCandidatesRespVO candidateResp = supplierReconSourceService.getCandidateItems(
                createReqVO.getSupplierId(), createReqVO.getPeriodStart(), createReqVO.getPeriodEnd());

        Map<String, SupplierReconCandidateItemRespVO> candidateMap = new HashMap<>();
        if (candidateResp != null && CollUtil.isNotEmpty(candidateResp.getItems())) {
            for (SupplierReconCandidateItemRespVO candidate : candidateResp.getItems()) {
                String fp = SupplierReconSourceService.buildFingerprint(
                        candidate.getSourceRefType(), candidate.getSourceRefId(), candidate.getSourceRefItemId());
                candidateMap.put(fp, candidate);
            }
        }

        boolean hasSourceItem = createReqVO.getItems().stream()
                .anyMatch(i -> !SupplierReconLineTypeEnum.ADJUST.getValue().equalsIgnoreCase(i.getLineType()));
        if (candidateMap.isEmpty() && !hasSourceItem) {
            // 只有调整项但候选为空时，根据业务定义检查区间内无可对账明细
            // 若全部为 ADJUST 且区间无源单据，仍需至少有一条合法行
        }

        // 4. 逐行校验与服务端权威重算
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SupplierReconItemDO> itemsToInsert = new ArrayList<>(createReqVO.getItems().size());

        for (SupplierReconItemSaveReqVO itemReq : createReqVO.getItems()) {
            String lineType = itemReq.getLineType();

            if (SupplierReconLineTypeEnum.ADJUST.getValue().equalsIgnoreCase(lineType)) {
                // ADJUST 行守卫：金额为 0 或未填备注报 005
                if (itemReq.getLineAmount() == null
                        || itemReq.getLineAmount().compareTo(BigDecimal.ZERO) == 0
                        || !StringUtils.hasText(itemReq.getAdjustReason())) {
                    throw exception(SUPPLIER_RECON_ADJUST_LINE_INVALID);
                }
                BigDecimal lineAmount = itemReq.getLineAmount().setScale(2, RoundingMode.HALF_UP);
                totalAmount = totalAmount.add(lineAmount);

                SupplierReconItemDO itemDO = SupplierReconItemDO.builder()
                        .lineType(SupplierReconLineTypeEnum.ADJUST.getValue())
                        .lineAmount(lineAmount)
                        .adjustReason(itemReq.getAdjustReason().trim())
                        .remark(itemReq.getRemark())
                        .build();
                itemsToInsert.add(itemDO);

            } else if (SupplierReconLineTypeEnum.PROCESS.getValue().equalsIgnoreCase(lineType)
                    || SupplierReconLineTypeEnum.PURCHASE.getValue().equalsIgnoreCase(lineType)) {

                // 源指纹比对：不在候选明细中则表示被抢引用或不存在
                String fp = SupplierReconSourceService.buildFingerprint(
                        itemReq.getSourceRefType(), itemReq.getSourceRefId(), itemReq.getSourceRefItemId());

                if (!candidateMap.containsKey(fp)) {
                    // 查出具体被哪张有效对账单引用
                    List<SupplierReconItemDO> refItems = supplierReconItemMapper.selectListBySourceRef(
                            itemReq.getSourceRefType(), itemReq.getSourceRefId(), itemReq.getSourceRefItemId());
                    String conflictReconNo = null;
                    if (CollUtil.isNotEmpty(refItems)) {
                        for (SupplierReconItemDO refItem : refItems) {
                            SupplierReconDO parentRecon = supplierReconMapper.selectById(refItem.getReconId());
                            if (parentRecon != null && (SupplierReconStatusEnum.DRAFT.getValue().equals(parentRecon.getStatus())
                                    || SupplierReconStatusEnum.CONFIRMED.getValue().equals(parentRecon.getStatus()))) {
                                conflictReconNo = parentRecon.getReconNo();
                                break;
                            }
                        }
                    }
                    if (conflictReconNo != null) {
                        throw exception(SUPPLIER_RECON_SOURCE_REFERENCED, conflictReconNo);
                    }
                    throw exception(SUPPLIER_RECON_NO_SOURCE_ITEMS);
                }

                SupplierReconCandidateItemRespVO candidate = candidateMap.get(fp);

                if (SupplierReconLineTypeEnum.PROCESS.getValue().equalsIgnoreCase(lineType)) {
                    // PROCESS: pricingBasis (METER -> qtyM, KG -> qtyKg), billQty, unitPrice, lineAmount
                    String pricingBasis = StringUtils.hasText(itemReq.getPricingBasis())
                            ? itemReq.getPricingBasis() : candidate.getPricingBasis();
                    BigDecimal billQty;
                    if (PricingBasisEnum.METER.getValue().equalsIgnoreCase(pricingBasis)) {
                        billQty = candidate.getQtyM() != null ? candidate.getQtyM()
                                : (itemReq.getQtyM() != null ? itemReq.getQtyM() : BigDecimal.ZERO);
                    } else {
                        billQty = candidate.getQtyKg() != null ? candidate.getQtyKg()
                                : (itemReq.getQtyKg() != null ? itemReq.getQtyKg() : BigDecimal.ZERO);
                    }
                    billQty = billQty.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal lineAmount = billQty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    totalAmount = totalAmount.add(lineAmount);

                    SupplierReconItemDO itemDO = SupplierReconItemDO.builder()
                            .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                            .sourceRefType(SupplierReconSourceRefTypeEnum.INSPECTION.getValue())
                            .sourceRefId(candidate.getSourceRefId())
                            .sourceRefItemId(null)
                            .inspectionOrderId(candidate.getInspectionOrderId())
                            .inspectionOrderNo(candidate.getInspectionOrderNo())
                            .inspectionType(candidate.getInspectionType())
                            .productId(candidate.getProductId())
                            .productCode(candidate.getProductCode())
                            .productName(candidate.getProductName())
                            .qtyKg(candidate.getQtyKg())
                            .qtyM(candidate.getQtyM())
                            .pricingBasis(pricingBasis)
                            .billQty(billQty)
                            .unitPrice(unitPrice)
                            .lineAmount(lineAmount)
                            .remark(itemReq.getRemark())
                            .build();
                    itemsToInsert.add(itemDO);

                } else {
                    // PURCHASE: billQty = candidate.billQty, unitPrice, lineAmount
                    BigDecimal billQty = candidate.getBillQty() != null ? candidate.getBillQty()
                            : (itemReq.getBillQty() != null ? itemReq.getBillQty() : BigDecimal.ZERO);
                    billQty = billQty.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal lineAmount = billQty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    totalAmount = totalAmount.add(lineAmount);

                    SupplierReconItemDO itemDO = SupplierReconItemDO.builder()
                            .lineType(SupplierReconLineTypeEnum.PURCHASE.getValue())
                            .sourceRefType(SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue())
                            .sourceRefId(candidate.getSourceRefId())
                            .sourceRefItemId(candidate.getSourceRefItemId())
                            .stockInId(candidate.getStockInId())
                            .stockInNo(candidate.getStockInNo())
                            .yarnOrderId(candidate.getYarnOrderId())
                            .yarnOrderNo(candidate.getYarnOrderNo())
                            .productId(candidate.getProductId())
                            .productCode(candidate.getProductCode())
                            .productName(candidate.getProductName())
                            .batchNo(candidate.getBatchNo())
                            .qtyKg(candidate.getQtyKg())
                            .qtyM(candidate.getQtyM())
                            .pricingBasis(candidate.getPricingBasis())
                            .billQty(billQty)
                            .unitPrice(unitPrice)
                            .lineAmount(lineAmount)
                            .remark(itemReq.getRemark())
                            .build();
                    itemsToInsert.add(itemDO);
                }
            } else {
                throw exception(SUPPLIER_RECON_NO_SOURCE_ITEMS);
            }
        }

        // 5. 生成单号并保存主表
        String reconNo = generateReconNoWithRetry();
        SupplierReconDO recon = SupplierReconDO.builder()
                .reconNo(reconNo)
                .supplierId(supplier.getId())
                .supplierName(supplier.getCompanyName())
                .periodStart(createReqVO.getPeriodStart())
                .periodEnd(createReqVO.getPeriodEnd())
                .status(SupplierReconStatusEnum.DRAFT.getValue())
                .totalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .remark(createReqVO.getRemark())
                .build();
        supplierReconMapper.insert(recon);

        // 6. 保存行表
        for (SupplierReconItemDO itemDO : itemsToInsert) {
            itemDO.setReconId(recon.getId());
            supplierReconItemMapper.insert(itemDO);
        }

        return recon.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SUPPLIER_RECON", subType = "UPDATE", bizNo = "{{#updateReqVO.id}}", success = "修改供应商对账单【{{#updateReqVO.id}}】")
    public void updateRecon(SupplierReconSaveReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }

        SupplierReconDO recon = supplierReconMapper.selectById(updateReqVO.getId());
        if (recon == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
        if (!SupplierReconStatusEnum.DRAFT.getValue().equals(recon.getStatus())) {
            throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
        }

        List<SupplierReconItemDO> dbItems = supplierReconItemMapper.selectListByReconId(recon.getId());
        Map<Long, SupplierReconItemDO> dbItemMap = dbItems.stream()
                .collect(Collectors.toMap(SupplierReconItemDO::getId, Function.identity()));

        Set<Long> reqItemIds = updateReqVO.getItems() != null ? updateReqVO.getItems().stream()
                .map(SupplierReconItemSaveReqVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()) : Collections.emptySet();

        // 删行
        for (SupplierReconItemDO dbItem : dbItems) {
            if (!reqItemIds.contains(dbItem.getId())) {
                supplierReconItemMapper.deleteById(dbItem.getId());
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(updateReqVO.getItems())) {
            for (SupplierReconItemSaveReqVO itemReq : updateReqVO.getItems()) {
                String lineType = itemReq.getLineType();

                if (SupplierReconLineTypeEnum.ADJUST.getValue().equalsIgnoreCase(lineType)) {
                    if (itemReq.getLineAmount() == null
                            || itemReq.getLineAmount().compareTo(BigDecimal.ZERO) == 0
                            || !StringUtils.hasText(itemReq.getAdjustReason())) {
                        throw exception(SUPPLIER_RECON_ADJUST_LINE_INVALID);
                    }
                    BigDecimal lineAmount = itemReq.getLineAmount().setScale(2, RoundingMode.HALF_UP);
                    totalAmount = totalAmount.add(lineAmount);

                    if (itemReq.getId() != null && dbItemMap.containsKey(itemReq.getId())) {
                        SupplierReconItemDO itemDO = dbItemMap.get(itemReq.getId());
                        itemDO.setLineAmount(lineAmount);
                        itemDO.setAdjustReason(itemReq.getAdjustReason().trim());
                        itemDO.setRemark(itemReq.getRemark());
                        supplierReconItemMapper.updateById(itemDO);
                    } else {
                        SupplierReconItemDO itemDO = SupplierReconItemDO.builder()
                                .reconId(recon.getId())
                                .lineType(SupplierReconLineTypeEnum.ADJUST.getValue())
                                .lineAmount(lineAmount)
                                .adjustReason(itemReq.getAdjustReason().trim())
                                .remark(itemReq.getRemark())
                                .build();
                        supplierReconItemMapper.insert(itemDO);
                    }

                } else if (SupplierReconLineTypeEnum.PROCESS.getValue().equalsIgnoreCase(lineType)) {
                    String pricingBasis = itemReq.getPricingBasis();
                    BigDecimal billQty;
                    if (PricingBasisEnum.METER.getValue().equalsIgnoreCase(pricingBasis)) {
                        billQty = itemReq.getQtyM() != null ? itemReq.getQtyM() : BigDecimal.ZERO;
                    } else {
                        billQty = itemReq.getQtyKg() != null ? itemReq.getQtyKg() : BigDecimal.ZERO;
                    }
                    billQty = billQty.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal lineAmount = billQty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    totalAmount = totalAmount.add(lineAmount);

                    if (itemReq.getId() != null && dbItemMap.containsKey(itemReq.getId())) {
                        SupplierReconItemDO itemDO = dbItemMap.get(itemReq.getId());
                        itemDO.setPricingBasis(pricingBasis);
                        itemDO.setBillQty(billQty);
                        itemDO.setUnitPrice(unitPrice);
                        itemDO.setLineAmount(lineAmount);
                        itemDO.setRemark(itemReq.getRemark());
                        supplierReconItemMapper.updateById(itemDO);
                    } else {
                        SupplierReconItemDO itemDO = buildItemDOFromReq(recon.getId(), itemReq, billQty, lineAmount);
                        supplierReconItemMapper.insert(itemDO);
                    }

                } else if (SupplierReconLineTypeEnum.PURCHASE.getValue().equalsIgnoreCase(lineType)) {
                    BigDecimal billQty = itemReq.getBillQty() != null ? itemReq.getBillQty() : BigDecimal.ZERO;
                    billQty = billQty.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal lineAmount = billQty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    totalAmount = totalAmount.add(lineAmount);

                    if (itemReq.getId() != null && dbItemMap.containsKey(itemReq.getId())) {
                        SupplierReconItemDO itemDO = dbItemMap.get(itemReq.getId());
                        itemDO.setBillQty(billQty);
                        itemDO.setUnitPrice(unitPrice);
                        itemDO.setLineAmount(lineAmount);
                        itemDO.setRemark(itemReq.getRemark());
                        supplierReconItemMapper.updateById(itemDO);
                    } else {
                        SupplierReconItemDO itemDO = buildItemDOFromReq(recon.getId(), itemReq, billQty, lineAmount);
                        supplierReconItemMapper.insert(itemDO);
                    }
                }
            }
        }

        recon.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        recon.setRemark(updateReqVO.getRemark());
        supplierReconMapper.updateById(recon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SUPPLIER_RECON", subType = "RE_FETCH", bizNo = "{{#id}}", success = "重取供应商对账单【{{#id}}】")
    public void reFetchRecon(Long id) {
        SupplierReconDO recon = supplierReconMapper.selectById(id);
        if (recon == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
        if (!SupplierReconStatusEnum.DRAFT.getValue().equals(recon.getStatus())) {
            throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
        }

        // 1. 保留 ADJUST 行，删除全部 PROCESS 与 PURCHASE 行
        List<SupplierReconItemDO> existingItems = supplierReconItemMapper.selectListByReconId(id);
        List<SupplierReconItemDO> adjustItems = new ArrayList<>();
        for (SupplierReconItemDO item : existingItems) {
            if (SupplierReconLineTypeEnum.ADJUST.getValue().equalsIgnoreCase(item.getLineType())) {
                adjustItems.add(item);
            } else {
                supplierReconItemMapper.deleteById(item.getId());
            }
        }

        // 2. 重新取数（排除其他对账单指纹，但包含本单据自身明细）
        SupplierReconCandidatesRespVO candidates = supplierReconSourceService.getCandidateItems(
                recon.getSupplierId(), recon.getPeriodStart(), recon.getPeriodEnd(), id);

        // 3. 重新插入取数候选行并计算合计金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SupplierReconItemDO adj : adjustItems) {
            if (adj.getLineAmount() != null) {
                totalAmount = totalAmount.add(adj.getLineAmount());
            }
        }

        if (candidates != null && CollUtil.isNotEmpty(candidates.getItems())) {
            for (SupplierReconCandidateItemRespVO c : candidates.getItems()) {
                SupplierReconItemDO itemDO = SupplierReconItemDO.builder()
                        .reconId(id)
                        .lineType(c.getLineType())
                        .sourceRefType(c.getSourceRefType())
                        .sourceRefId(c.getSourceRefId())
                        .sourceRefItemId(c.getSourceRefItemId())
                        .inspectionOrderId(c.getInspectionOrderId())
                        .inspectionOrderNo(c.getInspectionOrderNo())
                        .inspectionType(c.getInspectionType())
                        .stockInId(c.getStockInId())
                        .stockInNo(c.getStockInNo())
                        .yarnOrderId(c.getYarnOrderId())
                        .yarnOrderNo(c.getYarnOrderNo())
                        .productId(c.getProductId())
                        .productCode(c.getProductCode())
                        .productName(c.getProductName())
                        .batchNo(c.getBatchNo())
                        .qtyKg(c.getQtyKg())
                        .qtyM(c.getQtyM())
                        .pricingBasis(c.getPricingBasis())
                        .billQty(c.getBillQty())
                        .unitPrice(c.getUnitPrice())
                        .lineAmount(c.getLineAmount() != null ? c.getLineAmount() : BigDecimal.ZERO)
                        .remark(null)
                        .build();
                supplierReconItemMapper.insert(itemDO);
                if (itemDO.getLineAmount() != null) {
                    totalAmount = totalAmount.add(itemDO.getLineAmount());
                }
            }
        }

        recon.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        supplierReconMapper.updateById(recon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SUPPLIER_RECON", subType = "DELETE", bizNo = "{{#id}}", success = "删除供应商对账单【{{#id}}】")
    public void deleteRecon(Long id) {
        SupplierReconDO recon = supplierReconMapper.selectById(id);
        if (recon == null) {
            throw exception(SUPPLIER_RECON_NOT_EXISTS);
        }
        if (!SupplierReconStatusEnum.DRAFT.getValue().equals(recon.getStatus())) {
            throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
        }

        supplierReconMapper.deleteById(id);
        supplierReconItemMapper.deleteByReconId(id);
    }

    /**
     * 实时聚合对账单已申请金额：Σ( 挂该单且 approval_status NOT IN ('REJECTED', 'CANCELLED') 的付款申请额 )
     */
    private BigDecimal calculateAppliedAmount(Long reconId) {
        List<PaymentApplyDO> applies = paymentApplyMapper.selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .eq(PaymentApplyDO::getReconId, reconId)
                .notIn(PaymentApplyDO::getApprovalStatus, Arrays.asList("REJECTED", "CANCELLED")));

        BigDecimal sum = BigDecimal.ZERO;
        for (PaymentApplyDO apply : applies) {
            if (apply.getAmount() != null) {
                sum = sum.add(apply.getAmount());
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private SupplierReconItemDO buildItemDOFromReq(Long reconId, SupplierReconItemSaveReqVO req,
                                                    BigDecimal billQty, BigDecimal lineAmount) {
        return SupplierReconItemDO.builder()
                .reconId(reconId)
                .lineType(req.getLineType())
                .sourceRefType(req.getSourceRefType())
                .sourceRefId(req.getSourceRefId())
                .sourceRefItemId(req.getSourceRefItemId())
                .inspectionOrderId(req.getInspectionOrderId())
                .inspectionOrderNo(req.getInspectionOrderNo())
                .inspectionType(req.getInspectionType())
                .stockInId(req.getStockInId())
                .stockInNo(req.getStockInNo())
                .yarnOrderId(req.getYarnOrderId())
                .yarnOrderNo(req.getYarnOrderNo())
                .productId(req.getProductId())
                .productCode(req.getProductCode())
                .productName(req.getProductName())
                .batchNo(req.getBatchNo())
                .qtyKg(req.getQtyKg())
                .qtyM(req.getQtyM())
                .pricingBasis(req.getPricingBasis())
                .billQty(billQty)
                .unitPrice(req.getUnitPrice())
                .lineAmount(lineAmount)
                .adjustReason(req.getAdjustReason())
                .remark(req.getRemark())
                .build();
    }

    private SupplierReconRespVO convertToRespVO(SupplierReconDO recon, List<SupplierReconItemRespVO> items, BigDecimal appliedAmount) {
        return SupplierReconRespVO.builder()
                .id(recon.getId())
                .reconNo(recon.getReconNo())
                .supplierId(recon.getSupplierId())
                .supplierName(recon.getSupplierName())
                .periodStart(recon.getPeriodStart())
                .periodEnd(recon.getPeriodEnd())
                .status(recon.getStatus())
                .totalAmount(recon.getTotalAmount())
                .confirmedBy(recon.getConfirmedBy())
                .confirmedAt(recon.getConfirmedAt())
                .cancelledBy(recon.getCancelledBy())
                .cancelledAt(recon.getCancelledAt())
                .remark(recon.getRemark())
                .appliedAmount(appliedAmount)
                .createTime(recon.getCreateTime())
                .items(items)
                .build();
    }

    private SupplierReconItemRespVO convertItemToRespVO(SupplierReconItemDO item) {
        String qtyUnit = null;
        if (PricingBasisEnum.METER.getValue().equalsIgnoreCase(item.getPricingBasis())) {
            qtyUnit = "M";
        } else if (PricingBasisEnum.KG.getValue().equalsIgnoreCase(item.getPricingBasis())) {
            qtyUnit = "KG";
        }

        return SupplierReconItemRespVO.builder()
                .id(item.getId())
                .reconId(item.getReconId())
                .lineType(item.getLineType())
                .sourceRefType(item.getSourceRefType())
                .sourceRefId(item.getSourceRefId())
                .sourceRefItemId(item.getSourceRefItemId())
                .inspectionOrderId(item.getInspectionOrderId())
                .inspectionOrderNo(item.getInspectionOrderNo())
                .inspectionType(item.getInspectionType())
                .stockInId(item.getStockInId())
                .stockInNo(item.getStockInNo())
                .yarnOrderId(item.getYarnOrderId())
                .yarnOrderNo(item.getYarnOrderNo())
                .productId(item.getProductId())
                .productCode(item.getProductCode())
                .productName(item.getProductName())
                .batchNo(item.getBatchNo())
                .qtyKg(item.getQtyKg())
                .qtyM(item.getQtyM())
                .qtyUnit(qtyUnit)
                .pricingBasis(item.getPricingBasis())
                .billQty(item.getBillQty())
                .unitPrice(item.getUnitPrice())
                .lineAmount(item.getLineAmount())
                .adjustReason(item.getAdjustReason())
                .remark(item.getRemark())
                .build();
    }

    /**
     * 生成对账单号：SR + 8位年月日 + 3位日流水（超 999 自然扩位）。
     * 扫描同前缀全部单据（含已删除行，ADR-0006），按最大后缀 + 1。
     */
    private String generateReconNo() {
        LocalDate today = LocalDate.now();
        String prefix = RECON_NO_PREFIX + today.format(DATE_FORMATTER);
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
                long num;
                try {
                    num = Long.parseLong(digits);
                } catch (NumberFormatException ignore) {
                    continue;
                }
                if (num > maxNum) {
                    maxNum = num;
                    maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                }
            }
        }
        if (maxNum == 0L) {
            return prefix + String.format("%0" + NO_MIN_DIGITS + "d", 1);
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    /**
     * 自动取号（撞号有限次顺延重试）
     */
    private String generateReconNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateReconNo();
            if (supplierReconMapper.selectByReconNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(SUPPLIER_RECON_STATUS_NOT_ALLOWED);
    }

}
