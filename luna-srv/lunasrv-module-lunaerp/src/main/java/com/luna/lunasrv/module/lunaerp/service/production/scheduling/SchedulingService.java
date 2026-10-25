package com.luna.lunasrv.module.lunaerp.service.production.scheduling;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.PendingPoolRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskRespVO;

/**
 * 生产管理 - 排产中心只读查询与聚合 Service 接口
 *
 * @author luna
 */
public interface SchedulingService {

    /**
     * 查询甘特图数据（织机行 + 任务条带 + 冲突红标）
     *
     * @param reqVO 甘特查询入参
     * @return 甘特数据
     */
    GanttRespVO getGantt(GanttReqVO reqVO);

    /**
     * 查询待排池数据（未排期任务 + 未排满织造单）
     *
     * @return 待排池数据
     */
    PendingPoolRespVO getPendingPool();

    /**
     * 获得机台任务分页（支持跨单、织机过滤、冲突红标过滤，补齐织造单与产品快照）
     *
     * @param reqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<MachineTaskRespVO> getMachineTaskPage(MachineTaskPageReqVO reqVO);

}
