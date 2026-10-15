package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRegisterReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentController} 单元测试（契约与权限校验）
 */
@ExtendWith(MockitoExtension.class)
public class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController controller;

    @Test
    @DisplayName("registerPayment 接口：委托 Service 并返回付款单ID")
    void testRegisterPayment() {
        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        when(paymentService.registerPayment(reqVO)).thenReturn(1001L);

        CommonResult<Long> result = controller.registerPayment(reqVO);
        assertTrue(result.isSuccess());
        assertEquals(1001L, result.getData());
        verify(paymentService).registerPayment(reqVO);
    }

    @Test
    @DisplayName("getPayment 接口：委托 Service 并返回详情")
    void testGetPayment() {
        PaymentRespVO respVO = new PaymentRespVO();
        respVO.setId(1001L);
        when(paymentService.getPayment(1001L)).thenReturn(respVO);

        CommonResult<PaymentRespVO> result = controller.getPayment(1001L);
        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(paymentService).getPayment(1001L);
    }

    @Test
    @DisplayName("getPaymentPage 接口：委托 Service 并返回分页结果")
    void testGetPaymentPage() {
        PaymentPageReqVO pageReqVO = new PaymentPageReqVO();
        PageResult<PaymentRespVO> pageResult = new PageResult<>(List.of(new PaymentRespVO()), 1L);
        when(paymentService.getPaymentPage(pageReqVO)).thenReturn(pageResult);

        CommonResult<PageResult<PaymentRespVO>> result = controller.getPaymentPage(pageReqVO);
        assertTrue(result.isSuccess());
        assertEquals(pageResult, result.getData());
        verify(paymentService).getPaymentPage(pageReqVO);
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码（纵向越权守卫）")
    void testPermissions() throws NoSuchMethodException {
        assertPermission("registerPayment", new Class<?>[]{PaymentRegisterReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:register')");
        assertPermission("getPayment", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");
        assertPermission("getPaymentPage", new Class<?>[]{PaymentPageReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");
    }

    @Test
    @DisplayName("纵向越权守卫：验证无 :register 权限时抛出 AccessDeniedException (403)")
    void testRegisterPayment_AccessDenied() {
        PaymentRegisterReqVO reqVO = new PaymentRegisterReqVO();
        when(paymentService.registerPayment(reqVO)).thenThrow(new org.springframework.security.access.AccessDeniedException("无付款登记权限"));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> controller.registerPayment(reqVO));
    }

    private void assertPermission(String methodName, Class<?>[] paramTypes, String expectedExpression) throws NoSuchMethodException {
        Method method = PaymentController.class.getMethod(methodName, paramTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " 必须标注 @PreAuthorize");
        assertEquals(expectedExpression, annotation.value());
    }

}
