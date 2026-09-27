package com.luna.lunasrv.module.lunaerp.service.business.shipment;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentSaveReqVO;
import jakarta.validation.Valid;

import java.time.LocalDate;

/**
 * 业务管理 - 发货单 Service 接口
 *
 * @author luna
 */
public interface ShipmentService {

    /**
     * 创建发货单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createShipment(@Valid ShipmentSaveReqVO createReqVO);

    /**
     * 更新发货单（仅草稿态可编辑，明细与卷清单整体替换）
     *
     * @param updateReqVO 更新信息
     */
    void updateShipment(@Valid ShipmentSaveReqVO updateReqVO);

    /**
     * 删除发货单（仅草稿态可删，级联逻辑删除明细与卷清单）
     *
     * @param id 编号
     */
    void deleteShipment(Long id);

    /**
     * 获得发货单详情（包含发货明细与卷清单）
     *
     * @param id 编号
     * @return 发货单详情
     */
    ShipmentRespVO getShipment(Long id);

    /**
     * 获得发货单分页
     *
     * @param reqVO 分页查询
     * @return 发货单分页
     */
    PageResult<ShipmentRespVO> getShipmentPage(ShipmentPageReqVO reqVO);

    /**
     * 预览下一个发货单号（只读、不占号）
     *
     * @param shippingDate 发货日期（为空缺省当天）
     * @return 发货单号预览
     */
    String getNextShipmentNo(LocalDate shippingDate);

}
