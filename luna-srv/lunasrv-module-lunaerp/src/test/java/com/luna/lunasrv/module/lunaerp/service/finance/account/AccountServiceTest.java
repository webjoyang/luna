package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceAccountStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceAccountTypeEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link AccountServiceImpl} 单元测试
 *
 * 覆盖 Issue 02:
 * 1. createAccount：opening_balance 仅此可写，current_balance 自动初始化等于 opening_balance，名称重名校验
 * 2. updateAccount：opening_balance 只读守卫（改动抛 ACCOUNT_OPENING_BALANCE_IMMUTABLE），仅更新备注/状态/名称
 * 3. deleteAccount：分录引用守卫（有分录引用报 ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN）
 * 4. updateBalance：余额条件更新原子通道、余额不足拦截（PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH）、停用拦截（ACCOUNT_DISABLED）
 */
class AccountServiceTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private AccountServiceImpl accountService;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AccountDO.class);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("createAccount：成功创建，current_balance 等于 opening_balance")
    void testCreateAccount_Success() {
        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setAccountName("招商银行基本户");
        req.setAccountType(FinanceAccountTypeEnum.BANK.getValue());
        req.setCurrency("CNY");
        req.setOpeningBalance(new BigDecimal("10000.00"));
        req.setRemark("主基本户");

        when(accountMapper.selectByAccountNameIncludingDeleted("招商银行基本户")).thenReturn(null);
        when(accountMapper.insert(any(AccountDO.class))).thenAnswer(invocation -> {
            AccountDO arg = invocation.getArgument(0);
            arg.setId(10L);
            return 1;
        });

        Long id = accountService.createAccount(req);

        assertEquals(10L, id);
        ArgumentCaptor<AccountDO> captor = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountMapper).insert(captor.capture());
        AccountDO saved = captor.getValue();
        assertEquals("招商银行基本户", saved.getAccountName());
        assertEquals(new BigDecimal("10000.00"), saved.getOpeningBalance());
        assertEquals(new BigDecimal("10000.00"), saved.getCurrentBalance());
        assertEquals(FinanceAccountStatusEnum.ENABLE.getValue(), saved.getStatus());
    }

    @Test
    @DisplayName("createAccount：重名时抛出 ACCOUNT_NAME_DUPLICATE")
    void testCreateAccount_DuplicateName_ThrowsException() {
        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setAccountName("已有账户");
        req.setAccountType(FinanceAccountTypeEnum.CASH.getValue());

        when(accountMapper.selectByAccountNameIncludingDeleted("已有账户"))
                .thenReturn(AccountDO.builder().id(99L).accountName("已有账户").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.createAccount(req));
        assertEquals(ACCOUNT_NAME_DUPLICATE.getCode(), ex.getCode());
        verify(accountMapper, never()).insert(any(AccountDO.class));
    }

    @Test
    @DisplayName("updateAccount：尝试修改 opening_balance 抛出 ACCOUNT_OPENING_BALANCE_IMMUTABLE")
    void testUpdateAccount_OpeningBalanceImmutable_ThrowsException() {
        AccountDO existing = AccountDO.builder()
                .id(10L)
                .accountName("招商基本户")
                .openingBalance(new BigDecimal("10000.00"))
                .currentBalance(new BigDecimal("12000.00"))
                .status("ENABLE")
                .build();
        when(accountMapper.selectById(10L)).thenReturn(existing);

        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setId(10L);
        req.setAccountName("招商基本户");
        req.setAccountType("BANK");
        req.setOpeningBalance(new BigDecimal("20000.00")); // 试图篡改期初余额

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.updateAccount(req));
        assertEquals(ACCOUNT_OPENING_BALANCE_IMMUTABLE.getCode(), ex.getCode());
        verify(accountMapper, never()).updateById(any(AccountDO.class));
    }

    @Test
    @DisplayName("updateAccount：更新状态与备注成功，不更新余额")
    void testUpdateAccount_Success_OnlyUpdatesStatusAndRemark() {
        AccountDO existing = AccountDO.builder()
                .id(10L)
                .accountName("招商基本户")
                .openingBalance(new BigDecimal("10000.00"))
                .currentBalance(new BigDecimal("12000.00"))
                .status("ENABLE")
                .build();
        when(accountMapper.selectById(10L)).thenReturn(existing);

        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setId(10L);
        req.setAccountName("招商基本户");
        req.setAccountType("BANK");
        req.setOpeningBalance(new BigDecimal("10000.00")); // 相同期初余额
        req.setStatus("DISABLE");
        req.setRemark("临时停用");

        accountService.updateAccount(req);

        ArgumentCaptor<AccountDO> captor = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountMapper).updateById(captor.capture());
        AccountDO updated = captor.getValue();
        assertEquals("DISABLE", updated.getStatus());
        assertEquals("临时停用", updated.getRemark());
        assertNull(updated.getCurrentBalance()); // currentBalance 绝不能被修改覆盖
    }

    @Test
    @DisplayName("deleteAccount：已被分录引用的账户禁止删除，抛出 ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN")
    void testDeleteAccount_HasEntries_ThrowsException() {
        AccountDO existing = AccountDO.builder().id(10L).accountName("招商基本户").build();
        when(accountMapper.selectById(10L)).thenReturn(existing);
        when(ledgerService.countEntriesByAccountId(10L)).thenReturn(5L);

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.deleteAccount(10L));
        assertEquals(ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN.getCode(), ex.getCode());
        verify(accountMapper, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteAccount：无分录引用时可成功删除")
    void testDeleteAccount_NoEntries_Success() {
        AccountDO existing = AccountDO.builder().id(10L).accountName("测试账户").build();
        when(accountMapper.selectById(10L)).thenReturn(existing);
        when(ledgerService.countEntriesByAccountId(10L)).thenReturn(0L);

        accountService.deleteAccount(10L);

        verify(accountMapper).deleteById(10L);
    }

    @Test
    @DisplayName("updateBalance：条件更新成功")
    void testUpdateBalance_Success() {
        when(accountMapper.updateBalanceConditional(10L, new BigDecimal("500.00"))).thenReturn(1);

        assertDoesNotThrow(() -> accountService.updateBalance(10L, new BigDecimal("500.00")));
        verify(accountMapper).updateBalanceConditional(10L, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("updateBalance：出账时余额不足，抛出 PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH")
    void testUpdateBalance_BalanceNotEnough_ThrowsException() {
        when(accountMapper.updateBalanceConditional(10L, new BigDecimal("-6000.00"))).thenReturn(0);

        AccountDO existing = AccountDO.builder()
                .id(10L)
                .status("ENABLE")
                .currentBalance(new BigDecimal("5000.00")) // 5000 < 6000
                .build();
        when(accountMapper.selectById(10L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                accountService.updateBalance(10L, new BigDecimal("-6000.00")));
        assertEquals(PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updateBalance：账户已停用，抛出 ACCOUNT_DISABLED")
    void testUpdateBalance_AccountDisabled_ThrowsException() {
        when(accountMapper.updateBalanceConditional(10L, new BigDecimal("100.00"))).thenReturn(0);

        AccountDO existing = AccountDO.builder()
                .id(10L)
                .status("DISABLE")
                .currentBalance(new BigDecimal("5000.00"))
                .build();
        when(accountMapper.selectById(10L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                accountService.updateBalance(10L, new BigDecimal("100.00")));
        assertEquals(ACCOUNT_DISABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getSimpleAccountList：查询所有 ENABLE 账户")
    void testGetSimpleAccountList() {
        when(accountMapper.selectListByStatusEnable()).thenReturn(List.of(
                AccountDO.builder().id(1L).accountName("账户1").status("ENABLE").build()
        ));
        assertEquals(1, accountService.getSimpleAccountList().size());
    }

}
