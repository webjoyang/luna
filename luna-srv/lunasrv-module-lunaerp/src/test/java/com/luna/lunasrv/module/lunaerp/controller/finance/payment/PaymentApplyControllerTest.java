package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentApplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentApplyController} 单元测试（契约与权限校验）
 */
@ExtendWith(MockitoExtension.class)
public class PaymentApplyControllerTest {

    @Mock
    private PaymentApplyService paymentApplyService;

    @InjectMocks
    private PaymentApplyController controller;

    @Test
    @DisplayName("createPaymentApply 接口：委托 Service 并返回主键ID")
    void testCreatePaymentApply() {
        PaymentApplyCreateReqVO reqVO = new PaymentApplyCreateReqVO();
        when(paymentApplyService.createPaymentApply(reqVO)).thenReturn(1001L);

        CommonResult<Long> result = controller.createPaymentApply(reqVO);
        assertTrue(result.isSuccess());
        assertEquals(1001L, result.getData());
        verify(paymentApplyService).createPaymentApply(reqVO);
    }

    @Test
    @DisplayName("updatePaymentApply 接口：委托 Service 并返回成功")
    void testUpdatePaymentApply() {
        PaymentApplyUpdateReqVO reqVO = new PaymentApplyUpdateReqVO();
        CommonResult<Boolean> result = controller.updatePaymentApply(reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentApplyService).updatePaymentApply(reqVO);
    }

    @Test
    @DisplayName("deletePaymentApply 接口：委托 Service 并返回成功")
    void testDeletePaymentApply() {
        CommonResult<Boolean> result = controller.deletePaymentApply(1001L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentApplyService).deletePaymentApply(1001L);
    }

    @Test
    @DisplayName("getPaymentApply 接口：委托 Service 并返回详情")
    void testGetPaymentApply() {
        PaymentApplyRespVO respVO = new PaymentApplyRespVO();
        respVO.setId(1001L);
        when(paymentApplyService.getPaymentApply(1001L)).thenReturn(respVO);

        CommonResult<PaymentApplyRespVO> result = controller.getPaymentApply(1001L);
        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(paymentApplyService).getPaymentApply(1001L);
    }

    @Test
    @DisplayName("getPaymentApplyPage 接口：委托 Service 并返回分页")
    void testGetPaymentApplyPage() {
        PaymentApplyPageReqVO pageReqVO = new PaymentApplyPageReqVO();
        PageResult<PaymentApplyRespVO> pageResult = new PageResult<>(List.of(new PaymentApplyRespVO()), 1L);
        when(paymentApplyService.getPaymentApplyPage(pageReqVO)).thenReturn(pageResult);

        CommonResult<PageResult<PaymentApplyRespVO>> result = controller.getPaymentApplyPage(pageReqVO);
        assertTrue(result.isSuccess());
        assertEquals(pageResult, result.getData());
        verify(paymentApplyService).getPaymentApplyPage(pageReqVO);
    }

    @Test
    @DisplayName("submitPaymentApply 接口：委托 Service 提交审批并返回成功")
    void testSubmitPaymentApply() {
        CommonResult<Boolean> result = controller.submitPaymentApply(1001L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentApplyService).submitPaymentApply(1001L);
    }

    @Test
    @DisplayName("cancelApprove 接口：委托 Service 撤回审批并返回成功")
    void testCancelApprove() {
        CommonResult<Boolean> result = controller.cancelApprove(1001L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentApplyService).cancelApprove(1001L);
    }

    @Test
    @DisplayName("getPrepayBalance 接口：委托 Service 并返回实时预付余额")
    void testGetPrepayBalance() {
        when(paymentApplyService.getPrepayBalance(3001L)).thenReturn(new BigDecimal("6500.00"));

        CommonResult<BigDecimal> result = controller.getPrepayBalance(3001L);
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("6500.00"), result.getData());
        verify(paymentApplyService).getPrepayBalance(3001L);
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码")
    void testPermissions() throws NoSuchMethodException {
        assertPermission("createPaymentApply", new Class<?>[]{PaymentApplyCreateReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:apply')");
        assertPermission("updatePaymentApply", new Class<?>[]{PaymentApplyUpdateReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:apply')");
        assertPermission("deletePaymentApply", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:apply')");
        assertPermission("submitPaymentApply", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:apply')");
        assertPermission("cancelApprove", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:apply')");
        assertPermission("getPaymentApply", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");
        assertPermission("getPaymentApplyPage", new Class<?>[]{PaymentApplyPageReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");
        assertPermission("getPrepayBalance", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");
    }

    private void assertPermission(String methodName, Class<?>[] paramTypes, String expectedExpression) throws NoSuchMethodException {
        Method method = PaymentApplyController.class.getMethod(methodName, paramTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " 必须标注 @PreAuthorize");
        assertEquals(expectedExpression, annotation.value());
    }

}
