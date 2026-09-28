package com.luna.lunasrv.module.lunaerp.service.business.shipment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.enums.CommonStatusEnum;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.enums.business.shipment.ShipmentSignStatusEnum;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.service.stock.stockledger.StockLedgerService;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 业务管理 - 发货单 Service 实现类
 *
 * 编号生成：SH-YYYYMMDD-NNN 按天取号、租户内全量唯一含软删行（ADR-0029）；
 * 聚合保存：明细与卷清单整表替换，服务端权威重算金额与匹数（ADR-0003, ADR-0030）；
 * 状态机守卫：三态过账状态机，仅草稿态可编辑/删除，非草稿态报 1_030_016_002（ADR-0029）。
 *
 * @author luna
 */
@Service
public class ShipmentServiceImpl implements ShipmentService {

    /**
     * 单号前缀
     */
    private static final String ORDER_NO_PREFIX = "SH-";
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
     * 默认单据状态：草稿
     */
    private static final String DEFAULT_STATUS = "DRAFT";
    /**
     * 单据状态：已发货
     */
    private static final String STATUS_COMPLETED = "COMPLETED";
    /**
     * 单据状态：已取消
     */
    private static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * 关联单据类型：发货单
     */
    private static final String DOC_TYPE_SHIPMENT = "SHIPMENT";
    /**
     * 业务动作类型：发货出库（负数）
     */
    private static final String TXN_TYPE_SHIPMENT_OUT = "SHIPMENT_OUT";
    /**
     * 业务动作类型：发货出库红冲（正数）
     */
    private static final String TXN_TYPE_SHIPMENT_OUT_REVERSAL = "SHIPMENT_OUT_REVERSAL";

    /**
     * 权限码：修改（用于草稿作废）
     */
    private static final String PERMISSION_UPDATE = "lunaerp:business:shipment:update";
    /**
     * 权限码：审核（用于审核及红冲）
     */
    private static final String PERMISSION_AUDIT = "lunaerp:business:shipment:audit";

    /**
     * 默认签收状态：待签收
     */
    private static final String DEFAULT_SIGN_STATUS = "PENDING";
    /**
     * 默认币种：人民币
     */
    private static final String DEFAULT_CURRENCY = "RMB";

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private ShipmentItemMapper shipmentItemMapper;

    @Resource
    private ShipmentRollMapper shipmentRollMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private WarehouseMapper warehouseMapper;

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private SalesContractItemMapper salesContractItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StockInItemMapper stockInItemMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private StockLedgerMapper stockLedgerMapper;

    @Resource
    private StockLedgerService stockLedgerService;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShipment(ShipmentSaveReqVO createReqVO) {
        ShipmentDO shipment = BeanUtils.toBean(createReqVO, ShipmentDO.class);

        // 1. 缺省值
        shipment.setStatus(DEFAULT_STATUS);
        shipment.setSignStatus(DEFAULT_SIGN_STATUS);
        if (!StringUtils.hasText(shipment.getCurrency())) {
            shipment.setCurrency(DEFAULT_CURRENCY);
        }
        if (shipment.getShippingDate() == null) {
            shipment.setShippingDate(LocalDate.now());
        }
        if (shipment.getFreightFee() == null) {
            shipment.setFreightFee(BigDecimal.ZERO);
        }

        // 2. 编号：手填查重（含已删除行，撞号直接报错）；留空按天自动生成（有限次顺延重试）
        if (StringUtils.hasText(shipment.getShipmentNo())) {
            validateShipmentNoUnique(null, shipment.getShipmentNo());
        } else {
            shipment.setShipmentNo(generateShipmentNoWithRetry(shipment.getShippingDate()));
        }

        // 3. 关联存在性校验与快照回填
        fillSnapshotsAndValidateRefs(shipment, createReqVO);

        // 4. 明细聚合：服务端权威重算行金额与主表汇总（允许零明细草稿保存）
        List<ShipmentItemDO> itemDOList = buildItemDOList(createReqVO.getItems());
        shipment.setTotalAmount(recalculateTotalAmount(itemDOList));
        shipment.setTotalPieces(recalculateTotalPieces(itemDOList));

        // 5. 保存主表与明细及卷清单
        shipmentMapper.insert(shipment);
        saveItemsAndRolls(shipment.getId(), createReqVO.getItems(), itemDOList);

        return shipment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShipment(ShipmentSaveReqVO updateReqVO) {
        // 1. 校验发货单存在
        ShipmentDO existing = validateShipmentExists(updateReqVO.getId());

        // 2. 状态守卫：仅草稿态可编辑
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(SHIPMENT_STATUS_NOT_ALLOW);
        }

        ShipmentDO updateObj = BeanUtils.toBean(updateReqVO, ShipmentDO.class);
        updateObj.setStatus(null); // 禁止通过 update 篡改单据状态
        updateObj.setSignStatus(null); // 禁止通过草稿 update 篡改签收状态
        updateObj.setSignedByName(null);
        updateObj.setSignedAt(null);
        updateObj.setSignRemark(null);

        // 3. 编号：若有传且变更则查重；若未传则保持原编号（置 null 避免被覆盖为空）
        if (StringUtils.hasText(updateObj.getShipmentNo())) {
            validateShipmentNoUnique(updateObj.getId(), updateObj.getShipmentNo());
        } else {
            updateObj.setShipmentNo(null);
        }

        // 4. 关联存在性校验与快照回填
        fillSnapshotsAndValidateRefs(updateObj, updateReqVO);

        // 5. 明细整表替换与金额/匹数权威重算
        List<ShipmentItemDO> itemDOList = buildItemDOList(updateReqVO.getItems());
        updateObj.setTotalAmount(recalculateTotalAmount(itemDOList));
        updateObj.setTotalPieces(recalculateTotalPieces(itemDOList));

        // 6. 更新主表并替换明细及卷清单
        shipmentMapper.updateById(updateObj);
        shipmentRollMapper.deleteByShipmentId(updateReqVO.getId());
        shipmentItemMapper.deleteByShipmentId(updateReqVO.getId());
        saveItemsAndRolls(updateReqVO.getId(), updateReqVO.getItems(), itemDOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShipment(Long id) {
        // 1. 校验存在
        ShipmentDO existing = validateShipmentExists(id);

        // 2. 状态守卫：仅草稿态可删除
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(SHIPMENT_STATUS_NOT_ALLOW);
        }

        // 3. 逻辑删除主表 + 级联逻辑删除明细及卷清单
        shipmentMapper.deleteById(id);
        shipmentItemMapper.deleteByShipmentId(id);
        shipmentRollMapper.deleteByShipmentId(id);
    }

    @Override
    public ShipmentRespVO getShipment(Long id) {
        ShipmentDO shipment = validateShipmentExists(id);
        ShipmentRespVO respVO = BeanUtils.toBean(shipment, ShipmentRespVO.class);

        // 嵌套返回明细及卷清单
        List<ShipmentItemDO> itemDOList = shipmentItemMapper.selectListByShipmentId(id);
        if (CollUtil.isNotEmpty(itemDOList)) {
            List<ShipmentRollDO> rollDOList = shipmentRollMapper.selectListByShipmentId(id);
            Map<Long, List<ShipmentRollDO>> rollsByItemId = CollUtil.isEmpty(rollDOList)
                    ? Collections.emptyMap()
                    : rollDOList.stream().collect(Collectors.groupingBy(ShipmentRollDO::getShipmentItemId));

            List<ShipmentItemRespVO> itemRespVOList = new ArrayList<>();
            for (ShipmentItemDO itemDO : itemDOList) {
                ShipmentItemRespVO itemResp = BeanUtils.toBean(itemDO, ShipmentItemRespVO.class);
                List<ShipmentRollDO> itemRolls = rollsByItemId.getOrDefault(itemDO.getId(), Collections.emptyList());
                itemResp.setRolls(BeanUtils.toBean(itemRolls, ShipmentRollRespVO.class));
                itemRespVOList.add(itemResp);
            }
            respVO.setItems(itemRespVOList);
        } else {
            respVO.setItems(Collections.emptyList());
        }
        return respVO;
    }

    @Override
    public PageResult<ShipmentRespVO> getShipmentPage(ShipmentPageReqVO reqVO) {
        LambdaQueryWrapperX<ShipmentDO> wrapper = buildPageWrapper(reqVO);
        PageResult<ShipmentDO> pageResult = shipmentMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, ShipmentRespVO.class);
    }

    @Override
    public String getNextShipmentNo(LocalDate shippingDate) {
        // 只读预览、不占号：直接扫描同前缀单号（含已删除行，ADR-0029）
        LocalDate date = shippingDate != null ? shippingDate : LocalDate.now();
        return generateShipmentNo(date);
    }

    @Override
    public List<ShipmentItemRespVO> getShipmentItemsFromContract(Long contractId) {
        // 1. 校验销售合同存在，不存在报 1_030_016_003
        if (contractId == null) {
            throw exception(SHIPMENT_RELATED_REF_NOT_EXISTS, "销售合同");
        }
        SalesContractDO contract = salesContractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(SHIPMENT_RELATED_REF_NOT_EXISTS, "销售合同");
        }

        // 2. 查询合同明细列表（按 line_no 升序）
        List<SalesContractItemDO> contractItems = salesContractItemMapper.selectListByContractId(contractId);
        if (CollUtil.isEmpty(contractItems)) {
            return Collections.emptyList();
        }

        // 3. 实时只读计算：按已生效出库的发货单（COMPLETED 态、即已过账写负流水）依产品/客户色号聚合已发数量
        List<ShipmentDO> completedShipments = shipmentMapper.selectListByContractIdAndStatus(contractId, "COMPLETED");
        Map<String, BigDecimal> shippedQtyByKey = new HashMap<>();
        if (CollUtil.isNotEmpty(completedShipments)) {
            List<Long> completedShipmentIds = completedShipments.stream()
                    .map(ShipmentDO::getId)
                    .collect(Collectors.toList());
            List<ShipmentItemDO> shippedItems = shipmentItemMapper.selectListByShipmentIds(completedShipmentIds);
            if (CollUtil.isNotEmpty(shippedItems)) {
                for (ShipmentItemDO shippedItem : shippedItems) {
                    if (shippedItem.getQuantity() != null && shippedItem.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                        String key = makeProductColorKey(shippedItem.getProductCode(), shippedItem.getCustomerColorNo());
                        shippedQtyByKey.merge(key, shippedItem.getQuantity(), BigDecimal::add);
                    }
                }
            }
        }

        // 4. 按合同明细逐行组装预填发货明细：
        //    每行数量缺省为实时计算的"合同未发量"（合同数量 - 已发量，结果 >= 0），不直接落库
        List<ShipmentItemRespVO> result = new ArrayList<>();
        for (SalesContractItemDO contractItem : contractItems) {
            ShipmentItemRespVO resp = new ShipmentItemRespVO();

            // 4.1 产品快照与 ID 反查
            if (StringUtils.hasText(contractItem.getProductCode())) {
                ProductDO product = productMapper.selectByCode(contractItem.getProductCode());
                if (product != null) {
                    resp.setProductId(product.getId());
                    resp.setProductCode(product.getCode());
                    resp.setProductName(StringUtils.hasText(product.getName()) ? product.getName() : contractItem.getProductName());
                } else {
                    resp.setProductCode(contractItem.getProductCode());
                    resp.setProductName(contractItem.getProductName());
                }
            } else {
                resp.setProductName(contractItem.getProductName());
            }

            // 4.2 客户色号与单价
            resp.setCustomerColorNo(contractItem.getCustomerColor());
            BigDecimal unitPrice = contractItem.getUnitPrice() != null ? contractItem.getUnitPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            resp.setUnitPrice(unitPrice);

            // 4.3 合同未发量计算（贪心扣减池，确保每行未发量 >= 0）
            BigDecimal contractQty = contractItem.getQuantity() != null ? contractItem.getQuantity() : BigDecimal.ZERO;
            String key = makeProductColorKey(contractItem.getProductCode(), contractItem.getCustomerColor());
            BigDecimal shippedTotal = shippedQtyByKey.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal unshippedQty;
            if (shippedTotal.compareTo(BigDecimal.ZERO) <= 0) {
                unshippedQty = contractQty;
            } else if (shippedTotal.compareTo(contractQty) >= 0) {
                unshippedQty = BigDecimal.ZERO;
                shippedQtyByKey.put(key, shippedTotal.subtract(contractQty));
            } else {
                unshippedQty = contractQty.subtract(shippedTotal);
                shippedQtyByKey.put(key, BigDecimal.ZERO);
            }
            if (unshippedQty.compareTo(BigDecimal.ZERO) < 0) {
                unshippedQty = BigDecimal.ZERO;
            }
            resp.setQuantity(unshippedQty.setScale(2, RoundingMode.HALF_UP));

            // 4.4 其他字段缺省
            resp.setPieces(1);
            resp.setUnit(StringUtils.hasText(contract.getUnit()) ? contract.getUnit() : "KG");
            resp.setAmount(resp.getQuantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
            resp.setRemark(contractItem.getRemark());

            result.add(resp);
        }
        return result;
    }

    private String makeProductColorKey(String productCode, String customerColor) {
        String p = productCode == null ? "" : productCode.trim().toLowerCase();
        String c = customerColor == null ? "" : customerColor.trim().toLowerCase();
        return p + "::" + c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditShipment(Long id) {
        // 1. 校验发货单存在
        ShipmentDO existing = validateShipmentExists(id);

        // 2. 状态机守卫：仅草稿态可审核，否则报 1_030_016_002
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(SHIPMENT_STATUS_NOT_ALLOW);
        }

        // 3. 明细非空校验：零明细不可审核，报 1_030_016_004
        List<ShipmentItemDO> items = shipmentItemMapper.selectListByShipmentId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(SHIPMENT_AUDIT_NO_ITEMS);
        }

        // 4. 库存硬校验：按产品+单位+仓库合并需求量对比实时余额，不足硬拦截报 1_030_016_005（不允许负库存）
        Map<String, BigDecimal> requiredQtyMap = new LinkedHashMap<>();
        Map<String, ShipmentItemDO> itemGroupMap = new LinkedHashMap<>();
        for (ShipmentItemDO item : items) {
            String key = item.getProductId() + "_" + item.getUnit();
            BigDecimal current = requiredQtyMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            requiredQtyMap.put(key, current.add(qty));
            itemGroupMap.putIfAbsent(key, item);
        }

        for (Map.Entry<String, BigDecimal> entry : requiredQtyMap.entrySet()) {
            String key = entry.getKey();
            BigDecimal requiredQty = entry.getValue();
            ShipmentItemDO sampleItem = itemGroupMap.get(key);
            BigDecimal availableQty = stockLedgerService.getProductBalance(
                    existing.getWarehouseId(), sampleItem.getProductId(), sampleItem.getUnit());
            if (availableQty == null) {
                availableQty = BigDecimal.ZERO;
            }
            if (requiredQty.compareTo(availableQty) > 0) {
                String productName = StringUtils.hasText(sampleItem.getProductName()) ? sampleItem.getProductName() : sampleItem.getProductCode();
                throw exception(SHIPMENT_STOCK_NOT_ENOUGH, productName, requiredQty, sampleItem.getUnit(), availableQty, sampleItem.getUnit());
            }
        }

        // 5. 更新单据状态为 COMPLETED
        ShipmentDO updateObj = new ShipmentDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_COMPLETED);
        shipmentMapper.updateById(updateObj);

        // 6. 按明细行逐条写负数 SHIPMENT_OUT 流水（同一事务，卷清单不展开，库位为空）
        for (ShipmentItemDO item : items) {
            StockLedgerDO ledger = new StockLedgerDO();
            ledger.setItemType("PRODUCT");
            ledger.setProductId(item.getProductId());
            ledger.setProductCode(item.getProductCode());
            ledger.setProductName(item.getProductName());
            ledger.setWarehouseId(existing.getWarehouseId());
            ledger.setWarehouseName(existing.getWarehouseName());
            ledger.setLocationId(null);
            ledger.setLocationCode(null);
            ledger.setBatchNo(item.getVatNo());
            ledger.setTxnType(TXN_TYPE_SHIPMENT_OUT);
            ledger.setQuantity(item.getQuantity() != null ? item.getQuantity().negate() : BigDecimal.ZERO);
            ledger.setUnit(item.getUnit());
            ledger.setUnitPrice(item.getUnitPrice());
            ledger.setAmount(item.getAmount() != null ? item.getAmount().negate() : null);
            ledger.setRelatedDocType(DOC_TYPE_SHIPMENT);
            ledger.setRelatedDocId(existing.getId());
            ledger.setRelatedDocNo(existing.getShipmentNo());
            ledger.setRemark(item.getRemark());
            stockLedgerMapper.insert(ledger);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelShipment(Long id) {
        // 1. 校验发货单存在
        ShipmentDO existing = validateShipmentExists(id);

        if (DEFAULT_STATUS.equals(existing.getStatus())) {
            // 2.1 草稿态作废：校验 :update 权限
            if (securityFrameworkService != null && !securityFrameworkService.hasPermission(PERMISSION_UPDATE)) {
                throw new AccessDeniedException("没有作废发货单草稿权限");
            }
            // 仅作废单据，不写流水
            ShipmentDO updateObj = new ShipmentDO();
            updateObj.setId(id);
            updateObj.setStatus(STATUS_CANCELLED);
            shipmentMapper.updateById(updateObj);

        } else if (STATUS_COMPLETED.equals(existing.getStatus())) {
            // 2.2 已发货态红冲：校验 :audit 权限
            if (securityFrameworkService != null && !securityFrameworkService.hasPermission(PERMISSION_AUDIT)) {
                throw new AccessDeniedException("没有红冲已发货发货单权限");
            }
            // 更新单据状态为已取消终态
            ShipmentDO updateObj = new ShipmentDO();
            updateObj.setId(id);
            updateObj.setStatus(STATUS_CANCELLED);
            shipmentMapper.updateById(updateObj);

            // 红冲过账：同事务反查原流水逐条追加正数反向行（原流水永不物理删除）
            List<StockLedgerDO> origList = stockLedgerMapper.selectListByRelatedDoc(DOC_TYPE_SHIPMENT, id, TXN_TYPE_SHIPMENT_OUT);
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
                    reversal.setTxnType(TXN_TYPE_SHIPMENT_OUT_REVERSAL);
                    reversal.setQuantity(orig.getQuantity() != null ? orig.getQuantity().negate() : BigDecimal.ZERO);
                    reversal.setUnit(orig.getUnit());
                    reversal.setUnitPrice(orig.getUnitPrice());
                    reversal.setAmount(orig.getAmount() != null ? orig.getAmount().negate() : null);
                    reversal.setRelatedDocType(DOC_TYPE_SHIPMENT);
                    reversal.setRelatedDocId(orig.getRelatedDocId());
                    reversal.setRelatedDocNo(orig.getRelatedDocNo());
                    reversal.setRemark(orig.getRemark());
                    stockLedgerMapper.insert(reversal);
                }
            }
        } else {
            // 2.3 CANCELLED 终态或非法状态：报 1_030_016_002
            throw exception(SHIPMENT_STATUS_NOT_ALLOW);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShipmentSign(ShipmentUpdateSignReqVO reqVO) {
        // 1. 校验发货单存在
        ShipmentDO existing = validateShipmentExists(reqVO.getId());

        // 2. 状态守卫：仅已发货态 COMPLETED 允许登记签收，草稿与已取消报 1_030_016_002
        if (!STATUS_COMPLETED.equals(existing.getStatus())) {
            throw exception(SHIPMENT_STATUS_NOT_ALLOW);
        }

        // 3. 签收状态合法性校验：必须是 PENDING / SIGNED / REFUSED 之一，非法报 1_030_016_006
        if (!ShipmentSignStatusEnum.isValid(reqVO.getSignStatus())) {
            throw exception(SHIPMENT_SIGN_STATUS_INVALID);
        }

        // 4. 更新主表签收字段（拒签只登记状态，不自动红冲、不触发任何库存动作）
        ShipmentDO updateObj = new ShipmentDO();
        updateObj.setId(existing.getId());
        updateObj.setSignStatus(reqVO.getSignStatus());
        updateObj.setSignedByName(reqVO.getSignedByName());
        updateObj.setSignedAt(reqVO.getSignedAt());
        updateObj.setSignRemark(reqVO.getSignRemark());
        shipmentMapper.updateById(updateObj);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验发货单存在
     */
    private ShipmentDO validateShipmentExists(Long id) {
        if (id == null) {
            throw exception(SHIPMENT_NOT_EXISTS);
        }
        ShipmentDO shipment = shipmentMapper.selectById(id);
        if (shipment == null) {
            throw exception(SHIPMENT_NOT_EXISTS);
        }
        return shipment;
    }

    /**
     * 校验单号租户内全量唯一（含已删除行，ADR-0029）
     */
    private void validateShipmentNoUnique(Long excludeId, String shipmentNo) {
        if (!StringUtils.hasText(shipmentNo)) {
            return;
        }
        ShipmentDO exist = shipmentMapper.selectByShipmentNoIncludingDeleted(shipmentNo);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(SHIPMENT_NO_DUPLICATE, shipmentNo);
        }
    }

    /**
     * 自动取号（带顺延重试）
     */
    private String generateShipmentNoWithRetry(LocalDate shippingDate) {
        String lastCandidate = null;
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateShipmentNo(shippingDate);
            lastCandidate = candidate;
            if (shipmentMapper.selectByShipmentNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(SHIPMENT_NO_DUPLICATE, lastCandidate);
    }

    /**
     * 生成单号：SH- + YYYYMMDD + - + 3位顺序号（超 999 自然扩位）
     */
    private String generateShipmentNo(LocalDate shippingDate) {
        LocalDate date = shippingDate != null ? shippingDate : LocalDate.now();
        String prefix = ORDER_NO_PREFIX + date.format(DATE_FORMATTER) + "-";
        List<ShipmentDO> candidates = shipmentMapper.selectListByShipmentNoPrefixIncludingDeleted(prefix);

        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (ShipmentDO candidate : candidates) {
            String no = candidate.getShipmentNo();
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
    private void fillSnapshotsAndValidateRefs(ShipmentDO shipment, ShipmentSaveReqVO reqVO) {
        // 1. 客户（必填且必须为 ACTIVE 启用状态）
        CustomerDO customer = customerMapper.selectById(reqVO.getCustomerId());
        if (customer == null || !"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            throw exception(SHIPMENT_RELATED_REF_NOT_EXISTS, "客户");
        }
        shipment.setCustomerName(customer.getCompanyName());

        // 2. 发货仓库（必填且必须为 ACTIVE 启用状态）
        WarehouseDO warehouse = warehouseMapper.selectById(reqVO.getWarehouseId());
        if (warehouse == null || !"ACTIVE".equalsIgnoreCase(warehouse.getStatus())) {
            throw exception(SHIPMENT_RELATED_REF_NOT_EXISTS, "仓库");
        }
        shipment.setWarehouseName(warehouse.getName());

        // 3. 销售合同（可选）
        if (reqVO.getSalesContractId() != null) {
            SalesContractDO contract = salesContractMapper.selectById(reqVO.getSalesContractId());
            if (contract == null) {
                throw exception(SHIPMENT_RELATED_REF_NOT_EXISTS, "销售合同");
            }
            shipment.setContractNo(contract.getContractNo());
        } else {
            shipment.setContractNo(null);
        }

        // 4. 业务员（可选）
        if (reqVO.getSalespersonUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(reqVO.getSalespersonUserId());
            if (user == null || CommonStatusEnum.isDisable(user.getStatus())) {
                throw exception(SHIPMENT_RELATED_REF_NOT_EXISTS, "业务员");
            }
            shipment.setSalespersonName(user.getNickname());
        } else {
            shipment.setSalespersonName(null);
        }
    }

    /**
     * 校验明细关联并转换为 DO 列表，服务端重算行金额
     */
    private List<ShipmentItemDO> buildItemDOList(List<ShipmentItemSaveReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ShipmentItemDO> result = new ArrayList<>();
        for (ShipmentItemSaveReqVO itemReq : items) {
            ShipmentItemDO itemDO = BeanUtils.toBean(itemReq, ShipmentItemDO.class);

            // 校验产品存在
            ProductDO product = productMapper.selectById(itemReq.getProductId());
            if (product == null) {
                throw exception(SHIPMENT_RELATED_REF_NOT_EXISTS, "产品");
            }
            itemDO.setProductCode(product.getCode());
            itemDO.setProductName(product.getName());

            // 校验来源入库明细（若传）
            if (itemReq.getSourceStockInItemId() != null) {
                StockInItemDO stockInItem = stockInItemMapper.selectById(itemReq.getSourceStockInItemId());
                if (stockInItem == null) {
                    throw exception(SHIPMENT_RELATED_REF_NOT_EXISTS, "来源入库明细");
                }
            }

            // 匹数缺省 1
            if (itemDO.getPieces() == null) {
                itemDO.setPieces(1);
            }

            // 单位缺省 KG
            if (!StringUtils.hasText(itemDO.getUnit())) {
                itemDO.setUnit("KG");
            }

            // 金额服务端权威重算：数量 * 单价，保留两位小数
            if (itemReq.getQuantity() != null && itemReq.getUnitPrice() != null) {
                itemDO.setAmount(itemReq.getQuantity().multiply(itemReq.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
            } else {
                itemDO.setAmount(BigDecimal.ZERO);
            }

            result.add(itemDO);
        }
        return result;
    }

    /**
     * 保存明细与卷清单记录（整体替换）
     */
    private void saveItemsAndRolls(Long shipmentId, List<ShipmentItemSaveReqVO> itemReqs, List<ShipmentItemDO> itemDOList) {
        if (CollUtil.isEmpty(itemReqs) || CollUtil.isEmpty(itemDOList)) {
            return;
        }
        for (int i = 0; i < itemDOList.size(); i++) {
            ShipmentItemDO itemDO = itemDOList.get(i);
            itemDO.setId(null);
            itemDO.setShipmentId(shipmentId);
            shipmentItemMapper.insert(itemDO);

            ShipmentItemSaveReqVO itemReq = itemReqs.get(i);
            if (CollUtil.isNotEmpty(itemReq.getRolls())) {
                for (ShipmentRollSaveReqVO rollReq : itemReq.getRolls()) {
                    ShipmentRollDO rollDO = BeanUtils.toBean(rollReq, ShipmentRollDO.class);
                    rollDO.setId(null);
                    rollDO.setShipmentId(shipmentId);
                    rollDO.setShipmentItemId(itemDO.getId());
                    shipmentRollMapper.insert(rollDO);
                }
            }
        }
    }

    /**
     * 权威重算发货单货物金额合计（明细金额之和）
     */
    private BigDecimal recalculateTotalAmount(List<ShipmentItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ShipmentItemDO item : items) {
            if (item.getAmount() != null) {
                total = total.add(item.getAmount());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 权威重算发货单总匹数（明细匹数之和）
     */
    private Integer recalculateTotalPieces(List<ShipmentItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return 0;
        }
        int total = 0;
        for (ShipmentItemDO item : items) {
            if (item.getPieces() != null) {
                total += item.getPieces();
            }
        }
        return total;
    }

    /**
     * 构造分页查询条件：
     * keyword 对 shipment_no / customer_name / contract_no / roll_no 做 OR 模糊；
     * status / customerId / warehouseId 等值过滤；
     * contractNo 模糊过滤；
     * shippingDate 区间过滤。
     */
    private LambdaQueryWrapperX<ShipmentDO> buildPageWrapper(ShipmentPageReqVO reqVO) {
        LambdaQueryWrapperX<ShipmentDO> wrapper = new LambdaQueryWrapperX<>();

        // 等值过滤
        wrapper.eqIfPresent(ShipmentDO::getStatus, reqVO.getStatus());
        wrapper.eqIfPresent(ShipmentDO::getSignStatus, reqVO.getSignStatus());
        wrapper.eqIfPresent(ShipmentDO::getCustomerId, reqVO.getCustomerId());
        wrapper.eqIfPresent(ShipmentDO::getWarehouseId, reqVO.getWarehouseId());
        wrapper.likeIfPresent(ShipmentDO::getContractNo, reqVO.getContractNo());

        // 日期区间过滤（兼容 shippingDateStart/End 与 shippingDate 数组）
        LocalDate dateStart = reqVO.getShippingDateStart() != null ? reqVO.getShippingDateStart()
                : (reqVO.getShippingDate() != null && reqVO.getShippingDate().length > 0 ? reqVO.getShippingDate()[0] : null);
        LocalDate dateEnd = reqVO.getShippingDateEnd() != null ? reqVO.getShippingDateEnd()
                : (reqVO.getShippingDate() != null && reqVO.getShippingDate().length > 1 ? reqVO.getShippingDate()[1] : null);
        wrapper.betweenIfPresent(ShipmentDO::getShippingDate, dateStart, dateEnd);

        // keyword：命中 shipment_no / customer_name / contract_no 之一，或卷号
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword + "%";
            Long tenantId = TenantContextHolder.getTenantId();
            wrapper.and(w -> {
                w.apply("LOWER(shipment_no) LIKE LOWER({0})", like)
                        .or().apply("LOWER(customer_name) LIKE LOWER({0})", like)
                        .or().apply("LOWER(contract_no) LIKE LOWER({0})", like);
                if (tenantId != null) {
                    w.or().apply("id IN (SELECT shipment_id FROM lunaerp_business_shipment_roll WHERE tenant_id = {1} AND deleted = 0 AND LOWER(roll_no) LIKE LOWER({0}))", like, tenantId);
                } else {
                    w.or().apply("id IN (SELECT shipment_id FROM lunaerp_business_shipment_roll WHERE deleted = 0 AND LOWER(roll_no) LIKE LOWER({0}))", like);
                }
            });
        }

        wrapper.orderByDesc(ShipmentDO::getShippingDate).orderByDesc(ShipmentDO::getId);
        return wrapper;
    }

}
