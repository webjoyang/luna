package com.luna.lunasrv.module.lunaerp.controller.finance.payroll;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.payroll.PayrollDefectService;
import com.luna.lunasrv.module.lunaerp.service.finance.payroll.PayrollService;
import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
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


    @Test
    @DisplayName("端点委托：发放登记与批次分页均正确委托 Service")
    void testPaymentEndpointsDelegation() {
        // 1. 发放登记委托
        PayrollPaymentRegisterReqVO registerReq = PayrollPaymentRegisterReqVO.builder()
                .payrollId(1001L)
                .accountId(2001L)
                .amount(new BigDecimal("4000.00"))
                .build();
        when(payrollService.registerPayrollPayment(registerReq)).thenReturn(501L);
        CommonResult<Long> registerRes = payrollController.registerPayrollPayment(registerReq);
        assertEquals(0, registerRes.getCode());
        assertEquals(501L, registerRes.getData());
        verify(payrollService).registerPayrollPayment(registerReq);

        // 2. 批次分页委托
        PayrollPaymentPageReqVO pageReq = new PayrollPaymentPageReqVO(1001L);
        PayrollPaymentRespVO paymentVO = PayrollPaymentRespVO.builder()
                .id(501L)
                .paymentNo("GF20261019001")
                .build();
        when(payrollService.getPayrollPaymentPage(pageReq)).thenReturn(new PageResult<>(List.of(paymentVO), 1L));
        CommonResult<PageResult<PayrollPaymentRespVO>> pageRes = payrollController.getPayrollPaymentPage(pageReq);
        assertEquals(0, pageRes.getCode());
        assertEquals(1, pageRes.getData().getList().size());
        assertEquals("GF20261019001", pageRes.getData().getList().get(0).getPaymentNo());
        verify(payrollService).getPayrollPaymentPage(pageReq);
    }

    @Test
    @DisplayName("权限注解校验：确认发放登记与批次分页声明了正确的权限码（纵向越权守卫）")
    void testPaymentEndpointsPermissions() throws NoSuchMethodException {
        // 1. registerPayrollPayment
        Method registerMethod = PayrollController.class.getMethod("registerPayrollPayment", PayrollPaymentRegisterReqVO.class);
        PreAuthorize registerAuth = registerMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(registerAuth, "registerPayrollPayment 必须标注 @PreAuthorize 防纵向越权");
        assertEquals("@ss.hasPermission('lunaerp:finance:payroll:pay')", registerAuth.value());

        // 2. getPayrollPaymentPage
        Method pageMethod = PayrollController.class.getMethod("getPayrollPaymentPage", PayrollPaymentPageReqVO.class);
        PreAuthorize pageAuth = pageMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(pageAuth, "getPayrollPaymentPage 必须标注 @PreAuthorize 防纵向越权");
        assertEquals("@ss.hasAnyPermissions('lunaerp:finance:payroll:query', 'lunaerp:finance:payroll:pay')", pageAuth.value());
    }

    @Test
    @DisplayName("批次不可改删守卫：验证 PayrollController 绝不暴露发放批次的 update/delete/cancel/modify 端点")
    void testNoPaymentUpdateOrDeleteEndpoints() {
        Method[] methods = PayrollController.class.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName().toLowerCase();
            if (name.contains("payment")) {
                assertFalse(name.contains("update"), "PayrollController 严禁声明发放批次 update 端点: " + method.getName());
                assertFalse(name.contains("delete"), "PayrollController 严禁声明发放批次 delete 端点: " + method.getName());
                assertFalse(name.contains("modify"), "PayrollController 严禁声明发放批次 modify 端点: " + method.getName());
                assertFalse(name.contains("cancel"), "PayrollController 严禁声明发放批次 cancel 端点: " + method.getName());
            }
        }
    }
}
