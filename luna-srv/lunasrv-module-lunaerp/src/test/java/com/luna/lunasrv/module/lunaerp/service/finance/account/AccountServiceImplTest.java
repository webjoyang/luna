package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 资金账户服务单元测试（接缝 2）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {

    @InjectMocks
    private AccountServiceImpl accountService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AccountDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LedgerEntryDO.class);
    }

    @Test
    @DisplayName("createAccount: 正常创建账户，期初余额录入并初始化为当前余额")
    void testCreateAccount_success() {
        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setAccountName("招商银行基本户");
        req.setAccountType("BANK");
        req.setCurrency("RMB");
        req.setOpeningBalance(new BigDecimal("10000.00"));
        req.setRemark("公司主结算账户");

        when(accountMapper.selectByAccountName("招商银行基本户")).thenReturn(null);
        doAnswer(inv -> {
            AccountDO arg = inv.getArgument(0);
            arg.setId(101L);
            return 1;
        }).when(accountMapper).insert(any(AccountDO.class));

        Long id = accountService.createAccount(req);

        assertNotNull(id);
        assertEquals(101L, id);

        ArgumentCaptor<AccountDO> captor = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountMapper).insert(captor.capture());
        AccountDO saved = captor.getValue();
        assertEquals("招商银行基本户", saved.getAccountName());
        assertEquals(new BigDecimal("10000.00"), saved.getOpeningBalance());
        assertEquals(new BigDecimal("10000.00"), saved.getCurrentBalance());
        assertEquals("ENABLE", saved.getStatus());
    }

    @Test
    @DisplayName("createAccount: 账户名称重复拦截抛出 1_030_020_102 ACCOUNT_NAME_DUPLICATE")
    void testCreateAccount_duplicateName_throwsException() {
        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setAccountName("工商银行");
        req.setAccountType("BANK");
        req.setOpeningBalance(BigDecimal.ZERO);

        AccountDO existing = new AccountDO();
        existing.setId(201L);
        existing.setAccountName("工商银行");
        when(accountMapper.selectByAccountName("工商银行")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.createAccount(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NAME_DUPLICATE.getCode(), ex.getCode());
        verify(accountMapper, never()).insert(any(AccountDO.class));
    }

    @Test
    @DisplayName("updateAccount: 仅更新备注/状态，期初与当前余额保持只读不可变")
    void testUpdateAccount_success() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setAccountName("招商银行基本户");
        existing.setAccountType("BANK");
        existing.setCurrency("RMB");
        existing.setOpeningBalance(new BigDecimal("10000.00"));
        existing.setCurrentBalance(new BigDecimal("15000.00"));
        existing.setStatus("ENABLE");
        existing.setRemark("旧备注");

        when(accountMapper.selectById(101L)).thenReturn(existing);

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setStatus("DISABLE");
        req.setRemark("新备注");

        accountService.updateAccount(req);

        ArgumentCaptor<AccountDO> captor = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountMapper).updateById(captor.capture());
        AccountDO updated = captor.getValue();
        assertEquals("招商银行基本户", updated.getAccountName()); // 名称不变
        assertEquals("DISABLE", updated.getStatus());
        assertEquals("新备注", updated.getRemark());
        // 关键断言：期初余额与当前余额未被变更
        assertEquals(new BigDecimal("10000.00"), updated.getOpeningBalance());
        assertEquals(new BigDecimal("15000.00"), updated.getCurrentBalance());
    }

    @Test
    @DisplayName("updateAccount: 备注允许显式置空为 null (FieldStrategy.ALWAYS 验证)")
    void testUpdateAccount_clearRemark_success() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setStatus("ENABLE");
        existing.setRemark("原有备注");
        when(accountMapper.selectById(101L)).thenReturn(existing);

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setStatus("ENABLE");
        req.setRemark(null); // 清空备注

        accountService.updateAccount(req);

        ArgumentCaptor<AccountDO> captor = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountMapper).updateById(captor.capture());
        assertNull(captor.getValue().getRemark());
    }

    @Test
    @DisplayName("deleteAccount: 有分录引用的账户禁止删除，抛出 1_030_020_103 ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN")
    void testDeleteAccount_hasEntries_throwsForbidden() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        when(accountMapper.selectById(101L)).thenReturn(existing);
        when(ledgerService.countEntriesByAccountId(101L)).thenReturn(3L);

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.deleteAccount(101L));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN.getCode(), ex.getCode());
        verify(accountMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteAccount: 无分录引用的账户允许删除")
    void testDeleteAccount_noEntries_success() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        when(accountMapper.selectById(101L)).thenReturn(existing);
        when(ledgerService.countEntriesByAccountId(101L)).thenReturn(0L);

        accountService.deleteAccount(101L);
        verify(accountMapper, times(1)).deleteById(101L);
    }

    @Test
    @DisplayName("updateBalance: 增加余额（流入）条件更新成功")
    void testUpdateBalance_increase_success() {
        when(accountMapper.updateBalanceIncrease(101L, new BigDecimal("500.00"))).thenReturn(1);
        assertDoesNotThrow(() -> accountService.updateBalance(101L, new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("updateBalance: 扣减余额（流出）条件更新成功")
    void testUpdateBalance_decrease_success() {
        when(accountMapper.updateBalanceDecrease(101L, new BigDecimal("-300.00"), new BigDecimal("300.00"))).thenReturn(1);
        assertDoesNotThrow(() -> accountService.updateBalance(101L, new BigDecimal("-300.00")));
    }

    @Test
    @DisplayName("updateBalance: 扣减余额受阻（账户停用）抛出 1_030_020_101 ACCOUNT_DISABLED")
    void testUpdateBalance_disabled_throwsException() {
        when(accountMapper.updateBalanceDecrease(101L, new BigDecimal("-300.00"), new BigDecimal("300.00"))).thenReturn(0);

        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setStatus("DISABLE");
        existing.setCurrentBalance(new BigDecimal("1000.00"));
        when(accountMapper.selectById(101L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> accountService.updateBalance(101L, new BigDecimal("-300.00")));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updateBalance: 扣减余额受阻（余额不足）抛出 1_030_023_004 PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH")
    void testUpdateBalance_insufficientBalance_throwsException() {
        when(accountMapper.updateBalanceDecrease(101L, new BigDecimal("-300.00"), new BigDecimal("300.00"))).thenReturn(0);

        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setStatus("ENABLE");
        existing.setCurrentBalance(new BigDecimal("100.00")); // 小于 300
        when(accountMapper.selectById(101L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> accountService.updateBalance(101L, new BigDecimal("-300.00")));
        assertEquals(FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

}
