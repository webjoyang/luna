package com.luna.lunasrv.module.lunaerp.service.stock.stockin;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype.GreigeTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.greigetype.GreigeTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemPlacementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 库存管理 - 入库单 Service 实现类
 *
 * 编号生成：IN-YYYYMMDD-NNN 按天取号、租户内全量唯一含软删行（ADR-0015）；
 * 聚合保存：明细整体替换，服务端权威重算金额（ADR-0003）；
 * 状态机守卫：三态过账状态机，草稿态可编辑/删除，非草稿态报 1_030_011_002（ADR-0016）。
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

    @Resource
    private StockInMapper stockInMapper;

    @Resource
    private StockInItemMapper stockInItemMapper;

    @Resource
    private StockInItemPlacementMapper stockInItemPlacementMapper;

    @Resource
    private WarehouseMapper warehouseMapper;

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

        // 4. 明细聚合：服务端权威重算行金额与主表汇总（允许零明细草稿保存）
        List<StockInItemDO> itemDOList = buildItemDOList(createReqVO.getItems());
        stockIn.setTotalAmount(recalculateTotalAmount(itemDOList));

        // 5. 保存主表与明细
        stockInMapper.insert(stockIn);
        saveItems(stockIn.getId(), itemDOList);

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

        // 3. 编号：若有传且变更则查重；若未传则保持原编号（置 null 避免被覆盖为空）
        if (StringUtils.hasText(updateObj.getInNo())) {
            validateInNoUnique(updateObj.getId(), updateObj.getInNo());
        } else {
            updateObj.setInNo(null);
        }

        // 4. 关联存在性校验与快照回填
        fillSnapshotsAndValidateRefs(updateObj, updateReqVO);

        // 5. 明细整表替换与金额权威重算
        List<StockInItemDO> itemDOList = buildItemDOList(updateReqVO.getItems());
        updateObj.setTotalAmount(recalculateTotalAmount(itemDOList));

        // 6. 更新主表并替换明细
        stockInMapper.updateById(updateObj);
        stockInItemMapper.deleteByStockInId(updateReqVO.getId());
        saveItems(updateReqVO.getId(), itemDOList);
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

        // 嵌套返回明细
        List<StockInItemDO> itemDOList = stockInItemMapper.selectListByStockInId(id);
        respVO.setItems(BeanUtils.toBean(itemDOList, StockInItemRespVO.class));
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
     * 构建明细 DO 列表并回填产品快照与金额
     */
    private List<StockInItemDO> buildItemDOList(List<StockInItemSaveReqVO> items) {
        List<StockInItemDO> result = new ArrayList<>();
        if (CollUtil.isEmpty(items)) {
            return result;
        }

        for (StockInItemSaveReqVO itemReq : items) {
            StockInItemDO itemDO = BeanUtils.toBean(itemReq, StockInItemDO.class);

            // 校验产品存在并回填快照
            ProductDO product = productMapper.selectById(itemReq.getProductId());
            if (product == null) {
                throw exception(STOCK_IN_REFERENCE_NOT_FOUND, "产品");
            }
            itemDO.setProductCode(product.getCode());
            itemDO.setProductName(product.getName());

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

            result.add(itemDO);
        }
        return result;
    }

    /**
     * 批量插入明细
     */
    private void saveItems(Long stockInId, List<StockInItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (StockInItemDO item : items) {
            item.setStockInId(stockInId);
        }
        stockInItemMapper.insertBatch(items);
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
                    w.or().apply("id IN (SELECT stock_in_id FROM lunaerp_stock_in_item WHERE tenant_id = {1} AND deleted = 0 AND (LOWER(product_code) LIKE LOWER({0}) OR LOWER(product_name) LIKE LOWER({0})))", like, tenantId);
                } else {
                    w.or().apply("id IN (SELECT stock_in_id FROM lunaerp_stock_in_item WHERE deleted = 0 AND (LOWER(product_code) LIKE LOWER({0}) OR LOWER(product_name) LIKE LOWER({0})))", like);
                }
            });
        }

        wrapper.orderByDesc(StockInDO::getInDate).orderByDesc(StockInDO::getId);
        return wrapper;
    }

}
