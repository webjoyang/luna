package com.luna.lunasrv.module.lunaerp.approval.config;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ApprovalConfigController} 单元测试
 */
class ApprovalConfigControllerTest {

    @Mock
    private ApprovalConfigService approvalConfigService;

    @InjectMocks
    private ApprovalConfigController approvalConfigController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("创建流程配置：POST /lunaerp/approval/config")
    void testCreateApprovalConfig() {
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder()
                .bizType("sales_contract")
                .name("销售合同审批链")
                .build();
        when(approvalConfigService.createApprovalConfig(any())).thenReturn(100L);

        CommonResult<Long> result = approvalConfigController.createApprovalConfig(reqVO);
        assertTrue(result.isSuccess());
        assertEquals(100L, result.getData());
        verify(approvalConfigService).createApprovalConfig(reqVO);
    }

    @Test
    @DisplayName("更新流程配置：PUT /lunaerp/approval/config/{id}")
    void testUpdateApprovalConfig() {
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder()
                .id(100L)
                .bizType("sales_contract")
                .name("更新后名称")
                .build();

        CommonResult<Boolean> result = approvalConfigController.updateApprovalConfig(100L, reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(approvalConfigService).updateApprovalConfig(eq(100L), eq(reqVO));
    }

    @Test
    @DisplayName("更新流程状态：PUT /lunaerp/approval/config/{id}/status 传 body")
    void testUpdateApprovalConfigStatus_body() {
        ApprovalConfigUpdateStatusReqVO body = ApprovalConfigUpdateStatusReqVO.builder().status("DISABLED").build();
        CommonResult<Boolean> result = approvalConfigController.updateApprovalConfigStatus(100L, body, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(approvalConfigService).updateApprovalConfigStatus(100L, "DISABLED");
    }

    @Test
    @DisplayName("更新流程状态：PUT /lunaerp/approval/config/{id}/status 传 param")
    void testUpdateApprovalConfigStatus_param() {
        CommonResult<Boolean> result = approvalConfigController.updateApprovalConfigStatus(100L, null, "ENABLED");
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(approvalConfigService).updateApprovalConfigStatus(100L, "ENABLED");
    }

    @Test
    @DisplayName("更新流程状态：状态为空抛出异常")
    void testUpdateApprovalConfigStatus_blank() {
        assertThrows(ServiceException.class, () -> approvalConfigController.updateApprovalConfigStatus(100L, null, "  "));
        verify(approvalConfigService, never()).updateApprovalConfigStatus(any(), any());
    }

    @Test
    @DisplayName("删除流程配置：DELETE /lunaerp/approval/config/{id}")
    void testDeleteApprovalConfig() {
        CommonResult<Boolean> result = approvalConfigController.deleteApprovalConfig(100L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(approvalConfigService).deleteApprovalConfig(100L);
    }

    @Test
    @DisplayName("获取流程详情：GET /lunaerp/approval/config/{id}")
    void testGetApprovalConfig() {
        ApprovalConfigRespVO respVO = ApprovalConfigRespVO.builder()
                .id(100L)
                .bizType("payment")
                .name("付款审批")
                .nodes(List.of(ApprovalConfigNodeVO.builder().sort(1).name("第一步").build()))
                .build();
        when(approvalConfigService.getApprovalConfig(100L)).thenReturn(respVO);

        CommonResult<ApprovalConfigRespVO> result = approvalConfigController.getApprovalConfig(100L);
        assertTrue(result.isSuccess());
        assertEquals(100L, result.getData().getId());
        assertEquals(1, result.getData().getNodes().size());
        verify(approvalConfigService).getApprovalConfig(100L);
    }

    @Test
    @DisplayName("获取流程分页：GET /lunaerp/approval/config/page")
    void testGetApprovalConfigPage() {
        ApprovalConfigPageReqVO pageReqVO = new ApprovalConfigPageReqVO();
        pageReqVO.setBizType("payment");
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);

        PageResult<ApprovalConfigRespVO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(approvalConfigService.getApprovalConfigPage(pageReqVO)).thenReturn(pageResult);

        CommonResult<PageResult<ApprovalConfigRespVO>> result = approvalConfigController.getApprovalConfigPage(pageReqVO);
        assertTrue(result.isSuccess());
        assertEquals(0L, result.getData().getTotal());
        verify(approvalConfigService).getApprovalConfigPage(pageReqVO);
    }

}
