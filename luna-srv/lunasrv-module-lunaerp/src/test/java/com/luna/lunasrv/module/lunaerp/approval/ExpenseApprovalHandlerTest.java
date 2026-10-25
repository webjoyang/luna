package com.luna.lunasrv.module.lunaerp.approval;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.approval.biz.ExpenseApprovalBizHandler;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.handler.ExpenseApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseMapper;
import com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.expense.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 报销审批处理器测试（Seam 2）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseApprovalHandlerTest {

    @InjectMocks
    private ExpenseApprovalBizHandler bizHandler;

    @InjectMocks
    private ExpenseApprovalHandler approvalHandler;

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private ExpenseService expenseService;

    @org.junit.jupiter.api.BeforeEach
    public void setUp() {
        com.luna.lunasrv.module.lunaerp.support.TableInfoTestHelper.initTableInfo(ExpenseDO.class);
    }

    @Test
    @DisplayName("ExpenseApprovalBizHandler：元数据契约（bizType=expense、名称、单号）")
    public void testBizHandler_Metadata() {
        assertEquals(ApprovalHandler.BIZ_TYPE_EXPENSE, bizHandler.getBizType());
        assertEquals("费用报销", bizHandler.getBizTypeName());

        ExpenseDO expense = new ExpenseDO();
        expense.setId(5001L);
        expense.setExpenseNo("BX20261019001");
        when(expenseMapper.selectById(5001L)).thenReturn(expense);

        assertEquals("BX20261019001", bizHandler.getBizNo(5001L));
    }

    @Test
    @DisplayName("ExpenseApprovalBizHandler：校验提交状态（不存在024_000、审批中报错、已通过024_001）")
    public void testBizHandler_ValidateCanSubmit() {
        when(expenseMapper.selectById(9999L)).thenReturn(null);
        ServiceException exNotExists = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(9999L));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_NOT_EXISTS.getCode(), exNotExists.getCode());

        ExpenseDO approving = new ExpenseDO();
        approving.setId(5001L);
        approving.setApprovalStatus("APPROVING");
        when(expenseMapper.selectById(5001L)).thenReturn(approving);
        ServiceException exApproving = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(5001L));
        assertEquals(ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR.getCode(), exApproving.getCode());

        ExpenseDO approved = new ExpenseDO();
        approved.setId(5002L);
        approved.setApprovalStatus("APPROVED");
        when(expenseMapper.selectById(5002L)).thenReturn(approved);
        ServiceException exApproved = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(5002L));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_STATUS_NOT_ALLOWED.getCode(), exApproved.getCode());

        ExpenseDO pending = new ExpenseDO();
        pending.setId(5003L);
        pending.setApprovalStatus("PENDING");
        when(expenseMapper.selectById(5003L)).thenReturn(pending);
        assertDoesNotThrow(() -> bizHandler.validateCanSubmit(5003L));
    }

    @Test
    @DisplayName("ExpenseApprovalBizHandler：状态更新回写")
    public void testBizHandler_UpdateApprovalStatus() {
        bizHandler.updateApprovalStatus(5001L, "APPROVING");
        verify(expenseMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    @DisplayName("ExpenseApprovalHandler：supports 匹配性")
    public void testApprovalHandler_Supports() {
        assertTrue(approvalHandler.supports("expense"));
        assertTrue(approvalHandler.supports("EXPENSE"));
        assertFalse(approvalHandler.supports("payment"));
        assertFalse(approvalHandler.supports("sales_contract"));
    }

    @Test
    @DisplayName("ExpenseApprovalHandler：onApproved 触发同事务过账")
    public void testApprovalHandler_OnApproved_Success() {
        approvalHandler.onApproved(5001L);
        verify(expenseService).approveExpenseAndPost(5001L);
    }

    @Test
    @DisplayName("ExpenseApprovalHandler：过账失败抛出异常（回滚审批事务）")
    public void testApprovalHandler_OnApproved_RollbackOnError() {
        doThrow(new ServiceException(1_030_023_004, "账户余额不足"))
                .when(expenseService).approveExpenseAndPost(5001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalHandler.onApproved(5001L));
        assertEquals(1_030_023_004, ex.getCode());
    }

}
