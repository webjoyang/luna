package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.EntryAllocatedSumDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.ReceiptType;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerReceiptApprovalHandler} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class CustomerReceiptApprovalHandlerTest {

    @InjectMocks
    private CustomerReceiptApprovalHandler handler;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private AccountService accountService;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper ledgerEntryMapper;

    private MockedStatic<SecurityFrameworkUtils> mockedSecurity;

    private static final Long TENANT_ID = 1L;
    private static final Long LOGIN_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        mockedSecurity = mockStatic(SecurityFrameworkUtils.class);
        mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);

        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentRefDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LedgerEntryDO.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("supports 仅支持 CUSTOMER_RECEIPT")
    void testSupports() {
        assertTrue(handler.supports("CUSTOMER_RECEIPT"));
        assertTrue(handler.supports("customer_receipt"));
        assertFalse(handler.supports("sales_contract"));
        assertFalse(handler.supports("payment"));
    }

    @Test
    @DisplayName("审批过账同事务四写：资金账户条件增加 + RECEIPT入账分录 + RECEIPT_WRITE_OFF分录 + 核销行状态回填 + COMPLETED")
    void testOnApproved_FourWrites_Success() {
        Long paymentId = 901L;
        PaymentDO payment = new PaymentDO();
        payment.setId(paymentId);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setCustomerId(2001L);
        payment.setAccountId(3001L);
        payment.setCurrency("RMB");
        payment.setPayAmount(new BigDecimal("5000.00"));
        payment.setReceiptDate(LocalDate.of(2026, 10, 16));
        payment.setReceiptType(ReceiptType.RECEIPT.getValue());
        payment.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        when(paymentMapper.selectById(paymentId)).thenReturn(payment);

        // 模拟账户可用
        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountService.validateAccountAvailable(3001L)).thenReturn(account);
        when(exchangeRateService.getRate("RMB", LocalDate.of(2026, 10, 16))).thenReturn(BigDecimal.ONE);

        // 模拟核销明细行（核销 5000）
        PaymentRefDO ref = new PaymentRefDO();
        ref.setId(501L);
        ref.setPaymentId(paymentId);
        ref.setRefType("AR_ENTRY");
        ref.setRefId(101L);
        ref.setAllocateAmount(new BigDecimal("5000.00"));
        when(paymentRefMapper.selectList(any())).thenReturn(List.of(ref));

        LedgerEntryDO origEntry = new LedgerEntryDO();
        origEntry.setId(101L);
        origEntry.setCurrency("RMB");
        origEntry.setOriginalAmount(new BigDecimal("10000.00"));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(origEntry));

        // 分录 append 模拟
        when(ledgerService.append(any(LedgerEntryDO.class))).thenReturn(7001L, 7002L);

        // 执行过账副作用
        handler.onApproved(paymentId);

        // 1. 验证账户余额条件更新 (current_balance += 5000)
        verify(accountService).updateBalance(3001L, new BigDecimal("5000.00"));

        // 2. 验证分录账追加：一条 RECEIPT 入账 + 一条 RECEIPT_WRITE_OFF 核销
        ArgumentCaptor<LedgerEntryDO> entryCaptor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService, times(2)).append(entryCaptor.capture());
        List<LedgerEntryDO> entries = entryCaptor.getAllValues();

        LedgerEntryDO receiptEntry = entries.get(0);
        assertEquals("RECEIPT", receiptEntry.getCategory());
        assertEquals("CREDIT", receiptEntry.getDirection());
        assertEquals(3001L, receiptEntry.getAccountId());
        assertEquals(new BigDecimal("5000.00"), receiptEntry.getOriginalAmount());

        LedgerEntryDO writeOffEntry = entries.get(1);
        assertEquals("RECEIPT_WRITE_OFF", writeOffEntry.getCategory());
        assertEquals("CREDIT", writeOffEntry.getDirection());
        assertNull(writeOffEntry.getAccountId()); // 核销不影响资金账户流水
        assertEquals(new BigDecimal("5000.00"), writeOffEntry.getOriginalAmount());
        assertEquals(101L, writeOffEntry.getRefItemId()); // 回指 AR_INC 分录

        // 3. 验证核销行回填 ledgerEntryId 与 baseAmount
        verify(paymentRefMapper).updateById(any(PaymentRefDO.class));

        // 4. 验证主单状态更新为 COMPLETED，approvalStatus 为 APPROVED
        ArgumentCaptor<PaymentDO> paymentCaptor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).updateById(paymentCaptor.capture());
        PaymentDO updatedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.COMPLETED.getValue(), updatedPayment.getStatus());
        assertEquals(ApplyStatus.APPROVED.getValue(), updatedPayment.getApprovalStatus());
    }

    @Test
    @DisplayName("审批过账预收款分支：receiptType=ADVANCE 追加 ADVANCE 入账分录")
    void testOnApproved_AdvanceType_WritesAdvanceCategory() {
        Long paymentId = 902L;
        PaymentDO payment = new PaymentDO();
        payment.setId(paymentId);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setCustomerId(2001L);
        payment.setAccountId(3001L);
        payment.setCurrency("RMB");
        payment.setPayAmount(new BigDecimal("3000.00"));
        payment.setReceiptDate(LocalDate.of(2026, 10, 16));
        payment.setReceiptType(ReceiptType.ADVANCE.getValue());
        payment.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        when(paymentMapper.selectById(paymentId)).thenReturn(payment);

        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountService.validateAccountAvailable(3001L)).thenReturn(account);
        when(exchangeRateService.getRate("RMB", LocalDate.of(2026, 10, 16))).thenReturn(BigDecimal.ONE);
        when(paymentRefMapper.selectList(any())).thenReturn(List.of()); // 预收无核销明细
        when(ledgerService.append(any(LedgerEntryDO.class))).thenReturn(7003L);

        handler.onApproved(paymentId);

        ArgumentCaptor<LedgerEntryDO> entryCaptor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService).append(entryCaptor.capture());
        LedgerEntryDO advEntry = entryCaptor.getValue();
        assertEquals("ADVANCE", advEntry.getCategory());
        assertEquals(new BigDecimal("3000.00"), advEntry.getOriginalAmount());
    }

    @Test
    @DisplayName("审批过账并发守卫：并发超额核销抛出 RECEIPT_POST_CONFLICT")
    void testOnApproved_PostConflict_Throws() {
        Long paymentId = 903L;
        PaymentDO payment = new PaymentDO();
        payment.setId(paymentId);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setCustomerId(2001L);
        payment.setAccountId(3001L);
        payment.setCurrency("RMB");
        payment.setPayAmount(new BigDecimal("5000.00"));
        payment.setReceiptDate(LocalDate.of(2026, 10, 16));
        payment.setReceiptType(ReceiptType.RECEIPT.getValue());
        when(paymentMapper.selectById(paymentId)).thenReturn(payment);

        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountService.validateAccountAvailable(3001L)).thenReturn(account);

        PaymentRefDO ref = new PaymentRefDO();
        ref.setId(502L);
        ref.setPaymentId(paymentId);
        ref.setRefType("AR_ENTRY");
        ref.setRefId(102L);
        ref.setAllocateAmount(new BigDecimal("5000.00")); // 欲核销 5000
        when(paymentRefMapper.selectList(any())).thenReturn(List.of(ref));

        // 模拟 AR 分录已被其他交易并发核销，仅余 2000
        LedgerEntryDO origEntry = new LedgerEntryDO();
        origEntry.setId(102L);
        origEntry.setCurrency("RMB");
        origEntry.setOriginalAmount(new BigDecimal("6000.00"));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(origEntry));
        when(paymentRefMapper.selectSumAllocatedAmountByEntryIds(any())).thenReturn(List.of(
                EntryAllocatedSumDTO.builder().entryId(102L).totalAllocated(new BigDecimal("4000.00")).build()
        ));

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.onApproved(paymentId));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_POST_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审批过账并发守卫：审批中途发货单被红冲（AR_INC_REVERSAL）导致未核销余额不足，抛出 RECEIPT_POST_CONFLICT")
    void testOnApproved_PostConflictDueToRedReversal_Throws() {
        Long paymentId = 904L;
        PaymentDO payment = new PaymentDO();
        payment.setId(paymentId);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setCustomerId(2001L);
        payment.setAccountId(3001L);
        payment.setCurrency("RMB");
        payment.setPayAmount(new BigDecimal("5000.00"));
        payment.setReceiptDate(LocalDate.of(2026, 10, 16));
        payment.setReceiptType(ReceiptType.RECEIPT.getValue());
        when(paymentMapper.selectById(paymentId)).thenReturn(payment);

        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountService.validateAccountAvailable(3001L)).thenReturn(account);

        PaymentRefDO ref = new PaymentRefDO();
        ref.setId(503L);
        ref.setPaymentId(paymentId);
        ref.setRefType("AR_ENTRY");
        ref.setRefId(103L);
        ref.setAllocateAmount(new BigDecimal("5000.00"));
        when(paymentRefMapper.selectList(any())).thenReturn(List.of(ref));

        // 原单 6000，中途被红冲 2000，剩余 4000 < 待核销 5000
        LedgerEntryDO origEntry = new LedgerEntryDO();
        origEntry.setId(103L);
        origEntry.setCurrency("RMB");
        origEntry.setOriginalAmount(new BigDecimal("6000.00"));

        LedgerEntryDO revEntry = new LedgerEntryDO();
        revEntry.setId(104L);
        revEntry.setCategory("AR_INC_REVERSAL");
        revEntry.setReversalOfId(103L);
        revEntry.setOriginalAmount(new BigDecimal("2000.00"));

        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(origEntry), List.of(revEntry), List.of());
        when(paymentRefMapper.selectSumAllocatedAmountByEntryIds(any())).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.onApproved(paymentId));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_POST_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审批过账守卫：账户已停用或不可用抛出异常，触发回滚")
    void testOnApproved_AccountDisabled_Throws() {
        Long paymentId = 901L;
        PaymentDO payment = new PaymentDO();
        payment.setId(paymentId);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setCustomerId(2001L);
        payment.setAccountId(3001L);
        payment.setCurrency("RMB");
        payment.setPayAmount(new BigDecimal("5000.00"));
        payment.setReceiptDate(LocalDate.of(2026, 10, 16));
        payment.setReceiptType(ReceiptType.RECEIPT.getValue());
        when(paymentMapper.selectById(paymentId)).thenReturn(payment);

        when(accountService.validateAccountAvailable(3001L))
                .thenThrow(new ServiceException(FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode(), "资金账户已停用"));

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.onApproved(paymentId));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode(), ex.getCode());

        // 确保没有执行后续写动作
        verify(accountService, never()).updateBalance(any(), any());
        verify(ledgerService, never()).append(any());
    }

}
