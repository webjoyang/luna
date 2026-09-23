package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;

/**
 * 生产管理-检验单 Service 接口
 *
 * @author luna
 */
public interface InspectionOrderService {

    /**
     * 获得检验单分页
     *
     * @param pageReqVO 分页查询
     * @return 检验单分页
     */
    PageResult<InspectionOrderRespVO> getInspectionOrderPage(InspectionOrderPageReqVO pageReqVO);

    /**
     * 获得检验单详情
     *
     * @param id 检验单编号
     * @return 检验单详情
     */
    InspectionOrderRespVO getInspectionOrder(Long id);

    /**
     * 预览下一个检验单号（只读、不占号）
     *
     * @return 检验单号
     */
    String getNextInspectionOrderNo();

    /**
     * 创建检验单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInspectionOrder(com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO createReqVO);

    /**
     * 更新检验单
     *
     * @param updateReqVO 更新信息
     */
    void updateInspectionOrder(com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO updateReqVO);

    /**
     * 更新检验单状态
     *
     * @param id 检验单编号
     * @param status 状态
     */
    void updateInspectionOrderStatus(Long id, String status);

    /**
     * 完成检验单（DRAFT/ACTIVE -> COMPLETED）
     *
     * @param id 检验单编号
     */
    void completeInspectionOrder(Long id);

    /**
     * 取消检验单（任意态 -> CANCELLED）
     *
     * @param id 检验单编号
     */
    void cancelInspectionOrder(Long id);

    /**
     * 删除检验单（仅 DRAFT 可删，级联逻辑删除明细）
     *
     * @param id 检验单编号
     */
    void deleteInspectionOrder(Long id);

}

