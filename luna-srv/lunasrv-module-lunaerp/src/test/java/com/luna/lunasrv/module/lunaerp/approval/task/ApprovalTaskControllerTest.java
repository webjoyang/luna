package com.luna.lunasrv.module.lunaerp.approval.task;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ApprovalTaskController} 单元测试
 */
class ApprovalTaskControllerTest {

    @Mock
    private ApprovalTaskService approvalTaskService;

    @InjectMocks
    private ApprovalTaskController approvalTaskController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("待我审批分页：GET /lunaerp/approval/task/todo-page")
    void testGetTodoPage() {
        ApprovalTaskTodoPageReqVO reqVO = new ApprovalTaskTodoPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        ApprovalTaskTodoRespVO respVO = ApprovalTaskTodoRespVO.builder()
                .id(101L)
                .instanceId(501L)
                .bizNo("HT001")
                .nodeName("主管审批")
                .build();
        when(approvalTaskService.getTodoPage(any())).thenReturn(new PageResult<>(List.of(respVO), 1L));

        CommonResult<PageResult<ApprovalTaskTodoRespVO>> result = approvalTaskController.getTodoPage(reqVO);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getList().size());
        assertEquals(101L, result.getData().getList().get(0).getId());
        verify(approvalTaskService).getTodoPage(reqVO);
    }

    @Test
    @DisplayName("审批通过：PUT /lunaerp/approval/task/{id}/approve")
    void testApproveTask() {
        ApprovalTaskApproveReqVO reqVO = new ApprovalTaskApproveReqVO();
        reqVO.setComment("同意");

        CommonResult<Boolean> result = approvalTaskController.approveTask(101L, reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(approvalTaskService).approveTask(eq(101L), eq(reqVO));
    }

    @Test
    @DisplayName("审批驳回：PUT /lunaerp/approval/task/{id}/reject")
    void testRejectTask() {
        ApprovalTaskRejectReqVO reqVO = new ApprovalTaskRejectReqVO();
        reqVO.setComment("驳回重填");

        CommonResult<Boolean> result = approvalTaskController.rejectTask(101L, reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(approvalTaskService).rejectTask(eq(101L), eq(reqVO));
    }

    @Test
    @DisplayName("审批转交：PUT /lunaerp/approval/task/{id}/transfer")
    void testTransferTask() {
        ApprovalTaskTransferReqVO reqVO = new ApprovalTaskTransferReqVO();
        reqVO.setToUserId(202L);
        reqVO.setComment("转交");

        CommonResult<Boolean> result = approvalTaskController.transferTask(101L, reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(approvalTaskService).transferTask(eq(101L), eq(reqVO));
    }

}
