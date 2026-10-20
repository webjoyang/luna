package com.luna.lunasrv.module.lunaerp.portal.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo.PortalShipmentItemRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo.PortalShipmentRespVO;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
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

    @Override
    public PageResult<PortalShipmentRespVO> getPortalShipmentPage(PortalShipmentPageReqVO reqVO) {
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
        PageResult<PortalShipmentRespVO> result = BeanUtils.toBean(pageResult, PortalShipmentRespVO.class);
        if (CollUtil.isNotEmpty(result.getList())) {
            for (PortalShipmentRespVO vo : result.getList()) {
                fillCompatibilityFields(vo);
            }
        }
        return result;
    }

    @Override
    public PortalShipmentRespVO getPortalShipment(Long id, String shipmentNo) {
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

        // 组装 VO 与明细行
        PortalShipmentRespVO respVO = BeanUtils.toBean(shipment, PortalShipmentRespVO.class);
        fillCompatibilityFields(respVO);

        List<ShipmentItemDO> itemDOList = shipmentItemMapper.selectListByShipmentId(shipment.getId());
        List<PortalShipmentItemRespVO> itemVOList = BeanUtils.toBean(itemDOList, PortalShipmentItemRespVO.class);
        respVO.setItems(itemVOList != null ? itemVOList : Collections.emptyList());

        return respVO;
    }

    private void fillCompatibilityFields(PortalShipmentRespVO vo) {
        if (vo == null) return;
        if (vo.getLogisticsCompany() == null) {
            vo.setLogisticsCompany(vo.getCarrier());
        }
        if (vo.getLogisticsNo() == null) {
            vo.setLogisticsNo(vo.getTrackingNumber());
        }
        if (vo.getFreightAmount() == null) {
            vo.setFreightAmount(vo.getFreightFee());
        }
        if (vo.getShipmentDate() == null && vo.getShippingDate() != null) {
            vo.setShipmentDate(vo.getShippingDate().toString());
        }
    }
}
