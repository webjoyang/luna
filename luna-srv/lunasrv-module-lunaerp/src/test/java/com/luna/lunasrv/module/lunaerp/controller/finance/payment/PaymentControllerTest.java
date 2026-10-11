package com.luna.lunasrv.module.lunaerp.controller.finance.payment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRegisterItemReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRegisterReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    @DisplayName("分页查询端点转发验证")
    void testGetPaymentPage() {
        PaymentPageReqVO reqVO = new PaymentPageReqVO();
        reqVO.setSupplierId(100L);

        PageResult<PaymentRespVO> expectedPage = PageResult.empty();
        when(paymentService.getPaymentPage(any(PaymentPageReqVO.class))).thenReturn(expectedPage);

        CommonResult<PageResult<PaymentRespVO>> result = paymentController.getPaymentPage(reqVO);
        assertNotNull(result);
        assertEquals(expectedPage, result.getData());
        verify(paymentService).getPaymentPage(reqVO);
    }

    @Test
    @DisplayName("获取详情端点转发验证")
    void testGetPayment() {
        PaymentRespVO expected = PaymentRespVO.builder().id(999L).paymentNo("PAY20261011001").build();
        when(paymentService.getPayment(eq(999L))).thenReturn(expected);

        CommonResult<PaymentRespVO> result = paymentController.getPayment(999L);
        assertNotNull(result);
        assertEquals(expected, result.getData());
        verify(paymentService).getPayment(999L);
    }

}
