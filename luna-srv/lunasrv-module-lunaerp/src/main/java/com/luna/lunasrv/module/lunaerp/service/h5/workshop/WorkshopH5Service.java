package com.luna.lunasrv.module.lunaerp.service.h5.workshop;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTasksPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStartReqVO;

/**
 * 车间移动 H5 接入层编排服务
 *
 * @author luna
 */
public interface WorkshopH5Service {

    /**
     * 获取当前登录挡车工的机台任务分页列表
     *
     * @param reqVO 分页查询条件
     * @return 任务列表分页
     */
    PageResult<WorkshopMyTaskRespVO> getMyTasksPage(WorkshopMyTasksPageReqVO reqVO);

    /**
     * 按布票编码查询布票详情卡片（扫码/手输定位）
     *
     * @param code 布票编号
     * @return 布票卡精简出参
     */
    WorkshopTicketRespVO getTicketByCode(String code);

    /**
     * 布票开工（含未派工自助认领）
     *
     * @param reqVO 开工请求入参
     */
    void startTicket(WorkshopTicketStartReqVO reqVO);

}
