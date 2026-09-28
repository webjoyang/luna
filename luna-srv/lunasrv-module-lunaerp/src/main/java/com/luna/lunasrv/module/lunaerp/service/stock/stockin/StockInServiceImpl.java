package com.luna.lunasrv.module.lunaerp.service.stock.stockin;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderGenerateStockInReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderGenerateStockInRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype.GreigeTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemPlacementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation.WarehouseLocationDO;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.greigetype.GreigeTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemPlacementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import jakarta.annotation.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 库存管理 - 入库单 Service 实现类
 *
 * 编号生成：IN-YYYYMMDD-NNN 按天取号、租户内全量唯一含软删行（ADR-0015）；
 * 聚合保存：明细整体替换，服务端权威重算金额（ADR-0003）；
 * 状态机守卫：三态过账状态机，草稿态可编辑/删除，非草稿态报 1_030_011_002（ADR-0016）；
 * 审核过账与红冲：审核写正数库存流水，取消已审核写负数反向行（ADR-0016, ADR-0017）。
 *
 * @author luna
 */
@Service
public class StockInServiceImpl implements StockInService {

    /**
     * 单号前缀
     */
    private static final String ORDER_NO_PREFIX = "IN-";
    /**
     * 流水号最小位数（超 999 自然扩位）
     */
    private static final int NO_MIN_DIGITS = 3;
    /**
     * 自动取号撞号最大顺延重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;
    /**
     * 流水号正则
     */
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    /**
     * 单号日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 默认状态：草稿
     */
    private static final String DEFAULT_STATUS = "DRAFT";
    /**
     * 已完成状态：已审核
     */
    private static final String STATUS_COMPLETED = "COMPLETED";
    /**
     * 已取消状态：作废或红冲终态
     */
    private static final String STATUS_CANCELLED = "CANCELLED";
    /**
     * 关联单据类型：入库单
     */
    private static final String DOC_TYPE_STOCK_IN = "STOCK_IN";
    /**
     * 交易类型：入库正数
     */
    private static final String TXN_TYPE_STOCK_IN = "STOCK_IN";
    /**
     * 交易类型：红冲负数
     */
    private static final String TXN_TYPE_STOCK_IN_REVERSAL = "STOCK_IN_REVERSAL";
    /**
     * 权限标识：入库单修改（含草稿作废）
     */
    private static final String PERMISSION_UPDATE = "lunaerp:stock:stock-in:update";
    /**
     * 权限标识：入库单审核（含审核过账与红冲）
     */
    private static final String PERMISSION_AUDIT = "lunaerp:stock:stock-in:audit";

    @Resource
    private StockInMapper stockInMapper;

    @Resource
    private StockInItemMapper stockInItemMapper;

    @Resource
    private StockInItemPlacementMapper stockInItemPlacementMapper;

    @Resource
    private StockLedgerMapper stockLedgerMapper;

    @Resource
    private WarehouseMapper warehouseMapper;

    @Resource
    private WarehouseLocationMapper warehouseLocationMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Resource
    private DyeingOrderMapper dyeingOrderMapper;

    @Resource
    private GreigeTypeMapper greigeTypeMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Resource
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Resource
    private WeavingTicketService weavingTicketService;

    @Resource
    private WeavingTicketMapper weavingTicketMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockIn(StockInSaveReqVO createReqVO) {
        StockInDO stockIn = BeanUtils.toBean(createReqVO, StockInDO.class);

        // 1. 缺省值
        stockIn.setStatus(DEFAULT_STATUS);
        if (stockIn.getInDate() == null) {
            stockIn.setInDate(LocalDate.now());
        }

        // 2. 编号：手填查重（含已删除行，撞号直接报错）；留空按天自动生成（有限次顺延重试）
        if (StringUtils.hasText(stockIn.getInNo())) {
            validateInNoUnique(null, stockIn.getInNo());
        } else {
            stockIn.setInNo(generateInNoWithRetry(stockIn.getInDate()));
        }

        // 3. 关联存在性校验与快照回填
        fillSnapshotsAndValidateRefs(stockIn, createReqVO);

        // 4. 库位放置归属校验并收集库位信息
        Map<Long, WarehouseLocationDO> locationMap = validateAndGetPlacementLocations(stockIn.getWarehouseId(), stockIn.getWarehouseName(), createReqVO.getItems());

        // 5. 明细聚合：服务端权威重算行金额与主表汇总（允许零明细草稿保存）
        List<StockInItemDO> itemDOList = buildItemDOList(createReqVO.getItems());
        stockIn.setTotalAmount(recalculateTotalAmount(itemDOList));

        // 6. 保存主表与明细及放置记录
        stockInMapper.insert(stockIn);
        saveItemsAndPlacements(stockIn.getId(), locationMap, createReqVO.getItems(), itemDOList);

        return stockIn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockIn(StockInSaveReqVO updateReqVO) {
        // 1. 校验入库单存在
        StockInDO existing = validateStockInExists(updateReqVO.getId());

        // 2. 状态守卫：仅草稿态可编辑
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(STOCK_IN_STATUS_NOT_ALLOWED);
        }

        StockInDO updateObj = BeanUtils.toBean(updateReqVO, StockInDO.class);
        updateObj.setStatus(null); // 禁止通过 update 篡改单据状态

        // 3. 编号：若有传且变更则查重；若未传则保持原编号（置 null 避免被覆盖为空）
        if (StringUtils.hasText(updateObj.getInNo())) {
            validateInNoUnique(updateObj.getId(), updateObj.getInNo());
        } else {
            updateObj.setInNo(null);
        }

        // 4. 关联存在性校验与快照回填
        fillSnapshotsAndValidateRefs(updateObj, updateReqVO);

        // 5. 库位放置归属校验并收集库位信息
        Map<Long, WarehouseLocationDO> locationMap = validateAndGetPlacementLocations(updateObj.getWarehouseId(), updateObj.getWarehouseName(), updateReqVO.getItems());

        // 6. 两级子表差异保存（先父明细后子放置）并权威重算总金额
        List<StockInItemDO> activeItems = differentialSaveItemsAndPlacements(updateReqVO.getId(), locationMap, updateReqVO.getItems());
        updateObj.setTotalAmount(recalculateTotalAmount(activeItems));

        // 7. 更新主表
        stockInMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockIn(Long id) {
        // 1. 校验存在
        StockInDO existing = validateStockInExists(id);

        // 2. 状态守卫：仅草稿态可删除
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(STOCK_IN_STATUS_NOT_ALLOWED);
        }

        // 3. 逻辑删除主表 + 级联逻辑删除明细及放置记录
        stockInMapper.deleteById(id);
        stockInItemMapper.deleteByStockInId(id);
        stockInItemPlacementMapper.deleteByStockInId(id);
    }

    @Override
    public StockInRespVO getStockIn(Long id) {
        StockInDO stockIn = validateStockInExists(id);
        StockInRespVO respVO = BeanUtils.toBean(stockIn, StockInRespVO.class);

        // 嵌套返回明细及库位分配去向
        List<StockInItemDO> itemDOList = stockInItemMapper.selectListByStockInId(id);
        if (CollUtil.isNotEmpty(itemDOList)) {
            List<StockInItemPlacementDO> placementDOList = stockInItemPlacementMapper.selectListByStockInId(id);
            Map<Long, List<StockInItemPlacementDO>> placementsByItemId = CollUtil.isEmpty(placementDOList)
                    ? Collections.emptyMap()
                    : placementDOList.stream().collect(Collectors.groupingBy(StockInItemPlacementDO::getStockInItemId));

            List<StockInItemRespVO> itemRespVOList = new ArrayList<>();
            for (StockInItemDO itemDO : itemDOList) {
                StockInItemRespVO itemResp = BeanUtils.toBean(itemDO, StockInItemRespVO.class);
                List<StockInItemPlacementDO> itemPlacements = placementsByItemId.getOrDefault(itemDO.getId(), Collections.emptyList());
                itemResp.setPlacements(BeanUtils.toBean(itemPlacements, StockInItemPlacementRespVO.class));
                itemRespVOList.add(itemResp);
            }
            respVO.setItems(itemRespVOList);
        } else {
            respVO.setItems(Collections.emptyList());
        }
        return respVO;
    }

    @Override
    public PageResult<StockInRespVO> getStockInPage(StockInPageReqVO reqVO) {
        LambdaQueryWrapperX<StockInDO> wrapper = buildPageWrapper(reqVO);
        PageResult<StockInDO> pageResult = stockInMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, StockInRespVO.class);
    }

    @Override
    public String getNextInNo(LocalDate inDate) {
        // 只读预览、不占号：直接扫描同前缀单号（含已删除行，ADR-0015）
        LocalDate date = inDate != null ? inDate : LocalDate.now();
        return generateInNo(date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditStockIn(Long id) {
        // 1. 校验存在
        StockInDO existing = validateStockInExists(id);

        // 2. 状态机守卫：仅草稿态可审核，否则报 1_030_011_002
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(STOCK_IN_STATUS_NOT_ALLOWED);
        }

        // 3. 明细非空校验：零明细不可审核，报 1_030_011_005
        List<StockInItemDO> items = stockInItemMapper.selectListByStockInId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(STOCK_IN_ITEM_EMPTY);
        }

        // 4. 更新单据状态为 COMPLETED
        StockInDO updateObj = new StockInDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_COMPLETED);
        stockInMapper.updateById(updateObj);

        // 5. 过账流水展开写入（同一事务，ADR-0016, ADR-0017）
        List<StockInItemPlacementDO> placementList = stockInItemPlacementMapper.selectListByStockInId(id);
        Map<Long, List<StockInItemPlacementDO>> placementsByItemId = CollUtil.isEmpty(placementList)
                ? Collections.emptyMap()
                : placementList.stream().collect(Collectors.groupingBy(StockInItemPlacementDO::getStockInItemId));

        for (StockInItemDO item : items) {
            List<StockInItemPlacementDO> itemPlacements = placementsByItemId.get(item.getId());
            if (CollUtil.isNotEmpty(itemPlacements)) {
                // 有 placement：按放置记录逐条写入，带 location
                for (StockInItemPlacementDO placement : itemPlacements) {
                    StockLedgerDO ledger = buildBaseStockLedger(existing, item);
                    ledger.setLocationId(placement.getLocationId());
                    ledger.setLocationCode(placement.getLocationCode());
                    ledger.setTxnType(TXN_TYPE_STOCK_IN);
                    ledger.setQuantity(placement.getQuantity());
                    ledger.setUnit(StringUtils.hasText(placement.getUnit()) ? placement.getUnit() : item.getUnit());
                    if (item.getUnitPrice() != null && placement.getQuantity() != null) {
                        ledger.setAmount(item.getUnitPrice().multiply(placement.getQuantity()).setScale(2, RoundingMode.HALF_UP));
                    } else {
                        ledger.setAmount(null);
                    }
                    ledger.setRemark(StringUtils.hasText(placement.getRemark()) ? placement.getRemark() : item.getRemark());
                    stockLedgerMapper.insert(ledger);
                }
            } else {
                // 无 placement：按明细行写入一条，location 为空
                StockLedgerDO ledger = buildBaseStockLedger(existing, item);
                ledger.setLocationId(null);
                ledger.setLocationCode(null);
                ledger.setTxnType(TXN_TYPE_STOCK_IN);
                ledger.setQuantity(item.getQuantity());
                ledger.setUnit(item.getUnit());
                ledger.setAmount(item.getAmount());
                ledger.setRemark(item.getRemark());
                stockLedgerMapper.insert(ledger);
            }
        }

        // 6. 布票状态回写（ADR-0024：入库过账同事务把布票状态推进至 STOCKED）
        List<Long> ticketIds = new ArrayList<>();
        for (StockInItemDO item : items) {
            if (item.getWeavingTicketId() != null) {
                ticketIds.add(item.getWeavingTicketId());
            } else if (StringUtils.hasText(item.getPieceNo())) {
                WeavingTicketDO ticket = weavingTicketMapper.selectByCode(item.getPieceNo().trim());
                if (ticket != null) {
                    ticketIds.add(ticket.getId());
                }
            }
        }
        if (CollUtil.isNotEmpty(ticketIds)) {
            List<Long> distinctTicketIds = ticketIds.stream().distinct().collect(Collectors.toList());
            weavingTicketService.markStocked(distinctTicketIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelStockIn(Long id) {
        // 1. 校验存在
        StockInDO existing = validateStockInExists(id);

        if (DEFAULT_STATUS.equals(existing.getStatus())) {
            // 2.1 草稿态作废：校验 :update 权限
            if (securityFrameworkService != null && !securityFrameworkService.hasPermission(PERMISSION_UPDATE)) {
                throw new AccessDeniedException("没有作废入库单草稿权限");
            }
            // 仅作废单据，不写流水
            StockInDO updateObj = new StockInDO();
            updateObj.setId(id);
            updateObj.setStatus(STATUS_CANCELLED);
            stockInMapper.updateById(updateObj);

        } else if (STATUS_COMPLETED.equals(existing.getStatus())) {
            // 2.2 已审核态红冲：校验 :audit 权限
            if (securityFrameworkService != null && !securityFrameworkService.hasPermission(PERMISSION_AUDIT)) {
                throw new AccessDeniedException("没有红冲已审核入库单权限");
            }
            // 更新单据状态为已取消终态
            StockInDO updateObj = new StockInDO();
            updateObj.setId(id);
            updateObj.setStatus(STATUS_CANCELLED);
            stockInMapper.updateById(updateObj);

            // 红冲过账：同事务反查原流水逐条写入负数反向行（原流水永不物理删除）
            List<StockLedgerDO> origList = stockLedgerMapper.selectListByRelatedDoc(DOC_TYPE_STOCK_IN, id, TXN_TYPE_STOCK_IN);

            if (CollUtil.isNotEmpty(origList)) {
                for (StockLedgerDO orig : origList) {
                    StockLedgerDO reversal = new StockLedgerDO();
                    reversal.setItemType(orig.getItemType() != null ? orig.getItemType() : "PRODUCT");
                    reversal.setProductId(orig.getProductId());
                    reversal.setProductCode(orig.getProductCode());
                    reversal.setProductName(orig.getProductName());
                    reversal.setYarnSpecId(orig.getYarnSpecId());
                    reversal.setYarnSpecName(orig.getYarnSpecName());
                    reversal.setWarehouseId(orig.getWarehouseId());
                    reversal.setWarehouseName(orig.getWarehouseName());
                    reversal.setLocationId(orig.getLocationId());
                    reversal.setLocationCode(orig.getLocationCode());
                    reversal.setBatchNo(orig.getBatchNo());
                    reversal.setTxnType(TXN_TYPE_STOCK_IN_REVERSAL);
                    reversal.setQuantity(orig.getQuantity() != null ? orig.getQuantity().negate() : BigDecimal.ZERO);
                    reversal.setUnit(orig.getUnit());
                    reversal.setUnitPrice(orig.getUnitPrice());
                    reversal.setAmount(orig.getAmount() != null ? orig.getAmount().negate() : null);
                    reversal.setRelatedDocType(DOC_TYPE_STOCK_IN);
                    reversal.setRelatedDocId(orig.getRelatedDocId());
                    reversal.setRelatedDocNo(orig.getRelatedDocNo());
                    reversal.setRemark(orig.getRemark());
                    stockLedgerMapper.insert(reversal);
                }
            }
        } else {
            // 2.3 CANCELLED 终态或非法状态：报 1_030_011_002
            throw exception(STOCK_IN_STATUS_NOT_ALLOWED);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 校验入库单存在
     */
    private StockInDO validateStockInExists(Long id) {
        if (id == null) {
            throw exception(STOCK_IN_NOT_EXISTS);
        }
        StockInDO stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            throw exception(STOCK_IN_NOT_EXISTS);
        }
        return stockIn;
    }

    /**
     * 校验单号租户内全量唯一（含已删除行，ADR-0015）
     */
    private void validateInNoUnique(Long excludeId, String inNo) {
        if (!StringUtils.hasText(inNo)) {
            return;
        }
        StockInDO exist = stockInMapper.selectByInNoIncludingDeleted(inNo);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(STOCK_IN_NO_DUPLICATE, inNo);
        }
    }

    /**
     * 自动取号（带顺延重试）
     */
    private String generateInNoWithRetry(LocalDate inDate) {
        String lastCandidate = null;
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateInNo(inDate);
            lastCandidate = candidate;
            if (stockInMapper.selectByInNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(STOCK_IN_NO_DUPLICATE, lastCandidate);
    }

    /**
     * 生成单号：IN- + YYYYMMDD + - + 3位顺序号（超 999 自然扩位）
     */
    private String generateInNo(LocalDate inDate) {
        LocalDate date = inDate != null ? inDate : LocalDate.now();
        String prefix = ORDER_NO_PREFIX + date.format(DATE_FORMATTER) + "-";
        List<StockInDO> candidates = stockInMapper.selectListByInNoPrefixIncludingDeleted(prefix);

        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (StockInDO candidate : candidates) {
            String no = candidate.getInNo();
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
        if (maxNum == 0L) {
            return prefix + "001";
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    /**
     * 校验关联实体存在性并回填快照
     */
    private void fillSnapshotsAndValidateRefs(StockInDO stockIn, StockInSaveReqVO reqVO) {
        // 1. 入库仓库（必填且必须为 ACTIVE 启用状态）
        WarehouseDO warehouse = warehouseMapper.selectById(reqVO.getWarehouseId());
        if (warehouse == null || !"ACTIVE".equals(warehouse.getStatus())) {
            throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "仓库");
        }
        stockIn.setWarehouseName(warehouse.getName());

        // 2. 供应商（可选）
        if (reqVO.getSupplierId() != null) {
            SupplierDO supplier = supplierMapper.selectById(reqVO.getSupplierId());
            if (supplier == null) {
                throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "供应商");
            }
            stockIn.setSupplierName(supplier.getCompanyName());
        } else {
            stockIn.setSupplierName(null);
        }

        // 3. 销售合同（可选）
        if (reqVO.getSalesContractId() != null) {
            SalesContractDO contract = salesContractMapper.selectById(reqVO.getSalesContractId());
            if (contract == null) {
                throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "销售合同");
            }
            stockIn.setContractNo(contract.getContractNo());
        } else {
            stockIn.setContractNo(null);
        }

        // 4. 胚布单（可选）
        if (reqVO.getGreigeOrderId() != null) {
            GreigeOrderDO greigeOrder = greigeOrderMapper.selectById(reqVO.getGreigeOrderId());
            if (greigeOrder == null) {
                throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "胚布单");
            }
            stockIn.setGreigeOrderNo(greigeOrder.getOrderNo());
        } else {
            stockIn.setGreigeOrderNo(null);
        }

        // 5. 染色单（可选，染色单无单号快照）
        if (reqVO.getDyeingOrderId() != null) {
            DyeingOrderDO dyeingOrder = dyeingOrderMapper.selectById(reqVO.getDyeingOrderId());
            if (dyeingOrder == null) {
                throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "染色单");
            }
        }

        // 6. 胚布品种（可选）
        if (reqVO.getGreigeTypeId() != null) {
            GreigeTypeDO greigeType = greigeTypeMapper.selectById(reqVO.getGreigeTypeId());
            if (greigeType == null) {
                throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "胚布品种");
            }
            stockIn.setGreigeTypeName(greigeType.getName());
        } else {
            stockIn.setGreigeTypeName(null);
        }
    }

    /**
     * 构建单条明细 DO 并回填产品/规格快照与金额
     */
    private StockInItemDO buildItemDO(StockInItemSaveReqVO itemReq) {
        StockInItemDO itemDO = BeanUtils.toBean(itemReq, StockInItemDO.class);

        // 1. 物品类型缺省 PRODUCT 并校验合法性
        String itemType = StringUtils.hasText(itemReq.getItemType()) ? itemReq.getItemType().trim() : "PRODUCT";
        if (!"PRODUCT".equals(itemType) && !"YARN_SPEC".equals(itemType)) {
            throw exception(STOCK_IN_ITEM_TYPE_INVALID, itemType);
        }
        itemDO.setItemType(itemType);

        // 2. 根据物品类型进行产品 / 纱线规格互斥校验与快照回填
        if ("PRODUCT".equals(itemType)) {
            if (itemReq.getProductId() == null || itemReq.getYarnSpecId() != null) {
                throw exception(STOCK_IN_ITEM_MUTEX_FAILED);
            }
            ProductDO product = productMapper.selectById(itemReq.getProductId());
            if (product == null) {
                throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "产品");
            }
            itemDO.setProductId(product.getId());
            itemDO.setProductCode(product.getCode());
            itemDO.setProductName(product.getName());
            itemDO.setYarnSpecId(null);
            itemDO.setYarnSpecName(null);
        } else {
            // YARN_SPEC
            if (itemReq.getYarnSpecId() == null || itemReq.getProductId() != null) {
                throw exception(STOCK_IN_ITEM_MUTEX_FAILED);
            }
            // 纱线规格单位固定为 KG
            if (StringUtils.hasText(itemReq.getUnit()) && !"KG".equalsIgnoreCase(itemReq.getUnit().trim())) {
                throw exception(STOCK_IN_ITEM_YARN_UNIT_INVALID);
            }
            itemDO.setUnit("KG");

            YarnSpecDO yarnSpec = yarnSpecMapper.selectById(itemReq.getYarnSpecId());
            if (yarnSpec == null) {
                throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "纱线规格");
            }
            itemDO.setYarnSpecId(yarnSpec.getId());
            itemDO.setYarnSpecName(yarnSpec.getSpecName());
            itemDO.setProductId(null);
            itemDO.setProductCode(null);
            itemDO.setProductName(null);
        }

        // 匹数缺省 1
        if (itemDO.getPieceCount() == null) {
            itemDO.setPieceCount(1);
        }

        // 金额服务端权威重算：数量 * 单价，保留两位小数
        if (itemReq.getQuantity() != null && itemReq.getUnitPrice() != null) {
            itemDO.setAmount(itemReq.getQuantity().multiply(itemReq.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
        } else {
            itemDO.setAmount(null);
        }

        return itemDO;
    }

    /**
     * 构建明细 DO 列表并回填产品快照与金额
     */
    private List<StockInItemDO> buildItemDOList(List<StockInItemSaveReqVO> items) {
        List<StockInItemDO> result = new ArrayList<>();
        if (CollUtil.isEmpty(items)) {
            return result;
        }
        for (StockInItemSaveReqVO itemReq : items) {
            result.add(buildItemDO(itemReq));
        }
        return result;
    }

    /**
     * 入库明细与库位放置两级差异保存（先父明细后子放置，ADR-0032）
     *
     * @param stockInId   入库单 ID
     * @param locationMap 库位映射表
     * @param itemReqs    提交的明细列表
     * @return 本次保存后生效的全部明细 DO 列表（用于主表权威重算总金额）
     */
    private List<StockInItemDO> differentialSaveItemsAndPlacements(Long stockInId,
                                                                  Map<Long, WarehouseLocationDO> locationMap,
                                                                  List<StockInItemSaveReqVO> itemReqs) {
        // 1. 查询 DB 中存量明细与放置记录
        List<StockInItemDO> dbItems = stockInItemMapper.selectListByStockInId(stockInId);
        Map<Long, StockInItemDO> dbItemMap = dbItems.stream()
                .collect(Collectors.toMap(StockInItemDO::getId, item -> item));

        List<StockInItemPlacementDO> dbPlacements = stockInItemPlacementMapper.selectListByStockInId(stockInId);
        Map<Long, StockInItemPlacementDO> dbPlacementMap = dbPlacements.stream()
                .collect(Collectors.toMap(StockInItemPlacementDO::getId, p -> p));
        Map<Long, List<StockInItemPlacementDO>> dbPlacementsByItemId = dbPlacements.stream()
                .collect(Collectors.groupingBy(StockInItemPlacementDO::getStockInItemId));

        List<StockInItemSaveReqVO> safeItemReqs = itemReqs != null ? itemReqs : Collections.emptyList();

        // 2. 校验明细行 ID 归属与唯一性
        Set<Long> submittedItemIds = new HashSet<>();
        for (StockInItemSaveReqVO itemReq : safeItemReqs) {
            if (itemReq.getId() != null) {
                if (!submittedItemIds.add(itemReq.getId())) {
                    throw invalidParamException("入库明细 ID【{}】重复提交", itemReq.getId());
                }
                if (!dbItemMap.containsKey(itemReq.getId())) {
                    throw invalidParamException("入库明细 ID【{}】不属于当前入库单", itemReq.getId());
                }
            }
        }

        // 计算本次被删除的明细行 ID 集
        Set<Long> toDeleteItemIds = new HashSet<>(dbItemMap.keySet());
        toDeleteItemIds.removeAll(submittedItemIds);

        // 3. 防御性校验：放置记录是否引用了已被删除的明细行 ID，或跨父 ID 串联
        Set<Long> submittedPlacementIds = new HashSet<>();
        for (StockInItemSaveReqVO itemReq : safeItemReqs) {
            if (CollUtil.isNotEmpty(itemReq.getPlacements())) {
                for (StockInItemPlacementSaveReqVO pReq : itemReq.getPlacements()) {
                    // 3.1 若放置 payload 显式指定了 stockInItemId
                    if (pReq.getStockInItemId() != null) {
                        if (toDeleteItemIds.contains(pReq.getStockInItemId())) {
                            throw invalidParamException("放置记录引用了已被删除的入库明细 ID【{}】", pReq.getStockInItemId());
                        }
                        if (itemReq.getId() != null && !pReq.getStockInItemId().equals(itemReq.getId())) {
                            throw invalidParamException("放置记录引用的明细 ID【{}】与所属明细【{}】不一致", pReq.getStockInItemId(), itemReq.getId());
                        }
                        if (itemReq.getId() == null) {
                            throw invalidParamException("新增明细行的放置记录不能指定明细 ID【{}】", pReq.getStockInItemId());
                        }
                    }

                    // 3.2 若放置记录包含已有 id（更新既有放置）
                    if (pReq.getId() != null) {
                        if (!submittedPlacementIds.add(pReq.getId())) {
                            throw invalidParamException("放置记录 ID【{}】重复提交", pReq.getId());
                        }
                        StockInItemPlacementDO dbPlacement = dbPlacementMap.get(pReq.getId());
                        if (dbPlacement == null) {
                            throw invalidParamException("放置记录 ID【{}】不属于当前入库单", pReq.getId());
                        }
                        if (toDeleteItemIds.contains(dbPlacement.getStockInItemId())) {
                            throw invalidParamException("放置记录 ID【{}】原所属明细行【{}】已被删除，禁止引用", pReq.getId(), dbPlacement.getStockInItemId());
                        }
                        if (itemReq.getId() != null && !dbPlacement.getStockInItemId().equals(itemReq.getId())) {
                            throw invalidParamException("放置记录 ID【{}】不属于当前明细行【{}】", pReq.getId(), itemReq.getId());
                        }
                        if (itemReq.getId() == null) {
                            throw invalidParamException("不能将已有放置记录【{}】移动到新增明细行", pReq.getId());
                        }
                    }
                }
            }
        }

        // 4. 第一级：父表（入库明细）差异保存
        // 4.1 删除明细行：其下放置记录一并逻辑删除
        if (!toDeleteItemIds.isEmpty()) {
            Set<Long> placementIdsToDelete = new HashSet<>();
            for (Long delItemId : toDeleteItemIds) {
                List<StockInItemPlacementDO> pls = dbPlacementsByItemId.get(delItemId);
                if (CollUtil.isNotEmpty(pls)) {
                    for (StockInItemPlacementDO pl : pls) {
                        placementIdsToDelete.add(pl.getId());
                    }
                }
            }
            if (!placementIdsToDelete.isEmpty()) {
                stockInItemPlacementMapper.deleteByIds(placementIdsToDelete);
            }
            stockInItemMapper.deleteByIds(toDeleteItemIds);
        }

        // 4.2 保存 / 更新明细行
        List<StockInItemDO> activeItemDOs = new ArrayList<>();
        Map<StockInItemSaveReqVO, Long> reqToItemIdMap = new HashMap<>();

        for (StockInItemSaveReqVO itemReq : safeItemReqs) {
            StockInItemDO itemDO = buildItemDO(itemReq);
            itemDO.setStockInId(stockInId);
            if (itemReq.getId() != null) {
                // 已有明细行：updateById（保留行身份）
                itemDO.setId(itemReq.getId());
                stockInItemMapper.updateById(itemDO);
                reqToItemIdMap.put(itemReq, itemReq.getId());
            } else {
                // 新增明细行：insert
                itemDO.setId(null);
                stockInItemMapper.insert(itemDO);
                reqToItemIdMap.put(itemReq, itemDO.getId());
            }
            activeItemDOs.add(itemDO);
        }

        // 5. 第二级：子表（库位放置）差异保存（按明细行独立三分流）
        for (StockInItemSaveReqVO itemReq : safeItemReqs) {
            Long currentItemId = reqToItemIdMap.get(itemReq);
            String itemUnit = itemReq.getUnit();
            if (!StringUtils.hasText(itemUnit) && "YARN_SPEC".equals(itemReq.getItemType())) {
                itemUnit = "KG";
            }

            List<StockInItemPlacementSaveReqVO> reqPlacements = itemReq.getPlacements() != null
                    ? itemReq.getPlacements()
                    : Collections.emptyList();

            Set<Long> currentSubmittedPlacementIds = new HashSet<>();
            List<StockInItemPlacementDO> toInsertPlacements = new ArrayList<>();
            List<StockInItemPlacementDO> toUpdatePlacements = new ArrayList<>();

            for (StockInItemPlacementSaveReqVO pReq : reqPlacements) {
                StockInItemPlacementDO pDO = BeanUtils.toBean(pReq, StockInItemPlacementDO.class);
                pDO.setStockInId(stockInId);
                pDO.setStockInItemId(currentItemId);
                WarehouseLocationDO loc = locationMap.get(pReq.getLocationId());
                pDO.setLocationCode(loc != null ? loc.getCode() : null);
                pDO.setUnit(itemUnit);

                if (pReq.getId() != null) {
                    pDO.setId(pReq.getId());
                    toUpdatePlacements.add(pDO);
                    currentSubmittedPlacementIds.add(pReq.getId());
                } else {
                    pDO.setId(null);
                    toInsertPlacements.add(pDO);
                }
            }

            // DB 中原挂在该明细行下的放置记录
            List<StockInItemPlacementDO> existingChildPlacements = dbPlacementsByItemId.getOrDefault(currentItemId, Collections.emptyList());
            Set<Long> toDeleteChildPlacementIds = existingChildPlacements.stream()
                    .map(StockInItemPlacementDO::getId)
                    .filter(id -> !currentSubmittedPlacementIds.contains(id))
                    .collect(Collectors.toSet());

            if (!toDeleteChildPlacementIds.isEmpty()) {
                stockInItemPlacementMapper.deleteByIds(toDeleteChildPlacementIds);
            }
            for (StockInItemPlacementDO pDO : toUpdatePlacements) {
                stockInItemPlacementMapper.updateById(pDO);
            }
            for (StockInItemPlacementDO pDO : toInsertPlacements) {
                stockInItemPlacementMapper.insert(pDO);
            }
        }

        return activeItemDOs;
    }

    /**
     * 校验库位放置合法性并获取库位映射表：库位必须存在且必须属于本单入库仓库（1_030_011_004）
     */
    private Map<Long, WarehouseLocationDO> validateAndGetPlacementLocations(Long warehouseId, String warehouseName, List<StockInItemSaveReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyMap();
        }
        Map<Long, WarehouseLocationDO> locationMap = new HashMap<>();
        for (StockInItemSaveReqVO item : items) {
            if (CollUtil.isEmpty(item.getPlacements())) {
                continue;
            }
            for (StockInItemPlacementSaveReqVO placement : item.getPlacements()) {
                if (placement.getLocationId() == null) {
                    continue;
                }
                locationMap.computeIfAbsent(placement.getLocationId(), id -> {
                    WarehouseLocationDO loc = warehouseLocationMapper.selectById(id);
                    if (loc == null) {
                        throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "库位");
                    }
                    if (!loc.getWarehouseId().equals(warehouseId)) {
                        throw exception(STOCK_IN_LOCATION_NOT_IN_WAREHOUSE, loc.getCode(), warehouseName);
                    }
                    return loc;
                });
            }
        }
        return locationMap;
    }

    /**
     * 保存明细与库位分配记录（整体替换）
     */
    private void saveItemsAndPlacements(Long stockInId, Map<Long, WarehouseLocationDO> locationMap,
                                       List<StockInItemSaveReqVO> itemReqs, List<StockInItemDO> itemDOList) {
        if (CollUtil.isEmpty(itemReqs) || CollUtil.isEmpty(itemDOList)) {
            return;
        }
        for (int i = 0; i < itemDOList.size(); i++) {
            StockInItemDO itemDO = itemDOList.get(i);
            itemDO.setId(null);
            itemDO.setStockInId(stockInId);
            stockInItemMapper.insert(itemDO);

            StockInItemSaveReqVO itemReq = itemReqs.get(i);
            if (CollUtil.isNotEmpty(itemReq.getPlacements())) {
                for (StockInItemPlacementSaveReqVO placementReq : itemReq.getPlacements()) {
                    WarehouseLocationDO location = locationMap.get(placementReq.getLocationId());
                    StockInItemPlacementDO placementDO = BeanUtils.toBean(placementReq, StockInItemPlacementDO.class);
                    placementDO.setId(null);
                    placementDO.setStockInId(stockInId);
                    placementDO.setStockInItemId(itemDO.getId());
                    placementDO.setLocationCode(location != null ? location.getCode() : null);
                    placementDO.setUnit(itemDO.getUnit());
                    stockInItemPlacementMapper.insert(placementDO);
                }
            }
        }
    }

    /**
     * 权威重算入库单金额合计（明细金额之和；若无金额则返回 null）
     */
    private BigDecimal recalculateTotalAmount(List<StockInItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        boolean hasAmount = false;
        for (StockInItemDO item : items) {
            if (item.getAmount() != null) {
                total = total.add(item.getAmount());
                hasAmount = true;
            }
        }
        return hasAmount ? total.setScale(2, RoundingMode.HALF_UP) : null;
    }

    /**
     * 构造分页查询条件：
     * keyword 对 in_no / supplier_name / contract_no / item product_code / item product_name 做 OR 模糊；
     * inType / status / warehouseId / supplierId 等值过滤；
     * inDate 区间过滤。
     */
    private LambdaQueryWrapperX<StockInDO> buildPageWrapper(StockInPageReqVO reqVO) {
        LambdaQueryWrapperX<StockInDO> wrapper = new LambdaQueryWrapperX<>();

        // 等值过滤
        wrapper.eqIfPresent(StockInDO::getInType, reqVO.getInType());
        wrapper.eqIfPresent(StockInDO::getStatus, reqVO.getStatus());
        wrapper.eqIfPresent(StockInDO::getWarehouseId, reqVO.getWarehouseId());
        wrapper.eqIfPresent(StockInDO::getSupplierId, reqVO.getSupplierId());

        // 日期区间过滤（兼容 inDateStart/End 与 inDate 数组）
        LocalDate dateStart = reqVO.getInDateStart() != null ? reqVO.getInDateStart()
                : (reqVO.getInDate() != null && reqVO.getInDate().length > 0 ? reqVO.getInDate()[0] : null);
        LocalDate dateEnd = reqVO.getInDateEnd() != null ? reqVO.getInDateEnd()
                : (reqVO.getInDate() != null && reqVO.getInDate().length > 1 ? reqVO.getInDate()[1] : null);
        wrapper.betweenIfPresent(StockInDO::getInDate, dateStart, dateEnd);

        // keyword：命中 in_no / supplier_name / contract_no 之一，或明细产品编码/名称
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword + "%";
            Long tenantId = TenantContextHolder.getTenantId();
            wrapper.and(w -> {
                w.apply("LOWER(in_no) LIKE LOWER({0})", like)
                        .or().apply("LOWER(supplier_name) LIKE LOWER({0})", like)
                        .or().apply("LOWER(contract_no) LIKE LOWER({0})", like);
                if (tenantId != null) {
                    w.or().apply("id IN (SELECT stock_in_id FROM lunaerp_stock_in_item WHERE tenant_id = {1} AND deleted = 0 AND (LOWER(product_code) LIKE LOWER({0}) OR LOWER(product_name) LIKE LOWER({0}) OR LOWER(yarn_spec_name) LIKE LOWER({0})))", like, tenantId);
                } else {
                    w.or().apply("id IN (SELECT stock_in_id FROM lunaerp_stock_in_item WHERE deleted = 0 AND (LOWER(product_code) LIKE LOWER({0}) OR LOWER(product_name) LIKE LOWER({0}) OR LOWER(yarn_spec_name) LIKE LOWER({0})))", like);
                }
            });
        }

        wrapper.orderByDesc(StockInDO::getInDate).orderByDesc(StockInDO::getId);
        return wrapper;
    }

    /**
     * 构建基础库存流水台账记录（公共字段映射）
     */
    private StockLedgerDO buildBaseStockLedger(StockInDO stockIn, StockInItemDO item) {
        StockLedgerDO ledger = new StockLedgerDO();
        ledger.setItemType(item.getItemType() != null ? item.getItemType() : "PRODUCT");
        ledger.setProductId(item.getProductId());
        ledger.setProductCode(item.getProductCode());
        ledger.setProductName(item.getProductName());
        ledger.setYarnSpecId(item.getYarnSpecId());
        ledger.setYarnSpecName(item.getYarnSpecName());
        ledger.setWarehouseId(stockIn.getWarehouseId());
        ledger.setWarehouseName(stockIn.getWarehouseName());
        ledger.setBatchNo(item.getBatchNo());
        ledger.setUnitPrice(item.getUnitPrice());
        ledger.setRelatedDocType(DOC_TYPE_STOCK_IN);
        ledger.setRelatedDocId(stockIn.getId());
        ledger.setRelatedDocNo(stockIn.getInNo());
        return ledger;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionOrderGenerateStockInRespVO createStockInFromInspection(InspectionOrderGenerateStockInReqVO reqVO) {
        // 1. 校验检验单存在且状态为 COMPLETED
        InspectionOrderDO inspectionOrder = inspectionOrderMapper.selectById(reqVO.getInspectionOrderId());
        if (inspectionOrder == null) {
            throw exception(INSPECTION_ORDER_NOT_EXISTS);
        }
        if (!"COMPLETED".equals(inspectionOrder.getStatus())) {
            throw exception(INSPECTION_ORDER_STATUS_NOT_COMPLETED);
        }

        // 2. 校验仓库存在且有效
        WarehouseDO warehouse = warehouseMapper.selectById(reqVO.getWarehouseId());
        if (warehouse == null || !"ACTIVE".equals(warehouse.getStatus())) {
            throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "仓库");
        }

        // 3. 查询检验单全部明细
        List<InspectionOrderItemDO> allItems = inspectionOrderItemMapper.selectListByInspectionOrderId(inspectionOrder.getId());
        if (CollUtil.isEmpty(allItems)) {
            throw exception(INSPECTION_ORDER_NO_QUALIFIED_ITEMS);
        }

        // 4. 过滤合格品（grade != null 且 !grade.equalsIgnoreCase("DEFECTIVE")）
        List<InspectionOrderItemDO> candidateItems = allItems.stream()
                .filter(item -> StringUtils.hasText(item.getGrade()) && !"DEFECTIVE".equalsIgnoreCase(item.getGrade().trim()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(candidateItems)) {
            throw exception(INSPECTION_ORDER_NO_QUALIFIED_ITEMS);
        }

        // 5. 幂等查重：检查是否已有非 CANCELLED 的入库单明细引用了该 inspection_order_item_id
        List<Long> candidateItemIds = candidateItems.stream().map(InspectionOrderItemDO::getId).collect(Collectors.toList());
        List<StockInItemDO> existingStockInItems = stockInItemMapper.selectListByInspectionOrderItemIds(candidateItemIds);
        Set<Long> alreadyGeneratedItemIds = new HashSet<>();
        if (CollUtil.isNotEmpty(existingStockInItems)) {
            Map<Long, StockInDO> stockInCache = new HashMap<>();
            for (StockInItemDO existingItem : existingStockInItems) {
                StockInDO parent = stockInCache.computeIfAbsent(existingItem.getStockInId(), id -> stockInMapper.selectById(id));
                if (parent != null && !STATUS_CANCELLED.equals(parent.getStatus())) {
                    alreadyGeneratedItemIds.add(existingItem.getInspectionOrderItemId());
                }
            }
        }

        // 6. 得到本次需要生成的明细
        List<InspectionOrderItemDO> itemsToGenerate = candidateItems.stream()
                .filter(item -> !alreadyGeneratedItemIds.contains(item.getId()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(itemsToGenerate)) {
            throw exception(INSPECTION_ORDER_NO_QUALIFIED_ITEMS);
        }

        // 7. 创建 StockInDO
        StockInDO stockIn = new StockInDO();
        stockIn.setStatus(DEFAULT_STATUS);
        stockIn.setInDate(LocalDate.now());
        stockIn.setInNo(generateInNoWithRetry(stockIn.getInDate()));
        stockIn.setInType("PRODUCTION");
        stockIn.setWarehouseId(warehouse.getId());
        stockIn.setWarehouseName(warehouse.getName());
        stockIn.setInspectionOrderId(inspectionOrder.getId());
        stockIn.setInspectionOrderNo(inspectionOrder.getCode());
        stockIn.setSalesContractId(inspectionOrder.getSalesContractId());
        stockIn.setContractNo(inspectionOrder.getContractNo());
        stockIn.setGreigeOrderId(inspectionOrder.getGreigeOrderId());
        if (inspectionOrder.getGreigeOrderId() != null) {
            GreigeOrderDO greigeOrder = greigeOrderMapper.selectById(inspectionOrder.getGreigeOrderId());
            if (greigeOrder != null) {
                stockIn.setGreigeOrderNo(greigeOrder.getOrderNo());
            }
        }
        stockIn.setDyeingOrderId(inspectionOrder.getDyeingOrderId());
        if (inspectionOrder.getInspectionFactoryId() != null) {
            stockIn.setSupplierId(inspectionOrder.getInspectionFactoryId());
            stockIn.setSupplierName(inspectionOrder.getInspectionFactoryName());
        }
        stockIn.setRemark(StringUtils.hasText(reqVO.getRemark()) ? reqVO.getRemark().trim() : ("由检验单 " + inspectionOrder.getCode() + " 生成"));
        stockInMapper.insert(stockIn);

        // 8. 创建 StockInItemDO 列表
        for (InspectionOrderItemDO item : itemsToGenerate) {
            StockInItemDO itemDO = new StockInItemDO();
            itemDO.setStockInId(stockIn.getId());
            itemDO.setItemType("PRODUCT");
            itemDO.setProductId(inspectionOrder.getProductId());
            itemDO.setProductCode(StringUtils.hasText(item.getProductCode()) ? item.getProductCode() : inspectionOrder.getProductCode());
            itemDO.setProductName(StringUtils.hasText(item.getProductName()) ? item.getProductName() : inspectionOrder.getProductName());
            itemDO.setBatchNo(item.getVatNo());
            itemDO.setRollNo(item.getRollNo());
            itemDO.setPieceNo(item.getPieceNo());
            itemDO.setWeavingTicketId(item.getWeavingTicketId());
            itemDO.setInspectionOrderItemId(item.getId());
            itemDO.setPieceCount(item.getPieceCount() != null ? item.getPieceCount() : 1);

            BigDecimal qty = item.getMeasuredWeight() != null ? item.getMeasuredWeight()
                    : (item.getLengthM() != null ? item.getLengthM() : BigDecimal.ONE);
            itemDO.setQuantity(qty);
            itemDO.setUnit(item.getMeasuredWeight() != null ? "KG" : "M");
            itemDO.setRemark(item.getRemark());
            stockInItemMapper.insert(itemDO);
        }

        int skipped = allItems.size() - itemsToGenerate.size();
        return InspectionOrderGenerateStockInRespVO.builder()
                .stockInId(stockIn.getId())
                .stockInNo(stockIn.getInNo())
                .generatedCount(itemsToGenerate.size())
                .skippedCount(skipped)
                .build();
    }

}
