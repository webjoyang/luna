package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * {@link CustomerReceiptApprovalHandler} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CustomerReceiptApprovalHandlerTest {

    @Mock
    private CustomerReceiptService customerReceiptService;

    @InjectMocks
    private CustomerReceiptApprovalHandler approvalHandler;

    @Test
    @DisplayName("支持业务类型验证")
    void testSupports() {
        assertTrue(approvalHandler.supports("CUSTOMER_RECEIPT"));
        assertTrue(approvalHandler.supports("customer_receipt"));
        assertFalse(approvalHandler.supports("sales_contract"));
    }

    @Test
    @DisplayName("审批通过触发同事务过账调用")
    void testOnApproved() {
        approvalHandler.onApproved(100L);
        verify(customerReceiptService).postReceiptOnApproval(100L);
    }

}
