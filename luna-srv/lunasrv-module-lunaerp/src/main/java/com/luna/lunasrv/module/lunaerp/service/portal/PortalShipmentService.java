package com.luna.lunasrv.module.lunaerp.service.portal;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentPageItemRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentPageReqVO;

import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentSignReqVO;

/**
 * 客户门户 - 发货单服务接口
 *
 * @author luna
 */
public interface PortalShipmentService {

    /**
     * 分页查询当前客户已发货（COMPLETED）的发货单
     *
     * @param reqVO 分页查询条件
     * @return 发货单分页列表
     */
    PageResult<PortalShipmentPageItemRespVO> getShipmentPage(PortalShipmentPageReqVO reqVO);

    /**
     * 查询发货单详情（支持按 id 或 按 shipmentNo 查询，具备严格越权与租户守卫）
     *
     * @param id 发货单ID
     * @param no 发货单号
     * @return 发货单详情
     */
    PortalShipmentDetailRespVO getShipmentDetail(Long id, String no);

    /**
     * 登记发货单签收/拒收
     *
     * @param reqVO 签收登记请求
     */
    void signShipment(PortalShipmentSignReqVO reqVO);

}
