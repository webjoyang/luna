package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplySaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentApplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentApplyController} 单元测试
 */
class PaymentApplyControllerTest {

    @Mock
    private PaymentApplyService paymentApplyService;

    @InjectMocks
    private PaymentApplyController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("分页查询：GET /page")
    void testGetPage() {
        PaymentApplyPageReqVO pageReqVO = new PaymentApplyPageReqVO();
        when(paymentApplyService.getPaymentApplyPage(any())).thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        CommonResult<PageResult<PaymentApplyRespVO>> result = controller.getPaymentApplyPage(pageReqVO);
        assertTrue(result.isSuccess());
        verify(paymentApplyService).getPaymentApplyPage(eq(pageReqVO));
    }

    @Test
    @DisplayName("详情查询：GET /get")
    void testGet() {
        Long id = 100L;
        PaymentApplyRespVO respVO = PaymentApplyRespVO.builder().id(id).applyNo("FK20261011001").build();
        when(paymentApplyService.getPaymentApply(id)).thenReturn(respVO);

        CommonResult<PaymentApplyRespVO> result = controller.getPaymentApply(id);
        assertTrue(result.isSuccess());
        assertEquals("FK20261011001", result.getData().getApplyNo());
        verify(paymentApplyService).getPaymentApply(id);
    }

    @Test
    @DisplayName("创建：POST /create")
    void testCreate() {
        PaymentApplySaveReqVO saveReqVO = PaymentApplySaveReqVO.builder().amount(new BigDecimal("100.00")).build();
        when(paymentApplyService.createPaymentApply(any())).thenReturn(200L);

        CommonResult<Long> result = controller.createPaymentApply(saveReqVO);
        assertTrue(result.isSuccess());
        assertEquals(200L, result.getData());
        verify(paymentApplyService).createPaymentApply(eq(saveReqVO));
    }

    @Test
    @DisplayName("更新：PUT /update")
    void testUpdate() {
        PaymentApplySaveReqVO saveReqVO = PaymentApplySaveReqVO.builder().id(200L).amount(new BigDecimal("200.00")).build();

        CommonResult<Boolean> result = controller.updatePaymentApply(saveReqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentApplyService).updatePaymentApply(eq(saveReqVO));
    }

    @Test
    @DisplayName("删除：DELETE /delete")
    void testDelete() {
        Long id = 200L;
        CommonResult<Boolean> result = controller.deletePaymentApply(id);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentApplyService).deletePaymentApply(id);
    }

    @Test
    @DisplayName("提交审批：POST /submit")
    void testSubmit() {
        Long id = 200L;
        when(paymentApplyService.submitPaymentApply(id)).thenReturn(888L);

        CommonResult<Long> result = controller.submitPaymentApply(id);
        assertTrue(result.isSuccess());
        assertEquals(888L, result.getData());
        verify(paymentApplyService).submitPaymentApply(id);
    }

    @Test
    @DisplayName("撤回审批：POST /cancel-approve")
    void testCancelApprove() {
        Long id = 200L;
        CommonResult<Boolean> result = controller.cancelApprovePaymentApply(id);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(paymentApplyService).cancelApprovePaymentApply(id);
    }

    @Test
    @DisplayName("预付余额查询：GET /prepay-balance")
    void testGetPrepayBalance() {
        Long supplierId = 300L;
        when(paymentApplyService.getPrepayBalance(supplierId)).thenReturn(new BigDecimal("1234.56"));

        CommonResult<BigDecimal> result = controller.getPrepayBalance(supplierId);
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("1234.56"), result.getData());
        verify(paymentApplyService).getPrepayBalance(supplierId);
    }

}
