package com.luna.lunasrv.module.lunaerp.service.h5.workshop;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTasksPageReqVO;

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

}
