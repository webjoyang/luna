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

    // ==================== 收款单 IN 侧接口测试 ====================

    @Test
    @DisplayName("createReceipt 接口：委托 Service 并返回收款单ID")
    void testCreateReceipt() {
        com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptSaveReqVO reqVO =
                new com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptSaveReqVO();
        when(paymentService.createReceipt(reqVO)).thenReturn(2001L);

        CommonResult<Long> result = controller.createReceipt(reqVO);
        assertTrue(result.isSuccess());
        assertEquals(2001L, result.getData());
        verify(paymentService).createReceipt(reqVO);
    }

    @Test
    @DisplayName("updateReceipt 接口：委托 Service 执行更新")
    void testUpdateReceipt() {
        com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptUpdateReqVO reqVO =
                new com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptUpdateReqVO();

        CommonResult<Boolean> result = controller.updateReceipt(reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentService).updateReceipt(reqVO);
    }

    @Test
    @DisplayName("deleteReceipt 接口：委托 Service 执行删除")
    void testDeleteReceipt() {
        CommonResult<Boolean> result = controller.deleteReceipt(2001L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentService).deleteReceipt(2001L);
    }

    @Test
    @DisplayName("submitReceipt 接口：委托 Service 提交审批")
    void testSubmitReceipt() {
        CommonResult<Boolean> result = controller.submitReceipt(2001L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentService).submitReceipt(2001L);
    }

    @Test
    @DisplayName("reverseReceipt 接口：委托 Service 执行收款红冲")
    void testReverseReceipt() {
        CommonResult<Boolean> result = controller.reverseReceipt(2001L);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentService).reverseReceipt(2001L);
    }

    @Test
    @DisplayName("advanceWriteoff 接口：委托 Service 执行预收转核销")
    void testAdvanceWriteoff() {
        com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AdvanceWriteoffReqVO reqVO =
                new com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AdvanceWriteoffReqVO();
        CommonResult<Boolean> result = controller.advanceWriteoff(reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentService).advanceWriteoff(reqVO);
    }

    @Test
    @DisplayName("allocatePreview 接口：委托 Service 试算并返回列表")
    void testAllocatePreview() {
        com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AllocatePreviewReqVO reqVO =
                new com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AllocatePreviewReqVO();
        when(paymentService.allocatePreview(reqVO)).thenReturn(List.of());

        CommonResult<List<com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AllocatePreviewRespVO>> result =
                controller.allocatePreview(reqVO);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        verify(paymentService).allocatePreview(reqVO);
    }

    @Test
    @DisplayName("getArBalanceList 接口：委托 Service 返回未核销分录列表")
    void testGetArBalanceList() {
        when(paymentService.getArBalanceList(100L, "RMB")).thenReturn(List.of());

        CommonResult<List<com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ArBalanceRespVO>> result =
                controller.getArBalanceList(100L, "RMB");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        verify(paymentService).getArBalanceList(100L, "RMB");
    }

    @Test
    @DisplayName("getReceipt 接口：委托 Service 返回收款单详情")
    void testGetReceipt() {
        com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRespVO respVO =
                new com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRespVO();
        respVO.setId(2001L);
        when(paymentService.getReceipt(2001L)).thenReturn(respVO);

        CommonResult<com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRespVO> result =
                controller.getReceipt(2001L);
        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(paymentService).getReceipt(2001L);
    }

    @Test
    @DisplayName("getReceiptPage 接口：委托 Service 返回收款单分页")
    void testGetReceiptPage() {
        com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptPageReqVO pageReqVO =
                new com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptPageReqVO();
        PageResult<com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRespVO> pageResult =
                new PageResult<>(List.of(), 0L);
        when(paymentService.getReceiptPage(pageReqVO)).thenReturn(pageResult);

        CommonResult<PageResult<com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRespVO>> result =
                controller.getReceiptPage(pageReqVO);
        assertTrue(result.isSuccess());
        assertEquals(pageResult, result.getData());
        verify(paymentService).getReceiptPage(pageReqVO);
    }

    // ==================== 付款单 OUT 侧接口测试 ====================

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
        assertPermission("createReceipt", new Class<?>[]{com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptSaveReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:create')");
        assertPermission("updateReceipt", new Class<?>[]{com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptUpdateReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:update')");
        assertPermission("deleteReceipt", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:delete')");
        assertPermission("submitReceipt", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:create')");
        assertPermission("allocatePreview", new Class<?>[]{com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AllocatePreviewReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");
        assertPermission("getArBalanceList", new Class<?>[]{Long.class, String.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");
        assertPermission("getReceipt", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");
        assertPermission("getReceiptPage", new Class<?>[]{com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptPageReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:query')");

        assertPermission("reverseReceipt", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:reverse')");
        assertPermission("advanceWriteoff", new Class<?>[]{com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AdvanceWriteoffReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:writeoff')");
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
