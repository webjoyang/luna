package com.luna.lunasrv.module.lunaerp.approval.task;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.ApprovalTaskApproveReqVO;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.ApprovalTaskRejectReqVO;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.ApprovalTaskTodoPageReqVO;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.ApprovalTaskTodoRespVO;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.ApprovalTaskTransferReqVO;
import jakarta.validation.Valid;

/**
 * 审批任务 Service 接口
 *
 * @author luna
 */
public interface ApprovalTaskService {

    /**
     * 获得待我审批任务分页
     *
     * @param pageReqVO 分页查询条件
     * @return 待办任务分页
     */
    PageResult<ApprovalTaskTodoRespVO> getTodoPage(@Valid ApprovalTaskTodoPageReqVO pageReqVO);

    /**
     * 审批通过
     *
     * @param id 任务ID
     * @param reqVO 通过参数（含审批意见）
     */
    void approveTask(Long id, ApprovalTaskApproveReqVO reqVO);

    /**
     * 审批驳回
     *
     * @param id 任务ID
     * @param reqVO 驳回参数（含必填驳回意见）
     */
    void rejectTask(Long id, @Valid ApprovalTaskRejectReqVO reqVO);

    /**
     * 审批转交
     *
     * @param id 任务ID
     * @param reqVO 转交参数（接收人与说明）
     */
    void transferTask(Long id, @Valid ApprovalTaskTransferReqVO reqVO);

}
