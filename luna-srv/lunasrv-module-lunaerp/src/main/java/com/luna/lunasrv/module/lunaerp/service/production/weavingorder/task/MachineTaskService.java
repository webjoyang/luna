package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.task;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskHaltReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskSaveRespVO;
import jakarta.validation.Valid;

/**
 * 生产管理 - 机台任务 Service 接口
 *
 * @author luna
 */
public interface MachineTaskService {

    /**
     * 创建机台任务
     *
     * @param createReqVO 创建信息
     * @return 包含任务ID、派生编号与超量软警告的响应对象
     */
    MachineTaskSaveRespVO createMachineTask(@Valid MachineTaskSaveReqVO createReqVO);

    /**
     * 修改机台任务（排期/挡车工/匹数/织机）
     *
     * @param updateReqVO 修改信息
     * @return 包含任务ID、编号与超量软警告的响应对象
     */
    MachineTaskSaveRespVO updateMachineTask(@Valid MachineTaskSaveReqVO updateReqVO);

    /**
     * 删除机台任务（仅待开始 PENDING 态允许）
     *
     * @param id 任务ID
     */
    void deleteMachineTask(Long id);

    /**
     * 机台任务停机（已完工禁停，停机原因必填，记录 haltAt）
     *
     * @param haltReqVO 停机信息
     */
    void haltMachineTask(@Valid MachineTaskHaltReqVO haltReqVO);

    /**
     * 机台任务恢复（解除停机态，恢复至 PENDING）
     *
     * @param id 任务ID
     */
    void resumeMachineTask(Long id);

    /**
     * 获得机台任务详情
     *
     * @param id 任务ID
     * @return 任务详情
     */
    MachineTaskRespVO getMachineTask(Long id);

    /**
     * 获得机台任务分页
     *
     * @param pageReqVO 分页查询
     * @return 任务分页
     */
    PageResult<MachineTaskRespVO> getMachineTaskPage(MachineTaskPageReqVO pageReqVO);

    /**
     * 预览下一个机台任务编号（只读、不占号）
     *
     * @param weavingOrderId 织造单ID
     * @return 任务编号
     */
    String getNextMachineTaskCode(Long weavingOrderId);

    /**
     * 按织造单ID级联逻辑删除所有机台任务（供织造单删除监听器调用）
     *
     * @param weavingOrderId 织造单ID
     */
    void deleteByWeavingOrderId(Long weavingOrderId);

}
