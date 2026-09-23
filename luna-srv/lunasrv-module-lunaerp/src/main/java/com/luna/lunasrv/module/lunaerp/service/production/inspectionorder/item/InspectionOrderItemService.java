package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.item;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemSaveReqVO;

/**
 * 生产管理-检验单录检明细 Service 接口
 *
 * 录检明细作为独立子资源处理（ADR-0014），有自己的 CRUD 端点与权限码，逐卷增删改。
 *
 * @author luna
 */
public interface InspectionOrderItemService {

    /**
     * 创建录检明细
     *
     * @param createReqVO 明细信息
     * @return 明细编号
     */
    Long createInspectionOrderItem(InspectionOrderItemSaveReqVO createReqVO);

    /**
     * 更新录检明细
     *
     * @param updateReqVO 明细信息
     */
    void updateInspectionOrderItem(InspectionOrderItemSaveReqVO updateReqVO);

    /**
     * 删除录检明细
     *
     * @param id 明细编号
     */
    void deleteInspectionOrderItem(Long id);

    /**
     * 获得录检明细详情
     *
     * @param id 明细编号
     * @return 录检明细
     */
    InspectionOrderItemRespVO getInspectionOrderItem(Long id);

    /**
     * 获得录检明细分页
     *
     * @param pageReqVO 分页查询
     * @return 录检明细分页
     */
    PageResult<InspectionOrderItemRespVO> getInspectionOrderItemPage(InspectionOrderItemPageReqVO pageReqVO);

}
