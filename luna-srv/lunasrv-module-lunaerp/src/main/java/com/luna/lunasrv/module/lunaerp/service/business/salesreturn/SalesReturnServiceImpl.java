package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.datapermission.core.util.DataPermissionUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnReasonEnum;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnStatusEnum;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.*;

/**
 * 业务管理 - 销售退货 Service 实现（ADR-0042）
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class SalesReturnServiceImpl implements SalesReturnService {

    private static final String RETURN_NO_PREFIX = "RT-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern NO_SUFFIX_PATTERN = Pattern.compile("(\\d+)$");
    private static final int MAX_GENERATE_RETRY = 20;

    @Resource
    private SalesReturnMapper salesReturnMapper;

    @Resource
    private SalesReturnItemMapper salesReturnItemMapper;

    @Resource
    private SalesReturnRollMapper salesReturnRollMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private ShipmentItemMapper shipmentItemMapper;

    @Resource
    private ShipmentRollMapper shipmentRollMapper;

    @Resource
    private WarehouseMapper warehouseMapper;

    @Resource
    private SalesReturnAvailabilityService availabilityService;

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @Override
    public List<SalesReturnCandidateRespVO> getShipmentCandidates(Long customerId, String shipmentNo) {
        // 0. 若显式指定发货单号，守卫发货单必须存在、属于当前租户且为 COMPLETED 状态（否则 003）
        if (StrUtil.isNotBlank(shipmentNo)) {
            ShipmentDO targetShipment = shipmentMapper.selectOne(ShipmentDO::getShipmentNo, shipmentNo);
            if (targetShipment == null || !"COMPLETED".equals(targetShipment.getStatus())) {
                throw exception(SALES_RETURN_SHIPMENT_NOT_COMPLETED);
            }
        }

        // 1. 仅查询已发货 COMPLETED 状态的发货单（自定义扩展方法排在原生方法之前，coding-standards.md §1）
        LambdaQueryWrapperX<ShipmentDO> wrapper = new LambdaQueryWrapperX<ShipmentDO>()
                .eqIfPresent(ShipmentDO::getCustomerId, customerId)
                .likeIfPresent(ShipmentDO::getShipmentNo, shipmentNo)
                .eq(ShipmentDO::getStatus, "COMPLETED")
                .orderByDesc(ShipmentDO::getShippingDate)
                .orderByDesc(ShipmentDO::getId);

        List<ShipmentDO> shipments = shipmentMapper.selectList(wrapper);
        if (CollUtil.isEmpty(shipments)) {
            return Collections.emptyList();
        }

        // 2. 批量加载发货明细行
        List<Long> shipmentIds = shipments.stream().map(ShipmentDO::getId).collect(Collectors.toList());
        List<ShipmentItemDO> allItems = shipmentItemMapper.selectList(new LambdaQueryWrapperX<ShipmentItemDO>()
                .in(ShipmentItemDO::getShipmentId, shipmentIds));
        if (CollUtil.isEmpty(allItems)) {
            return Collections.emptyList();
        }

        // 3. 批量计算各行已占量
        List<Long> shipmentItemIds = allItems.stream().map(ShipmentItemDO::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> occupiedMap = availabilityService.getOccupiedQtyMap(shipmentItemIds, null);

        // 4. 按发货单分组判定是否存在实时可退量 > 0 的行
        Map<Long, List<ShipmentItemDO>> itemsByShipment = allItems.stream()
                .collect(Collectors.groupingBy(ShipmentItemDO::getShipmentId));

        List<SalesReturnCandidateRespVO> candidates = new ArrayList<>();
        for (ShipmentDO shipment : shipments) {
            List<ShipmentItemDO> items = itemsByShipment.get(shipment.getId());
            if (CollUtil.isEmpty(items)) {
                continue;
            }
            boolean hasAvailable = false;
            for (ShipmentItemDO item : items) {
                BigDecimal origQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal occupied = occupiedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
                if (origQty.subtract(occupied).compareTo(BigDecimal.ZERO) > 0) {
                    hasAvailable = true;
                    break;
                }
            }
            if (hasAvailable) {
                candidates.add(SalesReturnCandidateRespVO.builder()
                        .id(shipment.getId())
                        .shipmentNo(shipment.getShipmentNo())
                        .customerId(shipment.getCustomerId())
                        .customerName(shipment.getCustomerName())
                        .salesContractId(shipment.getSalesContractId())
                        .contractNo(shipment.getContractNo())
                        .warehouseId(shipment.getWarehouseId())
                        .warehouseName(shipment.getWarehouseName())
                        .currency(shipment.getCurrency())
                        .shippingDate(shipment.getShippingDate())
                        .totalAmount(shipment.getTotalAmount())
                        .totalPieces(shipment.getTotalPieces())
                        .build());
            }
        }
        return candidates;
    }

    @Override
    public SalesReturnShipmentDetailRespVO getShipmentDetail(Long shipmentId) {
        ShipmentDO shipment = validateCompletedShipment(shipmentId);

        // 1. 加载明细行与卷清单
        List<ShipmentItemDO> dbItems = shipmentItemMapper.selectList(new LambdaQueryWrapperX<ShipmentItemDO>()
                .eq(ShipmentItemDO::getShipmentId, shipmentId));
        List<ShipmentRollDO> dbRolls = shipmentRollMapper.selectList(new LambdaQueryWrapperX<ShipmentRollDO>()
                .eq(ShipmentRollDO::getShipmentId, shipmentId));

        Map<Long, List<ShipmentRollDO>> rollsByItem = CollUtil.isEmpty(dbRolls)
                ? Collections.emptyMap()
                : dbRolls.stream().collect(Collectors.groupingBy(ShipmentRollDO::getShipmentItemId));

        // 2. 批量核算各明细行的占量与可退量
        List<Long> itemIds = CollUtil.isEmpty(dbItems) ? Collections.emptyList()
                : dbItems.stream().map(ShipmentItemDO::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> occupiedMap = availabilityService.getOccupiedQtyMap(itemIds, null);

        List<SalesReturnShipmentItemRespVO> itemRespList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dbItems)) {
            for (ShipmentItemDO item : dbItems) {
                BigDecimal origQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal occupied = occupiedMap.getOrDefault(item.getId(), BigDecimal.ZERO);
                BigDecimal available = origQty.subtract(occupied);
                if (available.compareTo(BigDecimal.ZERO) < 0) {
                    available = BigDecimal.ZERO;
                }

                List<ShipmentRollDO> itemRolls = rollsByItem.getOrDefault(item.getId(), Collections.emptyList());
                List<SalesReturnShipmentRollRespVO> rollRespList = itemRolls.stream().map(r ->
                        SalesReturnShipmentRollRespVO.builder()
                                .shipmentRollId(r.getId())
                                .rollNo(r.getRollNo())
                                .weight(r.getWeight())
                                .lengthM(r.getLengthM())
                                .remark(r.getRemark())
                                .build()
                ).collect(Collectors.toList());

                itemRespList.add(SalesReturnShipmentItemRespVO.builder()
                        .shipmentItemId(item.getId())
                        .productId(item.getProductId())
                        .productCode(item.getProductCode())
                        .productName(item.getProductName())
                        .customerColorNo(item.getCustomerColorNo())
                        .colorName(item.getColorName())
                        .vatNo(item.getVatNo())
                        .unit(item.getUnit())
                        .quantity(item.getQuantity())
                        .pieces(item.getPieces())
                        .unitPrice(item.getUnitPrice())
                        .occupiedQty(occupied)
                        .availableQty(available)
                        .rolls(rollRespList)
                        .build());
            }
        }

        return SalesReturnShipmentDetailRespVO.builder()
                .shipmentId(shipment.getId())
                .shipmentNo(shipment.getShipmentNo())
                .customerId(shipment.getCustomerId())
                .customerName(shipment.getCustomerName())
                .salesContractId(shipment.getSalesContractId())
                .contractNo(shipment.getContractNo())
                .warehouseId(shipment.getWarehouseId())
                .warehouseName(shipment.getWarehouseName())
                .currency(shipment.getCurrency())
                .shippingDate(shipment.getShippingDate())
                .items(itemRespList)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSalesReturn(SalesReturnCreateReqVO createReqVO) {
        // 1. 校验来源发货单存在且为 COMPLETED 状态
        ShipmentDO shipment = validateCompletedShipment(createReqVO.getShipmentId());

        // 2. 校验退货仓库存在
        WarehouseDO warehouse = warehouseMapper.selectById(createReqVO.getWarehouseId());
        if (warehouse == null) {
            throw invalidParamException("退货仓库不存在");
        }

        // 3. 校验发货单明细与卷的血缘归属、卷信息合法性及防超退
        validateItemsAndRolls(shipment, createReqVO.getItems(), null);

        // 4. 单号处理：自动按天幂等取号
        String returnNo = generateReturnNoWithRetry(createReqVO.getReturnDate());

        // 5. 组装主表 DO（快照来自发货头单）
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setReturnNo(returnNo);
        salesReturn.setStatus(SalesReturnStatusEnum.DRAFT.getValue());
        salesReturn.setApprovalStatus("PENDING");
        salesReturn.setShipmentId(shipment.getId());
        salesReturn.setShipmentNo(shipment.getShipmentNo());
        salesReturn.setCustomerId(shipment.getCustomerId());
        salesReturn.setCustomerName(shipment.getCustomerName());
        salesReturn.setSalesContractId(shipment.getSalesContractId());
        salesReturn.setContractNo(shipment.getContractNo());
        salesReturn.setWarehouseId(warehouse.getId());
        salesReturn.setWarehouseName(warehouse.getName());
        salesReturn.setCurrency(shipment.getCurrency());
        salesReturn.setReturnDate(createReqVO.getReturnDate() != null ? createReqVO.getReturnDate() : LocalDate.now());
        salesReturn.setRemark(createReqVO.getRemark());

        // 6. 服务端权威重算明细行金额与主表汇总
        List<SalesReturnItemDO> itemDOList = buildItemDOList(shipment, createReqVO.getItems(), null);
        salesReturn.setTotalAmount(calculateTotalAmount(itemDOList));
        salesReturn.setTotalPieces(calculateTotalPieces(itemDOList));

        salesReturnMapper.insert(salesReturn);

        // 7. 保存行表与卷表
        saveItemsAndRolls(salesReturn.getId(), createReqVO.getItems(), itemDOList);

        return salesReturn.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalesReturn(SalesReturnUpdateReqVO updateReqVO) {
        // 1. 校验销售退货单存在
        SalesReturnDO existing = validateSalesReturnExists(updateReqVO.getId());

        // 2. 状态机守卫：仅草稿态（approvalStatus 为 PENDING 或 REJECTED）可编辑
        if (!SalesReturnStatusEnum.DRAFT.getValue().equals(existing.getStatus())
                || (!"PENDING".equals(existing.getApprovalStatus()) && !"REJECTED".equals(existing.getApprovalStatus()))) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }

        // 3. 校验关联发货单与退货仓库
        ShipmentDO shipment = validateCompletedShipment(existing.getShipmentId());
        WarehouseDO warehouse = warehouseMapper.selectById(updateReqVO.getWarehouseId());
        if (warehouse == null) {
            throw invalidParamException("退货仓库不存在");
        }

        // 4. 校验明细与卷（传入 excludeReturnId 排除自身占量）
        validateItemsAndRolls(shipment, updateReqVO.getItems(), existing.getId());

        // 5. 更新主表可变字段（coding-standards.md §14 Update VO 属性极小集原则）
        existing.setWarehouseId(warehouse.getId());
        existing.setWarehouseName(warehouse.getName());
        if (updateReqVO.getReturnDate() != null) {
            existing.setReturnDate(updateReqVO.getReturnDate());
        }
        existing.setRemark(updateReqVO.getRemark());

        // 6. 明细与卷两级差异保存（ADR-0032）
        diffSaveItemsAndRolls(existing.getId(), shipment, updateReqVO.getItems(), existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalesReturn(Long id) {
        // 1. 校验存在
        SalesReturnDO existing = validateSalesReturnExists(id);

        // 2. 状态机守卫：仅草稿态（PENDING/REJECTED）可删除
        if (!SalesReturnStatusEnum.DRAFT.getValue().equals(existing.getStatus())
                || (!"PENDING".equals(existing.getApprovalStatus()) && !"REJECTED".equals(existing.getApprovalStatus()))) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }

        // 3. 级联软删除主表、明细表与卷表
        salesReturnMapper.deleteById(id);
        salesReturnItemMapper.delete(new LambdaQueryWrapperX<SalesReturnItemDO>()
                .eq(SalesReturnItemDO::getReturnId, id));
        salesReturnRollMapper.delete(new LambdaQueryWrapperX<SalesReturnRollDO>()
                .eq(SalesReturnRollDO::getReturnId, id));
    }

    @Override
    public SalesReturnRespVO getSalesReturn(Long id) {
        SalesReturnDO salesReturn = validateSalesReturnExists(id);
        SalesReturnRespVO respVO = BeanUtils.toBean(salesReturn, SalesReturnRespVO.class);

        // 加载明细行与卷清单
        List<SalesReturnItemDO> items = salesReturnItemMapper.selectList(new LambdaQueryWrapperX<SalesReturnItemDO>()
                .eq(SalesReturnItemDO::getReturnId, id));
        if (CollUtil.isNotEmpty(items)) {
            List<SalesReturnRollDO> rolls = salesReturnRollMapper.selectList(new LambdaQueryWrapperX<SalesReturnRollDO>()
                .eq(SalesReturnRollDO::getReturnId, id));
            Map<Long, List<SalesReturnRollDO>> rollsByItemId = CollUtil.isEmpty(rolls)
                    ? Collections.emptyMap()
                    : rolls.stream().collect(Collectors.groupingBy(SalesReturnRollDO::getReturnItemId));

            List<SalesReturnItemRespVO> itemRespList = new ArrayList<>();
            for (SalesReturnItemDO itemDO : items) {
                SalesReturnItemRespVO itemResp = BeanUtils.toBean(itemDO, SalesReturnItemRespVO.class);
                List<SalesReturnRollDO> itemRolls = rollsByItemId.getOrDefault(itemDO.getId(), Collections.emptyList());
                itemResp.setRolls(BeanUtils.toBean(itemRolls, SalesReturnRollRespVO.class));
                itemRespList.add(itemResp);
            }
            respVO.setItems(itemRespList);
        } else {
            respVO.setItems(Collections.emptyList());
        }

        return respVO;
    }

    @Override
    public PageResult<SalesReturnRespVO> getSalesReturnPage(SalesReturnPageReqVO reqVO) {
        LambdaQueryWrapperX<SalesReturnDO> wrapper = new LambdaQueryWrapperX<SalesReturnDO>()
                .eqIfPresent(SalesReturnDO::getCustomerId, reqVO.getCustomerId())
                .likeIfPresent(SalesReturnDO::getShipmentNo, reqVO.getShipmentNo())
                .eqIfPresent(SalesReturnDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SalesReturnDO::getApprovalStatus, reqVO.getApprovalStatus())
                .betweenIfPresent(SalesReturnDO::getReturnDate, reqVO.getReturnDate());

        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            wrapper.and(w -> w.like(SalesReturnDO::getReturnNo, reqVO.getKeyword())
                    .or().like(SalesReturnDO::getCustomerName, reqVO.getKeyword()));
        }

        wrapper.orderByDesc(SalesReturnDO::getCreateTime).orderByDesc(SalesReturnDO::getId);

        PageResult<SalesReturnDO> pageResult = salesReturnMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, SalesReturnRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SALES_RETURN", subType = "SUBMIT", bizNo = "{{#id}}",
            success = "提交销售退货单【{{#id}}】审批")
    public void submitSalesReturn(Long id) {
        // 1. 校验单据存在且为草稿待提交/已驳回状态
        SalesReturnDO existing = validateSalesReturnExists(id);
        if (!SalesReturnStatusEnum.DRAFT.getValue().equals(existing.getStatus())
                || (!"PENDING".equalsIgnoreCase(existing.getApprovalStatus())
                && !"REJECTED".equalsIgnoreCase(existing.getApprovalStatus()))) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }

        // 2. 加载明细行与卷清单，进行完整性守卫
        List<SalesReturnItemDO> items = salesReturnItemMapper.selectList(new LambdaQueryWrapperX<SalesReturnItemDO>()
                .eq(SalesReturnItemDO::getReturnId, id));
        if (CollUtil.isEmpty(items)) {
            throw invalidParamException("退货明细行不能为空");
        }

        List<SalesReturnRollDO> rolls = salesReturnRollMapper.selectList(new LambdaQueryWrapperX<SalesReturnRollDO>()
                .eq(SalesReturnRollDO::getReturnId, id));
        Map<Long, List<SalesReturnRollDO>> rollsByItemId = CollUtil.isEmpty(rolls) ? Collections.emptyMap()
                : rolls.stream().collect(Collectors.groupingBy(SalesReturnRollDO::getReturnItemId));

        for (SalesReturnItemDO item : items) {
            // 行级原因必填且有效 (015)
            if (StrUtil.isBlank(item.getReturnReason()) || !SalesReturnReasonEnum.isValid(item.getReturnReason())) {
                throw exception(SALES_RETURN_REASON_EMPTY);
            }
            List<SalesReturnRollDO> itemRolls = rollsByItemId.get(item.getId());
            if (CollUtil.isEmpty(itemRolls)) {
                throw exception(SALES_RETURN_ROLL_INVALID);
            }
            for (SalesReturnRollDO roll : itemRolls) {
                if (StrUtil.isBlank(roll.getRollNo())) {
                    throw exception(SALES_RETURN_ROLL_INVALID);
                }
                boolean hasWeight = roll.getWeight() != null && roll.getWeight().compareTo(BigDecimal.ZERO) > 0;
                boolean hasLength = roll.getLengthM() != null && roll.getLengthM().compareTo(BigDecimal.ZERO) > 0;
                if (!hasWeight && !hasLength) {
                    throw exception(SALES_RETURN_ROLL_INVALID);
                }
            }
        }

        // 3. 加载发货单与卷血缘校验
        ShipmentDO shipment = validateCompletedShipment(existing.getShipmentId());
        List<ShipmentRollDO> shipmentRolls = shipmentRollMapper.selectList(new LambdaQueryWrapperX<ShipmentRollDO>()
                .eq(ShipmentRollDO::getShipmentId, shipment.getId()));
        Map<Long, ShipmentRollDO> shipmentRollMap = CollUtil.isEmpty(shipmentRolls) ? Collections.emptyMap()
                : shipmentRolls.stream().collect(Collectors.toMap(ShipmentRollDO::getId, Function.identity()));

        for (SalesReturnItemDO item : items) {
            List<SalesReturnRollDO> itemRolls = rollsByItemId.get(item.getId());
            for (SalesReturnRollDO roll : itemRolls) {
                if (roll.getShipmentRollId() != null) {
                    ShipmentRollDO sRoll = shipmentRollMap.get(roll.getShipmentRollId());
                    if (sRoll == null || !item.getShipmentItemId().equals(sRoll.getShipmentItemId())) {
                        throw exception(SALES_RETURN_ROLL_INVALID);
                    }
                }
            }
        }

        // 4. 行级悲观锁：按 shipmentItemId 升序对父发货明细行执行 FOR UPDATE 锁定，防死锁并串行化并发退货
        List<Long> shipmentItemIds = items.stream()
                .map(SalesReturnItemDO::getShipmentItemId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<Long, ShipmentItemDO> lockedShipmentItemMap = new HashMap<>(shipmentItemIds.size());
        for (Long sItemId : shipmentItemIds) {
            ShipmentItemDO lockedItem = shipmentItemMapper.selectOneForUpdate(ShipmentItemDO::getId, sItemId);
            if (lockedItem == null) {
                lockedItem = shipmentItemMapper.selectById(sItemId);
            }
            if (lockedItem == null || !shipment.getId().equals(lockedItem.getShipmentId())) {
                throw exception(SALES_RETURN_ITEM_LINEAGE_INVALID);
            }
            lockedShipmentItemMap.put(sItemId, lockedItem);
        }

        // 5. 锁内原子重算双维度防超退（005/006）
        Map<Long, BigDecimal> occupiedMap = availabilityService.getOccupiedQtyMap(shipmentItemIds, id);
        for (SalesReturnItemDO item : items) {
            ShipmentItemDO sItem = lockedShipmentItemMap.get(item.getShipmentItemId());
            BigDecimal occupied = occupiedMap.getOrDefault(sItem.getId(), BigDecimal.ZERO);
            BigDecimal originalQty = sItem.getQuantity() != null ? sItem.getQuantity() : BigDecimal.ZERO;
            BigDecimal availableQty = originalQty.subtract(occupied).max(BigDecimal.ZERO);

            if (item.getReturnQty().compareTo(availableQty) > 0) {
                throw exception(SALES_RETURN_QTY_EXCEED_AVAILABLE, item.getReturnQty(), availableQty);
            }

            BigDecimal origUnitPrice = sItem.getUnitPrice() != null ? sItem.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal maxAllowedAmount = availableQty.multiply(origUnitPrice).setScale(2, RoundingMode.HALF_UP);
            if (item.getAmount().compareTo(maxAllowedAmount) > 0) {
                throw exception(SALES_RETURN_AMOUNT_EXCEED_AVAILABLE);
            }
        }

        // 6. 条件更新 approval_status=PENDING/REJECTED → APPROVING
        int rows = salesReturnMapper.update(null, new LambdaUpdateWrapper<SalesReturnDO>()
                .eq(SalesReturnDO::getId, id)
                .eq(SalesReturnDO::getStatus, SalesReturnStatusEnum.DRAFT.getValue())
                .in(SalesReturnDO::getApprovalStatus, "PENDING", "REJECTED")
                .set(SalesReturnDO::getApprovalStatus, "APPROVING"));
        if (rows == 0) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }

        // 7. 调用 02 号审批引擎发起审批实例
        ApprovalInstanceSubmitReqVO submitReqVO = new ApprovalInstanceSubmitReqVO();
        submitReqVO.setBizType(ApprovalHandler.BIZ_TYPE_SALES_RETURN);
        submitReqVO.setBizId(id);
        submitReqVO.setComment("提交销售退货审批");
        approvalInstanceService.submitInstance(submitReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SALES_RETURN", subType = "WITHDRAW", bizNo = "{{#id}}",
            success = "撤回销售退货单【{{#id}}】审批")
    public void withdrawSalesReturn(Long id) {
        // 1. 校验单据存在
        SalesReturnDO existing = validateSalesReturnExists(id);

        // 2. 状态机守卫：仅 status=DRAFT 且 approval_status=APPROVING 可撤回
        if (!SalesReturnStatusEnum.DRAFT.getValue().equals(existing.getStatus())
                || !"APPROVING".equalsIgnoreCase(existing.getApprovalStatus())) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }

        // 3. 查询审批引擎最新审批实例并调用撤回
        ApprovalInstanceRespVO instance = approvalInstanceService.getLatestInstanceByBiz(
                ApprovalHandler.BIZ_TYPE_SALES_RETURN, id);
        if (instance != null && "APPROVING".equalsIgnoreCase(instance.getStatus())) {
            ApprovalInstanceWithdrawReqVO withdrawReqVO = new ApprovalInstanceWithdrawReqVO();
            withdrawReqVO.setReason("发起人撤回销售退货审批");
            approvalInstanceService.withdrawInstance(instance.getId(), withdrawReqVO);
            // approvalInstanceService.withdrawInstance 内部已通过 SalesReturnApprovalBizHandler.updateApprovalStatus
            // 将 approval_status 回调重置为 PENDING 并释放占量
        } else {
            // 4. 兜底更新：若未匹配到审批中实例，直接原子更新 approval_status = 'PENDING'
            salesReturnMapper.update(null, new LambdaUpdateWrapper<SalesReturnDO>()
                    .eq(SalesReturnDO::getId, id)
                    .eq(SalesReturnDO::getStatus, SalesReturnStatusEnum.DRAFT.getValue())
                    .set(SalesReturnDO::getApprovalStatus, "PENDING"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "SALES_RETURN", subType = "CANCEL", bizNo = "{{#id}}",
            success = "作废销售退货单【{{#id}}】")
    public void cancelSalesReturn(Long id, String cancelReason) {
        // 0. 校验作废原因非空
        if (StrUtil.isBlank(cancelReason)) {
            throw exception(SALES_RETURN_REASON_EMPTY);
        }

        // 1. 校验单据存在
        SalesReturnDO existing = validateSalesReturnExists(id);

        // 2. 状态机守卫：
        //    CANCELLED 终态不可再动
        //    RECEIVED / POSTED 终态不可作废
        //    APPROVING 必须先撤回
        //    仅 status=DRAFT 且 approvalStatus IN (PENDING, REJECTED) 可直接作废
        if (SalesReturnStatusEnum.CANCELLED.getValue().equals(existing.getStatus())
                || SalesReturnStatusEnum.RECEIVED.getValue().equals(existing.getStatus())
                || SalesReturnStatusEnum.POSTED.getValue().equals(existing.getStatus())) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }
        if ("APPROVING".equalsIgnoreCase(existing.getApprovalStatus())) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }
        if (!SalesReturnStatusEnum.DRAFT.getValue().equals(existing.getStatus())
                || (!"PENDING".equalsIgnoreCase(existing.getApprovalStatus()) && !"REJECTED".equalsIgnoreCase(existing.getApprovalStatus()))) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }

        // 3. 条件更新置 CANCELLED 并联动置 approval_status 为 CANCELLED，落 cancelled_* 留痕（DRAFT 分支无入库单无需库存动作）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        int rows = salesReturnMapper.update(null, new LambdaUpdateWrapper<SalesReturnDO>()
                .eq(SalesReturnDO::getId, id)
                .eq(SalesReturnDO::getStatus, SalesReturnStatusEnum.DRAFT.getValue())
                .in(SalesReturnDO::getApprovalStatus, "PENDING", "REJECTED")
                .set(SalesReturnDO::getStatus, SalesReturnStatusEnum.CANCELLED.getValue())
                .set(SalesReturnDO::getApprovalStatus, "CANCELLED")
                .set(SalesReturnDO::getCancelledBy, loginUserId)
                .set(SalesReturnDO::getCancelledAt, LocalDateTime.now())
                .set(SalesReturnDO::getCancelReason, cancelReason));
        if (rows == 0) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }
    }

    // ==================== 私有辅助方法 ====================

    private ShipmentDO validateCompletedShipment(Long shipmentId) {
        if (shipmentId == null) {
            throw exception(SALES_RETURN_SHIPMENT_NOT_COMPLETED);
        }
        ShipmentDO shipment = shipmentMapper.selectById(shipmentId);
        if (shipment == null || !"COMPLETED".equalsIgnoreCase(shipment.getStatus())) {
            throw exception(SALES_RETURN_SHIPMENT_NOT_COMPLETED);
        }
        return shipment;
    }

    private SalesReturnDO validateSalesReturnExists(Long id) {
        if (id == null) {
            throw exception(SALES_RETURN_NOT_EXISTS);
        }
        SalesReturnDO salesReturn = salesReturnMapper.selectById(id);
        if (salesReturn == null) {
            throw exception(SALES_RETURN_NOT_EXISTS);
        }
        return salesReturn;
    }

    private void validateReturnNoUnique(Long id, String returnNo) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        SalesReturnDO existing = salesReturnMapper.selectByReturnNoIncludingDeleted(tenantId, returnNo);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(SALES_RETURN_NO_DUPLICATE, returnNo);
        }
    }

    private void validateItemsAndRolls(ShipmentDO shipment, List<SalesReturnItemSaveReqVO> itemReqs, Long excludeReturnId) {
        if (CollUtil.isEmpty(itemReqs)) {
            throw invalidParamException("退货明细行不能为空");
        }

        // 查询发货单存量明细与卷
        List<ShipmentItemDO> dbShipmentItems = shipmentItemMapper.selectList(new LambdaQueryWrapperX<ShipmentItemDO>()
                .eq(ShipmentItemDO::getShipmentId, shipment.getId()));
        Map<Long, ShipmentItemDO> shipmentItemMap = dbShipmentItems.stream()
                .collect(Collectors.toMap(ShipmentItemDO::getId, Function.identity()));

        List<ShipmentRollDO> dbShipmentRolls = shipmentRollMapper.selectList(new LambdaQueryWrapperX<ShipmentRollDO>()
                .eq(ShipmentRollDO::getShipmentId, shipment.getId()));
        Map<Long, ShipmentRollDO> shipmentRollMap = CollUtil.isEmpty(dbShipmentRolls) ? Collections.emptyMap()
                : dbShipmentRolls.stream().collect(Collectors.toMap(ShipmentRollDO::getId, Function.identity()));

        // 批量查询各发货行实时已占量（防 N+1 批量快照模式，coding-standards.md §2）
        Map<Long, BigDecimal> occupiedMap = availabilityService.getOccupiedQtyMap(shipmentItemMap.keySet(), excludeReturnId);

        Set<Long> seenShipmentItemIds = new HashSet<>();
        for (SalesReturnItemSaveReqVO itemReq : itemReqs) {
            // 1. 发货行血缘强制属于当前发货单 (004)
            ShipmentItemDO sItem = shipmentItemMap.get(itemReq.getShipmentItemId());
            if (sItem == null) {
                throw exception(SALES_RETURN_ITEM_LINEAGE_INVALID);
            }
            if (!seenShipmentItemIds.add(itemReq.getShipmentItemId())) {
                throw invalidParamException("同一发货明细行不能重复添加");
            }

            // 2. 行原因必填与合法性 (015, SalesReturnReasonEnum)
            if (StrUtil.isBlank(itemReq.getReturnReason()) || !SalesReturnReasonEnum.isValid(itemReq.getReturnReason())) {
                throw exception(SALES_RETURN_REASON_EMPTY);
            }

            // 3. 卷清单守卫：每行至少一卷，卷号必填、重量/米数至少一项、卷血缘必须属于该发货明细行 (007)
            if (CollUtil.isEmpty(itemReq.getRolls())) {
                throw exception(SALES_RETURN_ROLL_INVALID);
            }

            for (SalesReturnRollSaveReqVO rollReq : itemReq.getRolls()) {
                if (StrUtil.isBlank(rollReq.getRollNo())) {
                    throw exception(SALES_RETURN_ROLL_INVALID);
                }
                boolean hasWeight = rollReq.getWeight() != null && rollReq.getWeight().compareTo(BigDecimal.ZERO) > 0;
                boolean hasLength = rollReq.getLengthM() != null && rollReq.getLengthM().compareTo(BigDecimal.ZERO) > 0;
                if (!hasWeight && !hasLength) {
                    throw exception(SALES_RETURN_ROLL_INVALID);
                }

                if (rollReq.getShipmentRollId() != null) {
                    ShipmentRollDO sRoll = shipmentRollMap.get(rollReq.getShipmentRollId());
                    if (sRoll == null || !sItem.getId().equals(sRoll.getShipmentItemId())) {
                        throw exception(SALES_RETURN_ROLL_INVALID);
                    }
                }
            }

            // 4. 防超退聚合校验（数量超 005、金额超 006）- 纯内存高吞吐判定
            BigDecimal occupied = occupiedMap.getOrDefault(sItem.getId(), BigDecimal.ZERO);
            BigDecimal availableQty = sItem.getQuantity().subtract(occupied).max(BigDecimal.ZERO);
            if (itemReq.getReturnQty().compareTo(availableQty) > 0) {
                throw exception(SALES_RETURN_QTY_EXCEED_AVAILABLE, itemReq.getReturnQty(), availableQty);
            }

            BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : sItem.getUnitPrice();
            BigDecimal returnAmount = itemReq.getReturnQty().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal maxAmount = availableQty.multiply(sItem.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            if (returnAmount.compareTo(maxAmount) > 0) {
                throw exception(SALES_RETURN_AMOUNT_EXCEED_AVAILABLE);
            }
        }
    }

    private List<SalesReturnItemDO> buildItemDOList(ShipmentDO shipment, List<SalesReturnItemSaveReqVO> itemReqs, Long returnId) {
        List<ShipmentItemDO> dbShipmentItems = shipmentItemMapper.selectList(new LambdaQueryWrapperX<ShipmentItemDO>()
                .eq(ShipmentItemDO::getShipmentId, shipment.getId()));
        Map<Long, ShipmentItemDO> shipmentItemMap = dbShipmentItems.stream()
                .collect(Collectors.toMap(ShipmentItemDO::getId, Function.identity()));

        List<SalesReturnItemDO> result = new ArrayList<>();
        for (SalesReturnItemSaveReqVO itemReq : itemReqs) {
            ShipmentItemDO sItem = shipmentItemMap.get(itemReq.getShipmentItemId());
            SalesReturnItemDO itemDO = new SalesReturnItemDO();
            itemDO.setId(itemReq.getId());
            itemDO.setReturnId(returnId);
            itemDO.setShipmentItemId(sItem.getId());
            itemDO.setProductId(sItem.getProductId());
            itemDO.setProductCode(sItem.getProductCode());
            itemDO.setProductName(sItem.getProductName());
            itemDO.setCustomerColorNo(sItem.getCustomerColorNo());
            itemDO.setColorName(sItem.getColorName());
            itemDO.setVatNo(sItem.getVatNo());
            itemDO.setUnit(sItem.getUnit());

            itemDO.setReturnQty(itemReq.getReturnQty());
            itemDO.setPieces(itemReq.getPieces() != null ? itemReq.getPieces() : 1);

            BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : sItem.getUnitPrice();
            itemDO.setUnitPrice(unitPrice);
            itemDO.setAmount(itemDO.getReturnQty().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));

            itemDO.setReturnReason(itemReq.getReturnReason());
            itemDO.setRemark(itemReq.getRemark());

            result.add(itemDO);
        }
        return result;
    }

    private BigDecimal calculateTotalAmount(List<SalesReturnItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(SalesReturnItemDO::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculateTotalPieces(List<SalesReturnItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return 0;
        }
        return items.stream()
                .map(SalesReturnItemDO::getPieces)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private void saveItemsAndRolls(Long returnId, List<SalesReturnItemSaveReqVO> itemReqs, List<SalesReturnItemDO> itemDOList) {
        for (int i = 0; i < itemReqs.size(); i++) {
            SalesReturnItemSaveReqVO itemReq = itemReqs.get(i);
            SalesReturnItemDO itemDO = itemDOList.get(i);
            itemDO.setReturnId(returnId);
            salesReturnItemMapper.insert(itemDO);

            if (CollUtil.isNotEmpty(itemReq.getRolls())) {
                for (SalesReturnRollSaveReqVO rollReq : itemReq.getRolls()) {
                    SalesReturnRollDO rollDO = new SalesReturnRollDO();
                    rollDO.setReturnId(returnId);
                    rollDO.setReturnItemId(itemDO.getId());
                    rollDO.setShipmentRollId(rollReq.getShipmentRollId());
                    rollDO.setRollNo(rollReq.getRollNo());
                    rollDO.setWeight(rollReq.getWeight());
                    rollDO.setLengthM(rollReq.getLengthM());
                    rollDO.setRemark(rollReq.getRemark());
                    salesReturnRollMapper.insert(rollDO);
                }
            }
        }
    }

    private void diffSaveItemsAndRolls(Long returnId, ShipmentDO shipment, List<SalesReturnItemSaveReqVO> itemReqs, SalesReturnDO updateObj) {
        // 1. 查询存量明细与存量卷
        List<SalesReturnItemDO> dbItems = salesReturnItemMapper.selectList(new LambdaQueryWrapperX<SalesReturnItemDO>()
                .eq(SalesReturnItemDO::getReturnId, returnId));
        Map<Long, SalesReturnItemDO> dbItemMap = dbItems.stream()
                .collect(Collectors.toMap(SalesReturnItemDO::getId, Function.identity()));
        Set<Long> remainingDbItemIds = new HashSet<>(dbItemMap.keySet());

        List<SalesReturnRollDO> dbRolls = salesReturnRollMapper.selectList(new LambdaQueryWrapperX<SalesReturnRollDO>()
                .eq(SalesReturnRollDO::getReturnId, returnId));
        Map<Long, SalesReturnRollDO> dbRollMap = dbRolls.stream()
                .collect(Collectors.toMap(SalesReturnRollDO::getId, Function.identity()));
        Set<Long> remainingDbRollIds = new HashSet<>(dbRollMap.keySet());

        // 2. 校验明细行 ID 归属
        for (SalesReturnItemSaveReqVO itemReq : itemReqs) {
            if (itemReq.getId() != null) {
                if (!remainingDbItemIds.remove(itemReq.getId())) {
                    throw invalidParamException("退货明细行 ID【{}】不属于当前退货单", itemReq.getId());
                }
            }
        }
        Set<Long> deletedItemIds = remainingDbItemIds;

        // 3. 校验卷 ID 归属
        for (SalesReturnItemSaveReqVO itemReq : itemReqs) {
            if (CollUtil.isNotEmpty(itemReq.getRolls())) {
                for (SalesReturnRollSaveReqVO rollReq : itemReq.getRolls()) {
                    if (rollReq.getId() != null) {
                        if (!remainingDbRollIds.remove(rollReq.getId())) {
                            throw invalidParamException("退货卷 ID【{}】不属于当前退货单", rollReq.getId());
                        }
                    }
                }
            }
        }
        Set<Long> deletedRollIds = remainingDbRollIds;

        // 4. 构建明细 DO 列表并三分流
        List<SalesReturnItemDO> itemDOList = buildItemDOList(shipment, itemReqs, returnId);
        List<SalesReturnItemDO> toUpdateItems = new ArrayList<>();
        List<SalesReturnItemDO> toInsertItems = new ArrayList<>();

        for (SalesReturnItemDO itemDO : itemDOList) {
            if (itemDO.getId() != null) {
                toUpdateItems.add(itemDO);
            } else {
                toInsertItems.add(itemDO);
            }
        }

        // 5. 权威重算并更新主表
        updateObj.setTotalAmount(calculateTotalAmount(itemDOList));
        updateObj.setTotalPieces(calculateTotalPieces(itemDOList));
        salesReturnMapper.updateById(updateObj);

        // 6. 执行父行明细差异保存
        if (CollUtil.isNotEmpty(deletedItemIds)) {
            salesReturnItemMapper.deleteByIds(deletedItemIds);
        }
        for (SalesReturnItemDO itemDO : toUpdateItems) {
            salesReturnItemMapper.updateById(itemDO);
        }
        for (SalesReturnItemDO itemDO : toInsertItems) {
            salesReturnItemMapper.insert(itemDO);
        }

        // 7. 执行子行卷差异保存
        if (CollUtil.isNotEmpty(deletedRollIds)) {
            salesReturnRollMapper.deleteByIds(deletedRollIds);
        }
        for (int i = 0; i < itemReqs.size(); i++) {
            SalesReturnItemSaveReqVO itemReq = itemReqs.get(i);
            SalesReturnItemDO itemDO = itemDOList.get(i);

            if (CollUtil.isNotEmpty(itemReq.getRolls())) {
                for (SalesReturnRollSaveReqVO rollReq : itemReq.getRolls()) {
                    SalesReturnRollDO rollDO = new SalesReturnRollDO();
                    rollDO.setReturnId(returnId);
                    rollDO.setReturnItemId(itemDO.getId());
                    rollDO.setShipmentRollId(rollReq.getShipmentRollId());
                    rollDO.setRollNo(rollReq.getRollNo());
                    rollDO.setWeight(rollReq.getWeight());
                    rollDO.setLengthM(rollReq.getLengthM());
                    rollDO.setRemark(rollReq.getRemark());

                    if (rollReq.getId() != null) {
                        rollDO.setId(rollReq.getId());
                        salesReturnRollMapper.updateById(rollDO);
                    } else {
                        salesReturnRollMapper.insert(rollDO);
                    }
                }
            }
        }
    }

    @DataPermission(enable = false)
    private String generateReturnNoWithRetry(LocalDate returnDate) {
        return DataPermissionUtils.executeIgnore(() -> {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            LocalDate date = returnDate != null ? returnDate : LocalDate.now();
            String prefix = RETURN_NO_PREFIX + date.format(DATE_FORMATTER) + "-";

            List<SalesReturnDO> candidates = salesReturnMapper.selectListByReturnNoPrefixIncludingDeleted(tenantId, prefix);

            long maxNum = 0L;
            int maxDigits = 3;
            for (SalesReturnDO candidate : candidates) {
                String no = candidate.getReturnNo();
                if (no == null || no.length() <= prefix.length()) {
                    continue;
                }
                String suffix = no.substring(prefix.length());
                Matcher matcher = NO_SUFFIX_PATTERN.matcher(suffix);
                if (matcher.find()) {
                    try {
                        long num = Long.parseLong(matcher.group(1));
                        if (num > maxNum) {
                            maxNum = num;
                            maxDigits = Math.max(3, matcher.group(1).length());
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            for (int i = 1; i <= MAX_GENERATE_RETRY; i++) {
                long candidateNum = maxNum + i;
                String candidateNo = prefix + String.format("%0" + maxDigits + "d", candidateNum);
                if (salesReturnMapper.selectByReturnNoIncludingDeleted(tenantId, candidateNo) == null) {
                    return candidateNo;
                }
            }
            throw exception(SALES_RETURN_NO_DUPLICATE, prefix + String.format("%0" + maxDigits + "d", maxNum + 1));
        });
    }

}
