package com.luna.lunasrv.module.lunaerp.portal.service;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageRespVO;

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
     * @return 发货单分页结果（最小契约：详情不入列表）
     */
    PageResult<PortalShipmentPageRespVO> getPortalShipmentPage(PortalShipmentPageReqVO reqVO);

    /**
     * 获得客户门户发货单详情（按 id 或 shipmentNo 查询，归属校验 customerId 不符抛 038-001）
     *
     * @param id 发货单ID
     * @param shipmentNo 发货单号
     * @return 发货单详情（头+逐行明细）
     */
    PortalShipmentDetailRespVO getPortalShipment(Long id, String shipmentNo);

}
