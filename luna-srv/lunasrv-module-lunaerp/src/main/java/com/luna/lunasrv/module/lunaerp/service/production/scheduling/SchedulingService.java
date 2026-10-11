package com.luna.lunasrv.module.lunaerp.service.production.scheduling;

import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttQueryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.PendingPoolRespVO;

/**
 * 生产管理 - 排产中心 Service 接口
 *
 * @author luna
 */
public interface SchedulingService {

    /**
     * 查询甘特图排产数据（织机行 + 任务条带）
     *
     * @param reqVO 查询条件
     * @return 甘特数据
     */
    GanttRespVO getGanttData(GanttQueryReqVO reqVO);

    /**
     * 查询待排池数据（无日期任务 + 未排满织造单）
     *
     * @return 待排池数据
     */
    PendingPoolRespVO getPendingPool();

}
