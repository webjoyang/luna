package com.luna.lunasrv.module.lunaerp.portal.service;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentUpdateSignReqVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentSignReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.shipment.ShipmentSignStatusEnum;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentService;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SHIPMENT_SIGN_STATUS_INVALID;
import static com.luna.lunasrv.module.lunaerp.enums.PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED;

/**
 * 客户门户发货单只读服务实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class PortalShipmentServiceImpl implements PortalShipmentService {

    public static final String COMPLETED_STATUS = "COMPLETED";

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private ShipmentItemMapper shipmentItemMapper;

    @Resource
    private ShipmentService shipmentService;

    @Override
    public PageResult<PortalShipmentPageRespVO> getPortalShipmentPage(PortalShipmentPageReqVO reqVO) {
        Long customerId = PortalContext.getCustomerId();
        if (customerId == null) {
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        String signStatus = StrUtil.isNotBlank(reqVO.getSignStatus()) ? reqVO.getSignStatus() : reqVO.getStatus();

        LambdaQueryWrapperX<ShipmentDO> wrapper = new LambdaQueryWrapperX<ShipmentDO>()
                .eq(ShipmentDO::getCustomerId, customerId)
                .eq(ShipmentDO::getStatus, COMPLETED_STATUS);

        if (StrUtil.isNotBlank(signStatus) && !"ALL".equalsIgnoreCase(signStatus)) {
            wrapper.eq(ShipmentDO::getSignStatus, signStatus.trim());
        }

        wrapper.orderByDesc(ShipmentDO::getShippingDate)
                .orderByDesc(ShipmentDO::getId);

        PageResult<ShipmentDO> pageResult = shipmentMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, PortalShipmentPageRespVO.class);
    }

    @Override
    public PortalShipmentDetailRespVO getPortalShipment(Long id, String shipmentNo) {
        Long customerId = PortalContext.getCustomerId();
        if (customerId == null) {
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        if (id == null && StrUtil.isBlank(shipmentNo)) {
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        ShipmentDO shipment;
        if (id != null) {
            shipment = shipmentMapper.selectById(id);
        } else {
            shipment = shipmentMapper.selectOne(new LambdaQueryWrapperX<ShipmentDO>()
                    .eq(ShipmentDO::getShipmentNo, shipmentNo.trim()));
        }

        if (shipment == null) {
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 归属校验：customerId 必须与 PortalContext 一致，且必须为 COMPLETED 状态
        if (!Objects.equals(shipment.getCustomerId(), customerId)
                || !COMPLETED_STATUS.equalsIgnoreCase(shipment.getStatus())) {
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 若同时传入了 id 和 shipmentNo，严格校验二者一致性
        if (id != null && StrUtil.isNotBlank(shipmentNo)
                && !StrUtil.equals(shipment.getShipmentNo(), shipmentNo.trim())) {
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 组装详情 VO 与明细行
        PortalShipmentDetailRespVO respVO = BeanUtils.toBean(shipment, PortalShipmentDetailRespVO.class);
        List<ShipmentItemDO> itemDOList = shipmentItemMapper.selectListByShipmentId(shipment.getId());
        List<PortalShipmentItemRespVO> itemVOList = BeanUtils.toBean(itemDOList, PortalShipmentItemRespVO.class);
        respVO.setItems(itemVOList != null ? itemVOList : Collections.emptyList());

        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "CUSTOMER_PORTAL", subType = "SIGN_SHIPMENT", bizNo = "{{#reqVO.id}}",
            success = "客户门户发货单【{{#reqVO.id}}】登记签收【{{#reqVO.signStatus}}】，签收人【{{#reqVO.signedByName}}】")
    public void signPortalShipment(PortalShipmentSignReqVO reqVO) {
        Long customerId = PortalContext.getCustomerId();
        if (customerId == null) {
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 1. 归属校验：校验发货单存在且属于当前登录客户
        ShipmentDO shipment = shipmentMapper.selectById(reqVO.getId());
        if (shipment == null || !Objects.equals(shipment.getCustomerId(), customerId)) {
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 2. 状态校验：门户端仅允许 SIGNED 或 REFUSED
        if (!ShipmentSignStatusEnum.SIGNED.getValue().equals(reqVO.getSignStatus())
                && !ShipmentSignStatusEnum.REFUSED.getValue().equals(reqVO.getSignStatus())) {
            throw exception(SHIPMENT_SIGN_STATUS_INVALID);
        }

        // 3. 委派既有发货单服务更新签收（复用状态守卫：仅 COMPLETED 可签、零新状态、零库存账务副作用）
        ShipmentUpdateSignReqVO updateReqVO = new ShipmentUpdateSignReqVO();
        updateReqVO.setId(reqVO.getId());
        updateReqVO.setSignStatus(reqVO.getSignStatus());
        updateReqVO.setSignedByName(reqVO.getSignedByName());
        updateReqVO.setSignedAt(LocalDateTime.now());
        updateReqVO.setSignRemark(reqVO.getSignRemark());

        shipmentService.updateShipmentSign(updateReqVO);
    }
}
