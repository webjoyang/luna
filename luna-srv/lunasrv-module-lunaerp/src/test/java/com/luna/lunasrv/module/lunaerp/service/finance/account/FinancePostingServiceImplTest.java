package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.account.dto.PostingReq;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 财务统一过账服务单元测试（接缝 1）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class FinancePostingServiceImplTest {

    @InjectMocks
    private FinancePostingServiceImpl postingService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private LedgerService ledgerService;

    @Test
    @DisplayName("post: 账户不存在或横向越权跨租户查不到时抛出 1_030_020_100 ACCOUNT_NOT_EXISTS")
    void testPost_accountNotExists_throws020_100() {
        PostingReq req = new PostingReq()
                .setAccountId(999L)
                .setDirection("DEBIT")
                .setCategory("IN_CUSTOMER")
                .setCurrency("RMB")
                .setOriginalAmount(new BigDecimal("100.00"));

        when(accountMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> postingService.post(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
        verify(accountMapper, never()).updateBalanceIncrease(any(), any());
        verify(ledgerService, never()).append(any());
    }

    @Test
    @DisplayName("post: 账户停用时抛出 1_030_020_101 ACCOUNT_DISABLED，且历史余额与统计不受影响")
    void testPost_accountDisabled_throws020_101() {
        AccountDO account = new AccountDO();
        account.setId(101L);
        account.setStatus("DISABLE");
        account.setCurrency("RMB");
        BigDecimal initialBalance = new BigDecimal("8888.88");
        account.setCurrentBalance(initialBalance);
        when(accountMapper.selectById(101L)).thenReturn(account);

        PostingReq req = new PostingReq()
                .setAccountId(101L)
                .setDirection("DEBIT")
                .setCategory("IN_CUSTOMER")
                .setCurrency("RMB")
                .setOriginalAmount(new BigDecimal("100.00"));

        ServiceException ex = assertThrows(ServiceException.class, () -> postingService.post(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode(), ex.getCode());
        assertEquals(initialBalance, account.getCurrentBalance());
        verify(accountMapper, never()).updateBalanceIncrease(any(), any());
        verify(accountMapper, never()).updateBalanceDecrease(any(), any(), any());
        verify(ledgerService, never()).append(any());
    }

    @Test
    @DisplayName("post: 分录币种为空或与账户币种不符抛出 1_030_020_106 ACCOUNT_CURRENCY_MISMATCH")
    void testPost_currencyMismatch_throws020_106() {
        AccountDO account = new AccountDO();
        account.setId(101L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountMapper.selectById(101L)).thenReturn(account);

        PostingReq reqMismatch = new PostingReq()
                .setAccountId(101L)
                .setDirection("DEBIT")
                .setCategory("IN_CUSTOMER")
                .setCurrency("USD")
                .setOriginalAmount(new BigDecimal("100.00"));

        ServiceException ex1 = assertThrows(ServiceException.class, () -> postingService.post(reqMismatch));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_CURRENCY_MISMATCH.getCode(), ex1.getCode());

        PostingReq reqEmpty = new PostingReq()
                .setAccountId(101L)
                .setDirection("DEBIT")
                .setCategory("IN_CUSTOMER")
                .setCurrency("   ")
                .setOriginalAmount(new BigDecimal("100.00"));

        ServiceException ex2 = assertThrows(ServiceException.class, () -> postingService.post(reqEmpty));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_CURRENCY_MISMATCH.getCode(), ex2.getCode());

        PostingReq reqNull = new PostingReq()
                .setAccountId(101L)
                .setDirection("DEBIT")
                .setCategory("IN_CUSTOMER")
                .setCurrency(null)
                .setOriginalAmount(new BigDecimal("100.00"));

        ServiceException ex3 = assertThrows(ServiceException.class, () -> postingService.post(reqNull));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_CURRENCY_MISMATCH.getCode(), ex3.getCode());

        verify(accountMapper, never()).updateBalanceIncrease(any(), any());
        verify(ledgerService, never()).append(any());
    }

    @Test
    @DisplayName("post: 流入（DEBIT）增加余额并成功写入经营分录，RMB汇率强制为1")
    void testPost_inflow_increaseBalance_andInsertLedgerEntry_success() {
        AccountDO account = new AccountDO();
        account.setId(101L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        account.setCurrentBalance(new BigDecimal("1000.00"));
        when(accountMapper.selectById(101L)).thenReturn(account);
        when(accountMapper.updateBalanceIncrease(eq(101L), eq(new BigDecimal("500.00")))).thenReturn(1);
        when(ledgerService.append(any(LedgerEntryDO.class))).thenReturn(888L);

        PostingReq req = new PostingReq()
                .setAccountId(101L)
                .setDirection("DEBIT")
                .setCategory("IN_CUSTOMER")
                .setPartyType("CUSTOMER")
                .setPartyId(2001L)
                .setPartyName("优质客户有限公司")
                .setCurrency("RMB")
                .setOriginalAmount(new BigDecimal("500.00"))
                .setBizDate(LocalDate.of(2026, 10, 19))
                .setRefType("RECEIPT")
                .setRefId(3001L)
                .setRefNo("SK-20261019-001")
                .setRemark("客户货款流入");

        Long entryId = postingService.post(req);
        assertEquals(888L, entryId);

        verify(accountMapper).updateBalanceIncrease(eq(101L), eq(new BigDecimal("500.00")));

        ArgumentCaptor<LedgerEntryDO> captor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService).append(captor.capture());
        LedgerEntryDO entry = captor.getValue();
        assertEquals(101L, entry.getAccountId());
        assertEquals("DEBIT", entry.getDirection());
        assertEquals("IN_CUSTOMER", entry.getCategory());
        assertEquals("CUSTOMER", entry.getPartyType());
        assertEquals(2001L, entry.getPartyId());
        assertEquals("优质客户有限公司", entry.getPartyName());
        assertEquals("RMB", entry.getCurrency());
        assertEquals(new BigDecimal("500.00"), entry.getOriginalAmount());
        assertEquals(BigDecimal.ONE, entry.getExchangeRate());
        assertEquals(new BigDecimal("500.00"), entry.getBaseAmount());
        assertEquals("RECEIPT", entry.getRefType());
        assertEquals(3001L, entry.getRefId());
        assertEquals("SK-20261019-001", entry.getRefNo());
        assertEquals("客户货款流入", entry.getRemark());
    }

    @Test
    @DisplayName("post: 流出（CREDIT）余额不足时抛出 1_030_023_004 PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH")
    void testPost_outflow_decreaseBalance_insufficientBalance_throws023_004() {
        AccountDO account = new AccountDO();
        account.setId(101L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        account.setCurrentBalance(new BigDecimal("200.00")); // 小于扣减的 500

        when(accountMapper.selectById(101L)).thenReturn(account);
        when(accountMapper.updateBalanceDecrease(eq(101L), eq(new BigDecimal("-500.00")), eq(new BigDecimal("500.00"))))
                .thenReturn(0);

        PostingReq req = new PostingReq()
                .setAccountId(101L)
                .setDirection("CREDIT")
                .setCategory("OUT_EXPENSE")
                .setPartyType("EMPLOYEE")
                .setPartyId(10L)
                .setPartyName("张三")
                .setCurrency("RMB")
                .setOriginalAmount(new BigDecimal("500.00"))
                .setBizDate(LocalDate.of(2026, 10, 19))
                .setRefType("EXPENSE")
                .setRefId(4001L)
                .setRefNo("BX-20261019-001")
                .setRemark("差旅报销付款");

        ServiceException ex = assertThrows(ServiceException.class, () -> postingService.post(req));
        assertEquals(FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
        verify(ledgerService, never()).append(any());
    }

    @Test
    @DisplayName("post: 流出（CREDIT）扣减受阻且账户已停用时抛出 1_030_020_101 ACCOUNT_DISABLED")
    void testPost_outflow_decreaseBalance_accountDisabled_throws020_101() {
        AccountDO accountInitial = new AccountDO();
        accountInitial.setId(101L);
        accountInitial.setStatus("ENABLE");
        accountInitial.setCurrency("RMB");

        AccountDO accountWhenChecked = new AccountDO();
        accountWhenChecked.setId(101L);
        accountWhenChecked.setStatus("DISABLE"); // 并发被停用
        accountWhenChecked.setCurrentBalance(new BigDecimal("1000.00"));

        when(accountMapper.selectById(101L)).thenReturn(accountInitial, accountWhenChecked);
        when(accountMapper.updateBalanceDecrease(eq(101L), eq(new BigDecimal("-500.00")), eq(new BigDecimal("500.00"))))
                .thenReturn(0);

        PostingReq req = new PostingReq()
                .setAccountId(101L)
                .setDirection("CREDIT")
                .setCategory("OUT_SUPPLIER")
                .setCurrency("RMB")
                .setOriginalAmount(new BigDecimal("500.00"));

        ServiceException ex = assertThrows(ServiceException.class, () -> postingService.post(req));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode(), ex.getCode());
        verify(ledgerService, never()).append(any());
    }

    @Test
    @DisplayName("post: 外币分录原币×汇率四舍五入保留2位小数折合本位币")
    void testPost_foreignCurrency_baseAmountRoundedHalfUp() {
        AccountDO account = new AccountDO();
        account.setId(102L);
        account.setStatus("ENABLE");
        account.setCurrency("USD");
        account.setCurrentBalance(new BigDecimal("10000.00"));

        when(accountMapper.selectById(102L)).thenReturn(account);
        when(accountMapper.updateBalanceIncrease(eq(102L), eq(new BigDecimal("100.555")))).thenReturn(1);
        when(ledgerService.append(any())).thenReturn(999L);

        // 100.555 * 7.1234 = 716.293587 -> 716.29
        PostingReq req = new PostingReq()
                .setAccountId(102L)
                .setDirection("DEBIT")
                .setCategory("IN_CUSTOMER")
                .setCurrency("USD")
                .setExchangeRate(new BigDecimal("7.12340000"))
                .setOriginalAmount(new BigDecimal("100.555"));

        Long entryId = postingService.post(req);
        assertEquals(999L, entryId);

        ArgumentCaptor<LedgerEntryDO> captor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService).append(captor.capture());
        LedgerEntryDO entry = captor.getValue();
        assertEquals(new BigDecimal("716.29"), entry.getBaseAmount());
        assertEquals(new BigDecimal("7.12340000"), entry.getExchangeRate());
    }

    @Test
    @DisplayName("appendArEntry: 13号往来分录只读追加不触碰资金账户余额，只写入经营分录")
    void testAppendArEntry_noBalanceUpdate_onlyAppendsEntry() {
        when(ledgerService.append(any(LedgerEntryDO.class))).thenReturn(777L);

        PostingReq req = new PostingReq()
                .setAccountId(null) // 不动钱
                .setDirection("CREDIT")
                .setCategory("AR_RETURN_REVERSAL")
                .setPartyType("CUSTOMER")
                .setPartyId(2001L)
                .setCurrency("USD")
                .setExchangeRate(new BigDecimal("7.10000000"))
                .setOriginalAmount(new BigDecimal("200.00"))
                .setRefType("SALES_RETURN")
                .setRefId(5001L)
                .setRefItemId(6001L)
                .setRefNo("TH-20261019-001")
                .setRemark("退货冲减往来分录");

        Long entryId = postingService.appendArEntry(req);
        assertEquals(777L, entryId);

        // 绝不调用账户更新与查询
        verifyNoInteractions(accountMapper);

        ArgumentCaptor<LedgerEntryDO> captor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService).append(captor.capture());
        LedgerEntryDO entry = captor.getValue();
        assertNull(entry.getAccountId());
        assertEquals("AR_RETURN_REVERSAL", entry.getCategory());
        assertEquals("CREDIT", entry.getDirection());
        assertEquals(new BigDecimal("1420.00"), entry.getBaseAmount()); // 200 * 7.1 = 1420.00
    }

}
