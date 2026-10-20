package com.luna.lunasrv.module.lunaerp.portal.service;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo.PortalShipmentRespVO;

/**
 * 客户门户发货单只读服务接口
 *
 * @author luna
 */
public interface PortalShipmentService {

    /**
     * 获得客户门户发货单分页（按 PortalContext customerId 过滤，仅 COMPLETED，时间倒序）
     *
     * @param reqVO 分页查询条件
     * @return 发货单分页结果
     */
    PageResult<PortalShipmentRespVO> getPortalShipmentPage(PortalShipmentPageReqVO reqVO);

    /**
     * 获得客户门户发货单详情（按 id 或 shipmentNo 查询，归属校验 customerId 不符抛 038-001）
     *
     * @param id 发货单ID
     * @param shipmentNo 发货单号
     * @return 发货单详情
     */
    PortalShipmentRespVO getPortalShipment(Long id, String shipmentNo);

}
