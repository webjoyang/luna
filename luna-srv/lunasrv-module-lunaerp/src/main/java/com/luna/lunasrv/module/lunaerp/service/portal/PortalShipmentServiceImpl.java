package com.luna.lunasrv.module.lunaerp.service.portal;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentUpdateSignReqVO;
import com.luna.lunasrv.module.lunaerp.enums.business.shipment.ShipmentSignStatusEnum;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.*;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SHIPMENT_SIGN_STATUS_INVALID;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SHIPMENT_STATUS_NOT_ALLOW;
import static com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED;

/**
 * 客户门户 - 发货单服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class PortalShipmentServiceImpl implements PortalShipmentService {

    private static final String STATUS_COMPLETED = "COMPLETED";

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private ShipmentItemMapper shipmentItemMapper;

    @Resource
    private ShipmentRollMapper shipmentRollMapper;

    @Resource
    private ShipmentService shipmentService;

    @Override
    public PageResult<PortalShipmentPageItemRespVO> getShipmentPage(PortalShipmentPageReqVO reqVO) {
        Long customerId = PortalContext.getCustomerId();
        if (customerId == null) {
            log.warn("[getShipmentPage][当前请求缺失客户门户上下文]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        LambdaQueryWrapperX<ShipmentDO> wrapper = new LambdaQueryWrapperX<ShipmentDO>()
                .eq(ShipmentDO::getCustomerId, customerId)
                .eq(ShipmentDO::getStatus, STATUS_COMPLETED);

        if (StringUtils.hasText(reqVO.getSignStatus())) {
            wrapper.eq(ShipmentDO::getSignStatus, reqVO.getSignStatus().trim());
        }

        wrapper.orderByDesc(ShipmentDO::getShippingDate)
                .orderByDesc(ShipmentDO::getId);

        PageResult<ShipmentDO> pageResult = shipmentMapper.selectPage(reqVO, wrapper);

        List<PortalShipmentPageItemRespVO> list = pageResult.getList().stream()
                .map(s -> PortalShipmentPageItemRespVO.builder()
                        .id(s.getId())
                        .shipmentNo(s.getShipmentNo())
                        .shippingDate(s.getShippingDate())
                        .logisticsCompany(s.getCarrier())
                        .logisticsTrackingNo(s.getTrackingNumber())
                        .signStatus(s.getSignStatus())
                        .totalAmount(s.getTotalAmount())
                        .freightFee(s.getFreightFee() != null ? s.getFreightFee() : BigDecimal.ZERO)
                        .totalPieces(s.getTotalPieces())
                        .build())
                .collect(Collectors.toList());

        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public PortalShipmentDetailRespVO getShipmentDetail(Long id, String no) {
        if (id == null && !StringUtils.hasText(no)) {
            log.warn("[getShipmentDetail][未提供 id 且未提供 no 参数]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        ShipmentDO shipment;
        if (id != null) {
            shipment = shipmentMapper.selectById(id);
            if (shipment != null && StringUtils.hasText(no) && !Objects.equals(shipment.getShipmentNo(), no.trim())) {
                log.warn("[getShipmentDetail][id 与 no 不一致: id={}, no={}, shipmentNo={}]", id, no, shipment.getShipmentNo());
                throw exception(PORTAL_DATA_ACCESS_DENIED);
            }
        } else {
            shipment = shipmentMapper.selectOne(new LambdaQueryWrapperX<ShipmentDO>()
                    .eq(ShipmentDO::getShipmentNo, no.trim()));
        }

        // 安全守卫 1: 单据存在性
        if (shipment == null) {
            log.warn("[getShipmentDetail][发货单不存在: id={}, no={}]", id, no);
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 安全守卫 2: 仅允许查看 COMPLETED 单据
        if (!STATUS_COMPLETED.equals(shipment.getStatus())) {
            log.warn("[getShipmentDetail][发货单状态非 COMPLETED: status={}, id={}, no={}]", shipment.getStatus(), id, no);
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 安全守卫 3: 横向越权防御（仅允许当前客户查看自己名下单据）
        Long currentCustomerId = PortalContext.getCustomerId();
        if (currentCustomerId == null || !currentCustomerId.equals(shipment.getCustomerId())) {
            log.warn("[getShipmentDetail][客户归属不匹配: currentCustomerId={}, shipmentCustomerId={}]", currentCustomerId, shipment.getCustomerId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 安全守卫 4: 跨租户防御
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(shipment.getTenantId())) {
            log.warn("[getShipmentDetail][租户归属不匹配: currentTenantId={}, shipmentTenantId={}]", currentTenantId, shipment.getTenantId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 获取发货明细与卷清单
        List<ShipmentItemDO> itemDOList = shipmentItemMapper.selectListByShipmentId(shipment.getId());
        List<ShipmentRollDO> rollDOList = shipmentRollMapper.selectListByShipmentId(shipment.getId());

        Map<Long, List<ShipmentRollDO>> rollsByItemId = (rollDOList == null || rollDOList.isEmpty())
                ? Collections.emptyMap()
                : rollDOList.stream().collect(Collectors.groupingBy(ShipmentRollDO::getShipmentItemId));

        List<PortalShipmentDetailItemRespVO> itemVOList = new ArrayList<>();
        if (itemDOList != null) {
            for (ShipmentItemDO itemDO : itemDOList) {
                List<ShipmentRollDO> itemRolls = rollsByItemId.getOrDefault(itemDO.getId(), Collections.emptyList());
                List<PortalShipmentRollRespVO> rollVOList = itemRolls.stream()
                        .map(r -> PortalShipmentRollRespVO.builder()
                                .id(r.getId())
                                .rollNo(r.getRollNo())
                                .weight(r.getWeight())
                                .lengthM(r.getLengthM())
                                .build())
                        .collect(Collectors.toList());

                itemVOList.add(PortalShipmentDetailItemRespVO.builder()
                        .id(itemDO.getId())
                        .productCode(itemDO.getProductCode())
                        .productName(itemDO.getProductName())
                        .customerColorNo(itemDO.getCustomerColorNo())
                        .colorName(itemDO.getColorName())
                        .vatNo(itemDO.getVatNo())
                        .quantity(itemDO.getQuantity())
                        .pieces(itemDO.getPieces())
                        .unit(itemDO.getUnit())
                        .unitPrice(itemDO.getUnitPrice())
                        .amount(itemDO.getAmount())
                        .rolls(rollVOList)
                        .build());
            }
        }

        return PortalShipmentDetailRespVO.builder()
                .id(shipment.getId())
                .shipmentNo(shipment.getShipmentNo())
                .shippingDate(shipment.getShippingDate())
                .logisticsCompany(shipment.getCarrier())
                .logisticsTrackingNo(shipment.getTrackingNumber())
                .signStatus(shipment.getSignStatus())
                .signedByName(shipment.getSignedByName())
                .signedAt(shipment.getSignedAt())
                .signRemark(shipment.getSignRemark())
                .totalAmount(shipment.getTotalAmount())
                .freightFee(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO)
                .totalPieces(shipment.getTotalPieces())
                .items(itemVOList)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "客户门户", subType = "发货签收", bizNo = "{{#reqVO.id}}",
            success = "门户客户登记发货单签收【{{#shipmentNo}}】：状态【{{#reqVO.signStatus}}】，签收人【{{#reqVO.signedByName}}】")
    public void signShipment(PortalShipmentSignReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            log.warn("[signShipment][请求参数或单据ID为空]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }
        if (!StringUtils.hasText(reqVO.getSignedByName())) {
            log.warn("[signShipment][签收人姓名为空: id={}]", reqVO.getId());
            throw exception(BAD_REQUEST, "签收人姓名不能为空");
        }

        // 1. 校验存在性及未删除
        ShipmentDO shipment = shipmentMapper.selectById(reqVO.getId());
        if (shipment == null) {
            log.warn("[signShipment][发货单不存在或已删除: id={}]", reqVO.getId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 留痕上下文变量记录
        LogRecordContext.putVariable("shipmentNo", shipment.getShipmentNo());

        // 2. 校验客户归属（横向越权防御）
        Long currentCustomerId = PortalContext.getCustomerId();
        if (currentCustomerId == null || !currentCustomerId.equals(shipment.getCustomerId())) {
            log.warn("[signShipment][客户归属不匹配: currentCustomerId={}, shipmentCustomerId={}]",
                    currentCustomerId, shipment.getCustomerId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 3. 校验租户归属（跨租户防御）
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(shipment.getTenantId())) {
            log.warn("[signShipment][租户归属不匹配: currentTenantId={}, shipmentTenantId={}]",
                    currentTenantId, shipment.getTenantId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 4. 状态守卫：仅已发货态 COMPLETED 允许签收
        if (!STATUS_COMPLETED.equals(shipment.getStatus())) {
            log.warn("[signShipment][发货单状态非 COMPLETED: status={}, id={}]", shipment.getStatus(), reqVO.getId());
            throw exception(SHIPMENT_STATUS_NOT_ALLOW);
        }

        // 5. 签收状态合法性校验：必须是 SIGNED 或 REFUSED
        if (!ShipmentSignStatusEnum.SIGNED.getValue().equals(reqVO.getSignStatus())
                && !ShipmentSignStatusEnum.REFUSED.getValue().equals(reqVO.getSignStatus())) {
            log.warn("[signShipment][签收状态非法: signStatus={}, id={}]", reqVO.getSignStatus(), reqVO.getId());
            throw exception(SHIPMENT_SIGN_STATUS_INVALID);
        }

        // 6. 复用既有签收守卫更新主表签收字段
        ShipmentUpdateSignReqVO signReqVO = new ShipmentUpdateSignReqVO();
        signReqVO.setId(shipment.getId());
        signReqVO.setSignStatus(reqVO.getSignStatus());
        signReqVO.setSignedByName(reqVO.getSignedByName().trim());
        signReqVO.setSignedAt(LocalDateTime.now());
        signReqVO.setSignRemark(reqVO.getSignRemark());
        shipmentService.updateShipmentSign(signReqVO);
    }

}
