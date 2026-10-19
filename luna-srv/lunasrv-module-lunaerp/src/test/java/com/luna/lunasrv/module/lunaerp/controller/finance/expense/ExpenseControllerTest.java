package com.luna.lunasrv.module.lunaerp.controller.finance.expense;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.expense.ExpenseService;
import com.luna.lunasrv.module.lunaerp.service.finance.expense.ExpenseServiceImpl;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 员工费用报销单控制器单元测试（Seam 3）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseControllerTest {

    @InjectMocks
    private ExpenseController expenseController;

    @Mock
    private ExpenseService expenseService;

    @Test
    @DisplayName("端点委托：增删改查、提交、撤回、套打均正确委托 Service")
    void testEndpointsDelegation() {
        ExpenseCreateReqVO createReq = new ExpenseCreateReqVO();
        when(expenseService.createExpense(createReq)).thenReturn(5001L);
        CommonResult<Long> createRes = expenseController.createExpense(createReq);
        assertEquals(0, createRes.getCode());
        assertEquals(5001L, createRes.getData());
        verify(expenseService).createExpense(createReq);

        ExpenseUpdateReqVO updateReq = new ExpenseUpdateReqVO();
        CommonResult<Boolean> updateRes = expenseController.updateExpense(updateReq);
        assertEquals(0, updateRes.getCode());
        assertTrue(updateRes.getData());
        verify(expenseService).updateExpense(updateReq);

        CommonResult<Boolean> deleteRes = expenseController.deleteExpense(5001L);
        assertEquals(0, deleteRes.getCode());
        assertTrue(deleteRes.getData());
        verify(expenseService).deleteExpense(5001L);

        CommonResult<Boolean> submitRes = expenseController.submitExpense(5001L);
        assertEquals(0, submitRes.getCode());
        assertTrue(submitRes.getData());
        verify(expenseService).submitExpense(5001L);

        CommonResult<Boolean> cancelRes = expenseController.cancelApprove(5001L);
        assertEquals(0, cancelRes.getCode());
        assertTrue(cancelRes.getData());
        verify(expenseService).cancelApprove(5001L);

        ExpenseRespVO respVO = new ExpenseRespVO();
        respVO.setId(5001L);
        when(expenseService.getExpense(5001L)).thenReturn(respVO);
        CommonResult<ExpenseRespVO> getRes = expenseController.getExpense(5001L);
        assertEquals(0, getRes.getCode());
        assertEquals(5001L, getRes.getData().getId());

        ExpensePageReqVO pageReq = new ExpensePageReqVO();
        PageResult<ExpenseRespVO> pageResult = new PageResult<>(List.of(respVO), 1L);
        when(expenseService.getExpensePage(pageReq)).thenReturn(pageResult);
        CommonResult<PageResult<ExpenseRespVO>> pageRes = expenseController.getExpensePage(pageReq);
        assertEquals(0, pageRes.getCode());
        assertEquals(1, pageRes.getData().getList().size());

        ExpensePrintRespVO printResp = ExpensePrintRespVO.builder().id(5001L).build();
        when(expenseService.getExpensePrintData(5001L)).thenReturn(printResp);
        CommonResult<ExpensePrintRespVO> printRes = expenseController.getExpensePrintData(5001L);
        assertEquals(0, printRes.getCode());
        assertEquals(5001L, printRes.getData().getId());
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码")
    void testPermissions() throws NoSuchMethodException {
        Method createMethod = ExpenseController.class.getMethod("createExpense", ExpenseCreateReqVO.class);
        PreAuthorize createAuth = createMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(createAuth);
        assertEquals("@ss.hasPermission('lunaerp:finance:expense:create')", createAuth.value());

        Method updateMethod = ExpenseController.class.getMethod("updateExpense", ExpenseUpdateReqVO.class);
        PreAuthorize updateAuth = updateMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(updateAuth);
        assertEquals("@ss.hasPermission('lunaerp:finance:expense:update')", updateAuth.value());

        Method deleteMethod = ExpenseController.class.getMethod("deleteExpense", Long.class);
        PreAuthorize deleteAuth = deleteMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(deleteAuth);
        assertEquals("@ss.hasPermission('lunaerp:finance:expense:delete')", deleteAuth.value());

        Method submitMethod = ExpenseController.class.getMethod("submitExpense", Long.class);
        PreAuthorize submitAuth = submitMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(submitAuth);
        assertEquals("@ss.hasPermission('lunaerp:finance:expense:submit')", submitAuth.value());

        Method cancelMethod = ExpenseController.class.getMethod("cancelApprove", Long.class);
        PreAuthorize cancelAuth = cancelMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(cancelAuth);
        assertEquals("@ss.hasPermission('lunaerp:finance:expense:submit')", cancelAuth.value());

        Method getMethod = ExpenseController.class.getMethod("getExpense", Long.class);
        PreAuthorize getAuth = getMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(getAuth);
        assertEquals("@ss.hasPermission('lunaerp:finance:expense:query')", getAuth.value());

        Method pageMethod = ExpenseController.class.getMethod("getExpensePage", ExpensePageReqVO.class);
        PreAuthorize pageAuth = pageMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(pageAuth);
        assertEquals("@ss.hasPermission('lunaerp:finance:expense:query')", pageAuth.value());

        Method printMethod = ExpenseController.class.getMethod("getExpensePrintData", Long.class);
        PreAuthorize printAuth = printMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(printAuth);
        assertEquals("@ss.hasPermission('lunaerp:finance:expense:print')", printAuth.value());
    }

    @Test
    @DisplayName("脱敏与豁免注解审查：业务 VO 标注 @Sensitive，打印 VO 豁免脱敏")
    void testDesensitizationAndExemption() throws NoSuchFieldException {
        Field totalAmountField = ExpenseRespVO.class.getDeclaredField("totalAmount");
        Sensitive totalSens = totalAmountField.getAnnotation(Sensitive.class);
        assertNotNull(totalSens, "ExpenseRespVO.totalAmount 必须标注 @Sensitive");
        assertEquals(SensitiveType.AMOUNT, totalSens.type());

        Field itemAmountField = ExpenseItemRespVO.class.getDeclaredField("originalAmount");
        Sensitive itemSens = itemAmountField.getAnnotation(Sensitive.class);
        assertNotNull(itemSens, "ExpenseItemRespVO.originalAmount 必须标注 @Sensitive");
        assertEquals(SensitiveType.AMOUNT, itemSens.type());

        // 套打 VO 豁免脱敏
        for (Field field : ExpensePrintRespVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "ExpensePrintRespVO." + field.getName() + " 必须豁免脱敏，严禁标注 @Sensitive");
        }
        for (Field field : ExpensePrintItemRespVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "ExpensePrintItemRespVO." + field.getName() + " 必须豁免脱敏，严禁标注 @Sensitive");
        }
    }

    @Test
    @DisplayName("留痕注解审查：关键服务方法正确标注 @LogRecord")
    void testLogRecordAnnotations() throws NoSuchMethodException {
        Method createMethod = ExpenseServiceImpl.class.getMethod("createExpense", ExpenseCreateReqVO.class);
        assertNotNull(createMethod.getAnnotation(LogRecord.class), "createExpense 必须标注 @LogRecord");

        Method submitMethod = ExpenseServiceImpl.class.getMethod("submitExpense", Long.class);
        assertNotNull(submitMethod.getAnnotation(LogRecord.class), "submitExpense 必须标注 @LogRecord");

        Method cancelMethod = ExpenseServiceImpl.class.getMethod("cancelApprove", Long.class);
        assertNotNull(cancelMethod.getAnnotation(LogRecord.class), "cancelApprove 必须标注 @LogRecord");

        Method postMethod = ExpenseServiceImpl.class.getMethod("approveExpenseAndPost", Long.class);
        assertNotNull(postMethod.getAnnotation(LogRecord.class), "approveExpenseAndPost 必须标注 @LogRecord");

        Method printMethod = ExpenseServiceImpl.class.getMethod("getExpensePrintData", Long.class);
        assertNotNull(printMethod.getAnnotation(LogRecord.class), "getExpensePrintData 必须标注 @LogRecord");
    }

}
