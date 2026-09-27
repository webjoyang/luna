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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
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
    private ProductMapper productMapper;

    @Resource
    private StockInItemMapper stockInItemMapper;

    @Resource
    private AdminUserApi adminUserApi;

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
