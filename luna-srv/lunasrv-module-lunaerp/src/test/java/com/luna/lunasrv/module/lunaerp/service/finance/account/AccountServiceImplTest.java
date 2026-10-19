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
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LedgerEntryDO.class);
    }

    @Test
    @DisplayName("createAccount: 正常创建账户，账号/性质录入，期初余额初始化为当前余额")
    void testCreateAccount_success() {
        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setAccountName("招商银行基本户");
        req.setAccountType("BANK");
        req.setAccountNo("6225880123456789");
        req.setAccountNature("PUBLIC");
        req.setCurrency("RMB");
        req.setOpeningBalance(new BigDecimal("10000.00"));
        req.setRemark("公司主结算账户");

        when(accountMapper.selectByAccountNameIncludingDeleted("招商银行基本户")).thenReturn(null);
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
        assertEquals("BANK", saved.getAccountType());
        assertEquals("6225880123456789", saved.getAccountNo());
        assertEquals("PUBLIC", saved.getAccountNature());
        assertEquals(new BigDecimal("10000.00"), saved.getOpeningBalance());
        assertEquals(new BigDecimal("10000.00"), saved.getCurrentBalance());
        assertEquals("ENABLE", saved.getStatus());
    }

    @Test
    @DisplayName("createAccount: BANK/WECHAT/ALIPAY 类型账号为空抛出 1_030_020_105 ACCOUNT_NO_REQUIRED")
    void testCreateAccount_accountNoRequiredForBank_throws105() {
        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setAccountName("支付宝企业账户");
        req.setAccountType("ALIPAY");
        req.setAccountNo(null); // 缺失账号
        req.setOpeningBalance(BigDecimal.ZERO);

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.createAccount(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NO_REQUIRED.getCode(), ex.getCode());
        verify(accountMapper, never()).insert(any(AccountDO.class));
    }

    @Test
    @DisplayName("createAccount: CASH 类型账号允许为空")
    void testCreateAccount_cashAccountNoCanBeBlank_success() {
        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setAccountName("现金保险柜");
        req.setAccountType("CASH");
        req.setAccountNo(""); // 现金允许为空
        req.setOpeningBalance(new BigDecimal("500.00"));

        when(accountMapper.selectByAccountNameIncludingDeleted("现金保险柜")).thenReturn(null);
        doAnswer(inv -> {
            AccountDO arg = inv.getArgument(0);
            arg.setId(102L);
            return 1;
        }).when(accountMapper).insert(any(AccountDO.class));

        Long id = accountService.createAccount(req);
        assertEquals(102L, id);
    }

    @Test
    @DisplayName("createAccount: 命中已软删除账户重名拦截抛出 1_030_020_102 ACCOUNT_NAME_DUPLICATE")
    void testCreateAccount_duplicateNameIncludingDeleted_throws102() {
        AccountSaveReqVO req = new AccountSaveReqVO();
        req.setAccountName("历史旧户");
        req.setAccountType("CASH");
        req.setOpeningBalance(BigDecimal.ZERO);

        AccountDO softDeleted = new AccountDO();
        softDeleted.setId(201L);
        softDeleted.setAccountName("历史旧户");
        softDeleted.setDeleted(true); // 软删除行
        when(accountMapper.selectByAccountNameIncludingDeleted("历史旧户")).thenReturn(softDeleted);

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.createAccount(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NAME_DUPLICATE.getCode(), ex.getCode());
        verify(accountMapper, never()).insert(any(AccountDO.class));
    }

    @Test
    @DisplayName("updateAccount: 仅更新备注/状态/账号/性质成功")
    void testUpdateAccount_basicFields_success() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setAccountName("招商银行基本户");
        existing.setAccountType("BANK");
        existing.setAccountNo("6225880123456789");
        existing.setAccountNature("PUBLIC");
        existing.setCurrency("RMB");
        existing.setOpeningBalance(new BigDecimal("10000.00"));
        existing.setCurrentBalance(new BigDecimal("15000.00"));
        existing.setStatus("ENABLE");
        existing.setRemark("旧备注");

        when(accountMapper.selectById(101L)).thenReturn(existing);

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setStatus("DISABLE");
        req.setAccountNo("6225880999999999");
        req.setAccountNature("PRIVATE");
        req.setRemark("新备注");

        accountService.updateAccount(req);

        ArgumentCaptor<AccountDO> captor = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountMapper).updateById(captor.capture());
        AccountDO updated = captor.getValue();
        assertEquals("招商银行基本户", updated.getAccountName());
        assertEquals("6225880999999999", updated.getAccountNo());
        assertEquals("PRIVATE", updated.getAccountNature());
        assertEquals("DISABLE", updated.getStatus());
        assertEquals("新备注", updated.getRemark());
        assertEquals(new BigDecimal("10000.00"), updated.getOpeningBalance());
        assertEquals(new BigDecimal("15000.00"), updated.getCurrentBalance());
    }

    @Test
    @DisplayName("updateAccount: BANK 账户清空账号抛出 1_030_020_105 ACCOUNT_NO_REQUIRED")
    void testUpdateAccount_clearAccountNoOnBank_throws105() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setAccountType("BANK");
        existing.setAccountNo("6225880123456789");
        existing.setStatus("ENABLE");
        when(accountMapper.selectById(101L)).thenReturn(existing);

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setAccountNo(""); // 清空银行账号

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.updateAccount(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NO_REQUIRED.getCode(), ex.getCode());
        verify(accountMapper, never()).updateById(any(AccountDO.class));
    }

    @Test
    @DisplayName("updateAccount: 存在分录时修改期初余额被拦截抛出 1_030_020_104 ACCOUNT_OPENING_BALANCE_IMMUTABLE")
    void testUpdateAccount_hasEntries_modifyOpeningBalance_throws104() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setOpeningBalance(new BigDecimal("10000.00"));
        existing.setStatus("ENABLE");
        when(accountMapper.selectById(101L)).thenReturn(existing);
        when(ledgerService.countEntriesByAccountId(101L)).thenReturn(5L); // 有5条分录

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setOpeningBalance(new BigDecimal("20000.00")); // 试图改期初

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.updateAccount(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_OPENING_BALANCE_IMMUTABLE.getCode(), ex.getCode());
        verify(accountMapper, never()).updateById(any(AccountDO.class));
    }

    @Test
    @DisplayName("updateAccount: 无分录时允许修改期初余额，物化当前余额同步更新为新期初")
    void testUpdateAccount_noEntries_modifyOpeningBalance_successAndSyncCurrentBalance() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setOpeningBalance(new BigDecimal("10000.00"));
        existing.setCurrentBalance(new BigDecimal("10000.00"));
        existing.setStatus("ENABLE");
        when(accountMapper.selectById(101L)).thenReturn(existing);
        when(ledgerService.countEntriesByAccountId(101L)).thenReturn(0L); // 零分录

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setOpeningBalance(new BigDecimal("20000.00"));

        accountService.updateAccount(req);

        ArgumentCaptor<AccountDO> captor = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountMapper).updateById(captor.capture());
        AccountDO updated = captor.getValue();
        assertEquals(new BigDecimal("20000.00"), updated.getOpeningBalance());
        assertEquals(new BigDecimal("20000.00"), updated.getCurrentBalance()); // 同步
    }

    @Test
    @DisplayName("updateAccount: 更新名称与其他账户（含软删行）撞名抛出 1_030_020_102 ACCOUNT_NAME_DUPLICATE")
    void testUpdateAccount_duplicateNameIncludingDeleted_throws102() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setAccountName("原名称");
        when(accountMapper.selectById(101L)).thenReturn(existing);

        AccountDO otherDeleted = new AccountDO();
        otherDeleted.setId(202L);
        otherDeleted.setAccountName("已占名称");
        otherDeleted.setDeleted(true);
        when(accountMapper.selectByAccountNameIncludingDeleted("已占名称")).thenReturn(otherDeleted);

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setAccountName("已占名称");

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.updateAccount(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NAME_DUPLICATE.getCode(), ex.getCode());
        verify(accountMapper, never()).updateById(any(AccountDO.class));
    }

    @Test
    @DisplayName("updateAccount: 快捷状态流转防静默清空备注（coding-standards.md §15.3）")
    void testUpdateAccount_shortcutStatusUpdate_preservesHistoricalRemark() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setStatus("ENABLE");
        existing.setRemark("原有重要备注");
        when(accountMapper.selectById(101L)).thenReturn(existing);

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setStatus("DISABLE");
        req.setRemark(null); // 快捷状态切换未传备注

        accountService.updateAccount(req);

        ArgumentCaptor<AccountDO> captor = ArgumentCaptor.forClass(AccountDO.class);
        verify(accountMapper).updateById(captor.capture());
        assertEquals("原有重要备注", captor.getValue().getRemark());
        assertEquals("DISABLE", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("updateAccount: 账户不存在或横向越权跨租户查不到时抛出 1_030_020_100 ACCOUNT_NOT_EXISTS")
    void testUpdateAccount_notExistsOrCrossTenant_throws100() {
        when(accountMapper.selectById(999L)).thenReturn(null);

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(999L);
        req.setStatus("DISABLE");

        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.updateAccount(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
        verify(accountMapper, never()).updateById(any(AccountDO.class));
    }

    @Test
    @DisplayName("updateAccount: 状态更新非法值抛出异常")
    void testUpdateAccount_invalidStatus_throwsException() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        existing.setStatus("ENABLE");
        when(accountMapper.selectById(101L)).thenReturn(existing);

        AccountUpdateReqVO req = new AccountUpdateReqVO();
        req.setId(101L);
        req.setStatus("ILLEGAL_STATUS");

        assertThrows(ServiceException.class, () -> accountService.updateAccount(req));
        verify(accountMapper, never()).updateById(any(AccountDO.class));
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
    @DisplayName("deleteAccount: 无分录引用的账户允许删除进入回收站（软删除）")
    void testDeleteAccount_noEntries_success() {
        AccountDO existing = new AccountDO();
        existing.setId(101L);
        when(accountMapper.selectById(101L)).thenReturn(existing);
        when(ledgerService.countEntriesByAccountId(101L)).thenReturn(0L);

        accountService.deleteAccount(101L);
        verify(accountMapper, times(1)).deleteById(101L);
    }

    @Test
    @DisplayName("deleteAccount: 账户不存在时抛出 1_030_020_100 ACCOUNT_NOT_EXISTS")
    void testDeleteAccount_notExists_throws100() {
        when(accountMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> accountService.deleteAccount(999L));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
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
        existing.setCurrentBalance(new BigDecimal("100.00"));
        when(accountMapper.selectById(101L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> accountService.updateBalance(101L, new BigDecimal("-300.00")));
        assertEquals(FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }

}
