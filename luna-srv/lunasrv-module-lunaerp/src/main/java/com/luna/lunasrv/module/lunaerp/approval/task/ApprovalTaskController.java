package com.luna.lunasrv.module.lunaerp.approval.task;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 审批中心 - 审批任务 Controller
 *
 * @author luna
 */
@Tag(name = "审批中心 - 审批任务")
@RestController
@RequestMapping("/lunaerp/approval/task")
@Validated
public class ApprovalTaskController {

    @Resource
    private ApprovalTaskService approvalTaskService;

    @GetMapping("/todo-page")
    @Operation(summary = "获得待我审批任务分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:task:query')")
    public CommonResult<PageResult<ApprovalTaskTodoRespVO>> getTodoPage(@Valid ApprovalTaskTodoPageReqVO pageReqVO) {
        return success(approvalTaskService.getTodoPage(pageReqVO));
    }

    @GetMapping("/done-page")
    @Operation(summary = "获得我已审批任务分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:approval:task:query')")
    public CommonResult<PageResult<ApprovalTaskDoneRespVO>> getDonePage(@Valid ApprovalTaskDonePageReqVO pageReqVO) {
        return success(approvalTaskService.getDonePage(pageReqVO));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "审批通过（仅审批人本人）")
    @Parameter(name = "id", description = "审批任务ID", required = true, example = "1024")
    public CommonResult<Boolean> approveTask(@PathVariable("id") Long id,
                                            @RequestBody(required = false) ApprovalTaskApproveReqVO reqVO) {
        approvalTaskService.approveTask(id, reqVO);
        return success(true);
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "审批驳回（仅审批人本人，意见必填）")
    @Parameter(name = "id", description = "审批任务ID", required = true, example = "1024")
    public CommonResult<Boolean> rejectTask(@PathVariable("id") Long id,
                                           @Valid @RequestBody ApprovalTaskRejectReqVO reqVO) {
        approvalTaskService.rejectTask(id, reqVO);
        return success(true);
    }

    @PutMapping("/{id}/transfer")
    @Operation(summary = "审批转交（仅审批人本人，接收人不能是自己）")
    @Parameter(name = "id", description = "审批任务ID", required = true, example = "1024")
    public CommonResult<Boolean> transferTask(@PathVariable("id") Long id,
                                             @Valid @RequestBody ApprovalTaskTransferReqVO reqVO) {
        approvalTaskService.transferTask(id, reqVO);
        return success(true);
    }

}
