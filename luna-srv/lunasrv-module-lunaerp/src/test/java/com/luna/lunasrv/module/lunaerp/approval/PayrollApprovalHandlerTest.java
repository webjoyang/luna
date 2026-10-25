package com.luna.lunasrv.module.lunaerp.approval;

import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.handler.PayrollApprovalHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工资单审批处理器单元测试（空 Handler 两步制验证）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class PayrollApprovalHandlerTest {

    @InjectMocks
    private PayrollApprovalHandler approvalHandler;

    @Test
    @DisplayName("supports 契约：支持 payroll 类型（大小写不敏感），拒绝其他单据类型")
    void testSupports() {
        assertTrue(approvalHandler.supports("payroll"));
        assertTrue(approvalHandler.supports("PAYROLL"));
        assertTrue(approvalHandler.supports("Payroll"));
        assertFalse(approvalHandler.supports("expense"));
        assertFalse(approvalHandler.supports("sales_contract"));
        assertFalse(approvalHandler.supports("payment"));
    }

    @Test
    @DisplayName("onApproved 空实现验证：APPROVED 不发钱，无资金过账副作用（对齐 11 号两步制先例）")
    void testOnApproved_NoSideEffect() {
        // 两步制：工资单审批通过时不写流水，由出纳后续登记发放，因此 onApproved 必须为空实现不报错
        assertDoesNotThrow(() -> approvalHandler.onApproved(1001L));
        assertDoesNotThrow(() -> approvalHandler.onApproved(null));
    }

}
