package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.CustomerReceiptService;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PaymentController} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private CustomerReceiptService customerReceiptService;

    @Mock
    private AccountService accountService;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    @DisplayName("付款登记端点转发验证")
    void testRegisterPayment() {
        PaymentRegisterItemReqVO item = PaymentRegisterItemReqVO.builder()
                .applyId(1L)
                .allocateAmount(new BigDecimal("1000.00"))
                .build();
        PaymentRegisterReqVO reqVO = PaymentRegisterReqVO.builder()
                .supplierId(100L)
                .accountId(200L)
                .payAmount(new BigDecimal("1000.00"))
                .items(Collections.singletonList(item))
                .build();

        when(paymentService.registerPayment(any(PaymentRegisterReqVO.class))).thenReturn(123L);

        CommonResult<Long> result = paymentController.registerPayment(reqVO);
        assertNotNull(result);
        assertEquals(123L, result.getData());
        verify(paymentService).registerPayment(reqVO);
    }

    @Test
    @DisplayName("创建客户收款单端点转发验证")
    void testCreateReceipt() {
        ReceiptSaveReqVO reqVO = ReceiptSaveReqVO.builder()
                .customerId(1001L)
                .receiptType("RECEIPT")
                .currency("CNY")
                .payAmount(new BigDecimal("5000.00"))
                .receiptDate(LocalDate.of(2026, 10, 12))
                .accountId(2001L)
                .build();
        when(customerReceiptService.createReceipt(any(ReceiptSaveReqVO.class))).thenReturn(888L);

        CommonResult<Long> result = paymentController.createReceipt(reqVO);
        assertNotNull(result);
        assertEquals(888L, result.getData());
        verify(customerReceiptService).createReceipt(reqVO);
    }

    @Test
    @DisplayName("提交客户收款单端点转发验证")
    void testSubmitReceipt() {
        when(customerReceiptService.submitReceipt(888L)).thenReturn(666L);
        CommonResult<Long> result = paymentController.submitReceipt(888L);
        assertNotNull(result);
        assertEquals(666L, result.getData());
        verify(customerReceiptService).submitReceipt(888L);
    }

    @Test
    @DisplayName("自动核销分配试算端点转发验证")
    void testAllocatePreview() {
        AllocatePreviewReqVO reqVO = AllocatePreviewReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .payAmount(new BigDecimal("3000.00"))
                .build();
        AllocatePreviewRespVO respVO = AllocatePreviewRespVO.builder()
                .totalPayAmount(new BigDecimal("3000.00"))
                .totalAllocated(new BigDecimal("3000.00"))
                .build();
        when(customerReceiptService.allocatePreview(any(AllocatePreviewReqVO.class))).thenReturn(respVO);

        CommonResult<AllocatePreviewRespVO> result = paymentController.allocatePreview(reqVO);
        assertNotNull(result);
        assertEquals(new BigDecimal("3000.00"), result.getData().getTotalAllocated());
        verify(customerReceiptService).allocatePreview(reqVO);
    }

    @Test
    @DisplayName("未核销明细分录查询端点转发验证")
    void testGetArBalance() {
        ArBalanceRespVO ar = ArBalanceRespVO.builder().ledgerEntryId(501L).build();
        when(customerReceiptService.getArBalanceList(1001L, "CNY")).thenReturn(List.of(ar));

        CommonResult<List<ArBalanceRespVO>> result = paymentController.getArBalance(1001L, "CNY");
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(customerReceiptService).getArBalanceList(1001L, "CNY");
    }

    @Test
    @DisplayName("分页查询端点（OUT付款与IN收款分流）验证")
    void testGetPaymentPage() {
        PaymentPageReqVO outReq = new PaymentPageReqVO();
        outReq.setDirection("OUT");
        PageResult<PaymentRespVO> expectedOut = PageResult.empty();
        when(paymentService.getPaymentPage(any(PaymentPageReqVO.class))).thenReturn(expectedOut);
        CommonResult<PageResult<PaymentRespVO>> outResult = paymentController.getPaymentPage(outReq);
        assertEquals(expectedOut, outResult.getData());
        verify(paymentService).getPaymentPage(outReq);

        PaymentPageReqVO inReq = new PaymentPageReqVO();
        inReq.setDirection("IN");
        PageResult<PaymentRespVO> expectedIn = PageResult.empty();
        when(customerReceiptService.getReceiptPage(any(PaymentPageReqVO.class))).thenReturn(expectedIn);
        CommonResult<PageResult<PaymentRespVO>> inResult = paymentController.getPaymentPage(inReq);
        assertEquals(expectedIn, inResult.getData());
        verify(customerReceiptService).getReceiptPage(inReq);
    }

    @Test
    @DisplayName("获取详情端点（OUT与IN分流）验证")
    void testGetPayment() {
        PaymentDO inPayment = PaymentDO.builder().id(888L).direction("IN").build();
        when(paymentMapper.selectById(888L)).thenReturn(inPayment);
        PaymentRespVO inResp = PaymentRespVO.builder().id(888L).direction("IN").build();
        when(customerReceiptService.getReceipt(888L)).thenReturn(inResp);

        CommonResult<PaymentRespVO> result = paymentController.getPayment(888L);
        assertNotNull(result);
        assertEquals(888L, result.getData().getId());
        verify(customerReceiptService).getReceipt(888L);
    }

    @Test
    @DisplayName("收款单红冲端点验证")
    void testReverseReceipt() {
        CommonResult<Boolean> result = paymentController.reverseReceipt(999L);
        assertTrue(result.getData());
        verify(customerReceiptService).reverseReceipt(999L);
    }

    @Test
    @DisplayName("预收转核销端点验证")
    void testAdvanceWriteoff() {
        AdvanceWriteoffReqVO reqVO = AdvanceWriteoffReqVO.builder()
                .paymentId(100L)
                .items(List.of(ReceiptRefSaveVO.builder().ledgerEntryId(501L).allocateAmount(new BigDecimal("1000.00")).build()))
                .build();
        CommonResult<Boolean> result = paymentController.advanceWriteoff(reqVO);
        assertTrue(result.getData());
        verify(customerReceiptService).advanceWriteoff(reqVO);
    }

    @Test
    @DisplayName("获取预收余额端点验证")
    void testGetAdvanceBalance() {
        when(customerReceiptService.getAdvanceBalance(100L)).thenReturn(new BigDecimal("5000.00"));
        CommonResult<BigDecimal> result = paymentController.getAdvanceBalance(100L);
        assertEquals(new BigDecimal("5000.00"), result.getData());
        verify(customerReceiptService).getAdvanceBalance(100L);
    }

}
