package com.luna.lunasrv.module.lunaerp.approval.instance;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ApprovalInstanceController} 单元测试
 */
class ApprovalInstanceControllerTest {

    @Mock
    private ApprovalInstanceService approvalInstanceService;

    @InjectMocks
    private ApprovalInstanceController approvalInstanceController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("提交审批：POST /lunaerp/approval/instance/submit")
    void testSubmitInstance() {
        ApprovalInstanceSubmitReqVO reqVO = new ApprovalInstanceSubmitReqVO();
        reqVO.setBizType("sales_contract");
        reqVO.setBizId(1024L);
        when(approvalInstanceService.submitInstance(any())).thenReturn(5001L);

        CommonResult<Long> result = approvalInstanceController.submitInstance(reqVO);
        assertTrue(result.isSuccess());
        assertEquals(5001L, result.getData());
        verify(approvalInstanceService).submitInstance(reqVO);
    }

    @Test
    @DisplayName("撤回审批：PUT /lunaerp/approval/instance/{id}/withdraw")
    void testWithdrawInstance() {
        ApprovalInstanceWithdrawReqVO reqVO = new ApprovalInstanceWithdrawReqVO();
        reqVO.setReason("填写有误");

        CommonResult<Boolean> result = approvalInstanceController.withdrawInstance(5001L, reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(approvalInstanceService).withdrawInstance(eq(5001L), eq(reqVO));
    }

    @Test
    @DisplayName("获取审批详情：GET /lunaerp/approval/instance/{id}")
    void testGetInstance() {
        ApprovalInstanceRespVO respVO = ApprovalInstanceRespVO.builder()
                .id(5001L)
                .bizType("sales_contract")
                .bizId(1024L)
                .status("APPROVING")
                .build();
        when(approvalInstanceService.getInstance(5001L)).thenReturn(respVO);

        CommonResult<ApprovalInstanceRespVO> result = approvalInstanceController.getInstance(5001L);
        assertTrue(result.isSuccess());
        assertEquals(5001L, result.getData().getId());
        assertEquals("APPROVING", result.getData().getStatus());
    }

    @Test
    @DisplayName("按单据获取最新审批：GET /lunaerp/approval/instance/get-by-biz")
    void testGetLatestInstanceByBiz() {
        ApprovalInstanceRespVO respVO = ApprovalInstanceRespVO.builder()
                .id(5001L)
                .bizType("sales_contract")
                .bizId(1024L)
                .status("APPROVING")
                .build();
        when(approvalInstanceService.getLatestInstanceByBiz("sales_contract", 1024L)).thenReturn(respVO);

        CommonResult<ApprovalInstanceRespVO> result = approvalInstanceController.getLatestInstanceByBiz("sales_contract", 1024L);
        assertTrue(result.isSuccess());
        assertEquals(5001L, result.getData().getId());
    }

}
