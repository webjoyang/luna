package com.luna.lunasrv.module.lunaerp.controller.finance.payroll;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.payroll.PayrollDefectService;
import com.luna.lunasrv.module.lunaerp.service.finance.payroll.PayrollService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 员工工资单控制器单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class PayrollControllerTest {

    @InjectMocks
    private PayrollController payrollController;

    @Mock
    private PayrollService payrollService;

    @Mock
    private PayrollDefectService payrollDefectService;

    @Test
    @DisplayName("端点委托：增删改查、提交、撤回、作废、套打均正确委托 Service")
    void testEndpointsDelegation() {
        // 1. 创建
        PayrollCreateReqVO createReq = new PayrollCreateReqVO();
        when(payrollService.createPayroll(createReq)).thenReturn(1001L);
        CommonResult<Long> createRes = payrollController.createPayroll(createReq);
        assertEquals(0, createRes.getCode());
        assertEquals(1001L, createRes.getData());
        verify(payrollService).createPayroll(createReq);

        // 2. 更新
        PayrollUpdateReqVO updateReq = new PayrollUpdateReqVO();
        CommonResult<Boolean> updateRes = payrollController.updatePayroll(updateReq);
        assertEquals(0, updateRes.getCode());
        assertTrue(updateRes.getData());
        verify(payrollService).updatePayroll(updateReq);

        // 3. 删除
        CommonResult<Boolean> deleteRes = payrollController.deletePayroll(1001L);
        assertEquals(0, deleteRes.getCode());
        assertTrue(deleteRes.getData());
        verify(payrollService).deletePayroll(1001L);

        // 4. 提交
        CommonResult<Boolean> submitRes = payrollController.submitPayroll(1001L);
        assertEquals(0, submitRes.getCode());
        assertTrue(submitRes.getData());
        verify(payrollService).submitPayroll(1001L);

        // 5. 撤回审批
        CommonResult<Boolean> cancelApproveRes = payrollController.cancelApprove(1001L);
        assertEquals(0, cancelApproveRes.getCode());
        assertTrue(cancelApproveRes.getData());
        verify(payrollService).cancelApprove(1001L);

        // 6. 作废
        PayrollCancelReqVO cancelReq = new PayrollCancelReqVO(1001L, "重新调整计件");
        CommonResult<Boolean> cancelRes = payrollController.cancelPayroll(null, cancelReq);
        assertEquals(0, cancelRes.getCode());
        assertTrue(cancelRes.getData());
        verify(payrollService).cancelPayroll(1001L, "重新调整计件");

        // 7. 详情
        PayrollRespVO detail = new PayrollRespVO();
        detail.setId(1001L);
        when(payrollService.getPayroll(1001L)).thenReturn(detail);
        CommonResult<PayrollRespVO> getRes = payrollController.getPayroll(1001L);
        assertEquals(0, getRes.getCode());
        assertEquals(1001L, getRes.getData().getId());
        verify(payrollService).getPayroll(1001L);

        // 8. 分页
        PayrollPageReqVO pageReq = new PayrollPageReqVO();
        when(payrollService.getPayrollPage(pageReq)).thenReturn(new PageResult<>(Collections.emptyList(), 0L));
        CommonResult<PageResult<PayrollRespVO>> pageRes = payrollController.getPayrollPage(pageReq);
        assertEquals(0, pageRes.getCode());
        assertNotNull(pageRes.getData());
        verify(payrollService).getPayrollPage(pageReq);

        // 9. 套打数据
        PayrollPrintRespVO printResp = new PayrollPrintRespVO();
        printResp.setId(1001L);
        when(payrollService.getPayrollPrintData(1001L)).thenReturn(printResp);
        CommonResult<PayrollPrintRespVO> printRes = payrollController.getPayrollPrintData(1001L);
        assertEquals(0, printRes.getCode());
        assertEquals(1001L, printRes.getData().getId());
        verify(payrollService).getPayrollPrintData(1001L);

        // 10. 次品扣款预览
        PayrollDefectPreviewRespVO previewResp = PayrollDefectPreviewRespVO.builder()
                .period("2026-10")
                .groups(Collections.emptyList())
                .build();
        when(payrollDefectService.getDefectPreview("2026-10", 1001L)).thenReturn(previewResp);
        CommonResult<PayrollDefectPreviewRespVO> previewRes = payrollController.getDefectPreview("2026-10", 1001L);
        assertEquals(0, previewRes.getCode());
        assertEquals("2026-10", previewRes.getData().getPeriod());
        verify(payrollDefectService).getDefectPreview("2026-10", 1001L);
    }

}
