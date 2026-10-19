package com.luna.lunasrv.module.lunaerp.controller.finance.account;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
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
 * 资金账户控制器及权限/脱敏/留痕单元测试（接缝 4）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class AccountControllerTest {

    @InjectMocks
    private AccountController accountController;

    @Mock
    private AccountService accountService;

    @Test
    @DisplayName("updateAccount: 委托 Service 修改账户返回成功")
    void testUpdateAccount() {
        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);

        CommonResult<Boolean> result = accountController.updateAccount(req);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(accountService).updateAccount(req);
    }

    @Test
    @DisplayName("deleteAccount: 委托 Service 删除账户返回成功")
    void testDeleteAccount() {
        CommonResult<Boolean> result = accountController.deleteAccount(101L);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(accountService).deleteAccount(101L);
    }

    @Test
    @DisplayName("getSimpleAccountList: 下拉列表查询")
    void testGetSimpleAccountList() {
        FinanceAccountSimpleRespVO simpleVO = new FinanceAccountSimpleRespVO();
        simpleVO.setId(101L);
        when(accountService.getSimpleAccountList()).thenReturn(List.of(simpleVO));

        CommonResult<List<FinanceAccountSimpleRespVO>> result = accountController.getSimpleAccountList();
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("权限码审查：Controller 各端点权限码配置严格符合规范")
    void testControllerPermissions() throws Exception {
        Method updateMethod = AccountController.class.getMethod("updateAccount", AccountUpdateReqVO.class);
        PreAuthorize updateAuth = updateMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(updateAuth);
        assertTrue(updateAuth.value().contains("lunaerp:finance:account:update"));

        Method deleteMethod = AccountController.class.getMethod("deleteAccount", Long.class);
        PreAuthorize deleteAuth = deleteMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(deleteAuth);
        assertTrue(deleteAuth.value().contains("lunaerp:finance:account:delete"));

        Method simpleMethod = AccountController.class.getMethod("getSimpleAccountList");
        PreAuthorize simpleAuth = simpleMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(simpleAuth);
        assertTrue(simpleAuth.value().contains("lunaerp:finance:account:query"));
    }

    @Test
    @DisplayName("脱敏注解审查：AccountRespVO 与 FinanceAccountSimpleRespVO 正确标注 @Sensitive")
    void testSensitiveAnnotations() throws Exception {
        Field accountNo = AccountRespVO.class.getDeclaredField("accountNo");
        Sensitive accountNoSens = accountNo.getAnnotation(Sensitive.class);
        assertNotNull(accountNoSens, "accountNo 必须标注 @Sensitive");
        assertEquals(SensitiveConstants.PERMISSION_CONTACT, accountNoSens.seePermission());
        assertEquals(SensitiveType.BANK_CARD, accountNoSens.type());

        Field openingBalance = AccountRespVO.class.getDeclaredField("openingBalance");
        Sensitive openingSens = openingBalance.getAnnotation(Sensitive.class);
        assertNotNull(openingSens, "openingBalance 必须标注 @Sensitive");
        assertEquals(SensitiveConstants.PERMISSION_AMOUNT, openingSens.seePermission());
        assertEquals(SensitiveType.AMOUNT, openingSens.type());

        Field currentBalance = AccountRespVO.class.getDeclaredField("currentBalance");
        Sensitive currentSens = currentBalance.getAnnotation(Sensitive.class);
        assertNotNull(currentSens, "currentBalance 必须标注 @Sensitive");
        assertEquals(SensitiveConstants.PERMISSION_AMOUNT, currentSens.seePermission());
        assertEquals(SensitiveType.AMOUNT, currentSens.type());

        Field simpleBalance = FinanceAccountSimpleRespVO.class.getDeclaredField("currentBalance");
        Sensitive simpleBalanceSens = simpleBalance.getAnnotation(Sensitive.class);
        assertNotNull(simpleBalanceSens, "FinanceAccountSimpleRespVO.currentBalance 必须标注 @Sensitive");
    }

    @Test
    @DisplayName("留痕注解审查：AccountServiceImpl 增改删均标注 @LogRecord")
    void testLogRecordAnnotations() throws Exception {
        Class<?> serviceImpl = Class.forName("com.luna.lunasrv.module.lunaerp.service.finance.account.AccountServiceImpl");

        Method createMethod = serviceImpl.getMethod("createAccount", AccountSaveReqVO.class);
        LogRecord createLog = createMethod.getAnnotation(LogRecord.class);
        assertNotNull(createLog, "createAccount 必须标注 @LogRecord");
        assertEquals("FINANCE_ACCOUNT", createLog.type());
        assertEquals("CREATE", createLog.subType());

        Method updateMethod = serviceImpl.getMethod("updateAccount", AccountUpdateReqVO.class);
        LogRecord updateLog = updateMethod.getAnnotation(LogRecord.class);
        assertNotNull(updateLog, "updateAccount 必须标注 @LogRecord");
        assertEquals("FINANCE_ACCOUNT", updateLog.type());
        assertTrue(updateLog.subType().contains("STATUS_CHANGE") && updateLog.subType().contains("OPENING_UPDATE"));

        Method deleteMethod = serviceImpl.getMethod("deleteAccount", Long.class);
        LogRecord deleteLog = deleteMethod.getAnnotation(LogRecord.class);
        assertNotNull(deleteLog, "deleteAccount 必须标注 @LogRecord");
        assertEquals("FINANCE_ACCOUNT", deleteLog.type());
        assertEquals("DELETE", deleteLog.subType());
    }

}
