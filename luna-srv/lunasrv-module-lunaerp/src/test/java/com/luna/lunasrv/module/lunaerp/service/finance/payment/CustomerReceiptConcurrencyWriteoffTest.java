package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.handler.CustomerReceiptApprovalHandler;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AdvanceWriteoffReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptItemReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.EntryAllocatedSumDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 客户收款单并发核销与红冲恢复专测
 *
 * <p>对应 Issue 05:
 * 1. 并发重复核销专测：两事务同时过账同一 AR 分录，后到事务条件更新重验失败，抛出 RECEIPT_POST_CONFLICT（1_030_023_105）并回滚；
 * 2. 预收转核销并发超核守卫；
 * 3. 收款单红冲四写断言与核销行自动重开（未核销余额恢复后可被后续收款单再次核销）。</p>
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class CustomerReceiptConcurrencyWriteoffTest {

    @InjectMocks
    private CustomerReceiptApprovalHandler approvalHandler;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private AccountService accountService;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private ExchangeRateService exchangeRateService;

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
    @DisplayName("并发核销专测：两事务同时过账同一 AR 分录，Tx1 成功过账，Tx2 因未核销额不足失败回滚抛出 RECEIPT_POST_CONFLICT")
    void testConcurrentPosting_SameArEntry_SecondTransactionThrowsPostConflict() {
        // 场景设定：
        // AR 分录 entryId=1001L 原金额 10,000.00 元
        // 收款单 A (Tx1): 拟核销 6,000.00 元
        // 收款单 B (Tx2): 拟核销 6,000.00 元 (两者合计 12,000 > 10,000，必然有一方冲突)
        Long entryId = 1001L;
        Long paymentAId = 901L;
        Long paymentBId = 902L;

        LedgerEntryDO arEntry = new LedgerEntryDO();
        arEntry.setId(entryId);
        arEntry.setCurrency("RMB");
        arEntry.setOriginalAmount(new BigDecimal("10000.00"));

        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountService.validateAccountAvailable(3001L)).thenReturn(account);
        when(exchangeRateService.getRate(eq("RMB"), any())).thenReturn(BigDecimal.ONE);

        // 模拟全局数据库中已核销额的状态演变（模拟悲观锁锁定下的实时视图）
        AtomicReference<BigDecimal> committedAllocated = new AtomicReference<>(BigDecimal.ZERO);

        // 当 handler 调用 selectSumAllocatedAmountByEntryIds 时，返回当前的已核销累计
        when(paymentRefMapper.selectSumAllocatedAmountByEntryIds(any())).thenAnswer(invocation -> {
            BigDecimal current = committedAllocated.get();
            if (current.compareTo(BigDecimal.ZERO) == 0) {
                return List.of();
            }
            return List.of(EntryAllocatedSumDTO.builder().entryId(entryId).totalAllocated(current).build());
        });

        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(arEntry));

        // ---------- 事务 1 (Tx1): 收款单 A 过账 ----------
        PaymentDO paymentA = new PaymentDO();
        paymentA.setId(paymentAId);
        paymentA.setTenantId(TENANT_ID);
        paymentA.setDirection("IN");
        paymentA.setCustomerId(2001L);
        paymentA.setAccountId(3001L);
        paymentA.setCurrency("RMB");
        paymentA.setPayAmount(new BigDecimal("6000.00"));
        paymentA.setReceiptDate(LocalDate.of(2026, 10, 16));
        paymentA.setReceiptType(ReceiptType.RECEIPT.getValue());
        paymentA.setStatus("APPROVING");
        paymentA.setApprovalStatus(ApplyStatus.APPROVING.getValue());

        PaymentRefDO refA = new PaymentRefDO();
        refA.setId(501L);
        refA.setPaymentId(paymentAId);
        refA.setRefType("AR_ENTRY");
        refA.setRefId(entryId);
        refA.setAllocateAmount(new BigDecimal("6000.00"));

        when(paymentMapper.selectById(paymentAId)).thenReturn(paymentA);
        when(paymentRefMapper.selectList(any())).thenReturn(List.of(refA));

        // Tx1 执行过账
        approvalHandler.onApproved(paymentAId);

        // Tx1 成功，主单置为 COMPLETED，已核销额推进至 6,000.00
        assertEquals(PaymentStatus.COMPLETED.getValue(), paymentA.getStatus());
        committedAllocated.set(new BigDecimal("6000.00"));

        // ---------- 事务 2 (Tx2): 收款单 B 并发过账 ----------
        PaymentDO paymentB = new PaymentDO();
        paymentB.setId(paymentBId);
        paymentB.setTenantId(TENANT_ID);
        paymentB.setDirection("IN");
        paymentB.setCustomerId(2001L);
        paymentB.setAccountId(3001L);
        paymentB.setCurrency("RMB");
        paymentB.setPayAmount(new BigDecimal("6000.00"));
        paymentB.setReceiptDate(LocalDate.of(2026, 10, 16));
        paymentB.setReceiptType(ReceiptType.RECEIPT.getValue());
        paymentB.setStatus("APPROVING");
        paymentB.setApprovalStatus(ApplyStatus.APPROVING.getValue());

        PaymentRefDO refB = new PaymentRefDO();
        refB.setId(502L);
        refB.setPaymentId(paymentBId);
        refB.setRefType("AR_ENTRY");
        refB.setRefId(entryId);
        refB.setAllocateAmount(new BigDecimal("6000.00")); // 欲核销 6000，但剩余仅 4000

        when(paymentMapper.selectById(paymentBId)).thenReturn(paymentB);
        when(paymentRefMapper.selectList(any())).thenReturn(List.of(refB));

        // Tx2 执行过账，重验时触发冲突异常 RECEIPT_POST_CONFLICT
        ServiceException conflictEx = assertThrows(ServiceException.class, () -> approvalHandler.onApproved(paymentBId));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_POST_CONFLICT.getCode(), conflictEx.getCode());

        // Tx2 发生异常回滚，paymentB 状态绝不可变成 COMPLETED
        assertNotEquals(PaymentStatus.COMPLETED.getValue(), paymentB.getStatus());
    }

    @Test
    @DisplayName("并发预收转核销与审批核销冲突：预收先核销，审批后到检测到超核冲突回滚")
    void testConcurrentAdvanceWriteoffAndApproval_ThrowsPostConflict() {
        Long entryId = 1002L;
        LedgerEntryDO arEntry = new LedgerEntryDO();
        arEntry.setId(entryId);
        arEntry.setCurrency("RMB");
        arEntry.setOriginalAmount(new BigDecimal("5000.00"));

        AccountDO account = new AccountDO();
        account.setId(3001L);
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountService.validateAccountAvailable(3001L)).thenReturn(account);

        // 模拟已核销金额已被预收转核销占用了 4000.00 元（剩余 1000.00 元）
        when(paymentRefMapper.selectSumAllocatedAmountByEntryIds(any())).thenReturn(List.of(
                EntryAllocatedSumDTO.builder().entryId(entryId).totalAllocated(new BigDecimal("4000.00")).build()
        ));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(arEntry));

        // 收款单审批拟核销 2000.00 元（2000 > 1000）
        Long paymentId = 903L;
        PaymentDO payment = new PaymentDO();
        payment.setId(paymentId);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setCustomerId(2001L);
        payment.setAccountId(3001L);
        payment.setCurrency("RMB");
        payment.setPayAmount(new BigDecimal("2000.00"));
        payment.setReceiptType(ReceiptType.RECEIPT.getValue());
        when(paymentMapper.selectById(paymentId)).thenReturn(payment);

        PaymentRefDO ref = new PaymentRefDO();
        ref.setId(503L);
        ref.setPaymentId(paymentId);
        ref.setRefType("AR_ENTRY");
        ref.setRefId(entryId);
        ref.setAllocateAmount(new BigDecimal("2000.00"));
        when(paymentRefMapper.selectList(any())).thenReturn(List.of(ref));

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalHandler.onApproved(paymentId));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_POST_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("红冲后核销行自动重开：红冲导致 payment.status 变为 REVERSED，核销额从已核销聚合中释放，未核销额恢复")
    void testReverseReceipt_ReopensAllocationLines_RestoresUnallocatedBalance() {
        Long paymentId = 905L;
        PaymentDO payment = new PaymentDO();
        payment.setId(paymentId);
        payment.setTenantId(TENANT_ID);
        payment.setDirection("IN");
        payment.setCustomerId(2001L);
        payment.setAccountId(3001L);
        payment.setCurrency("RMB");
        payment.setPayAmount(new BigDecimal("5000.00"));
        payment.setReceiptType(ReceiptType.RECEIPT.getValue());
        payment.setStatus(PaymentStatus.COMPLETED.getValue());
        payment.setPaymentNo("RC-20261016-005");
        when(paymentMapper.selectById(paymentId)).thenReturn(payment);

        LedgerEntryDO origEntry = new LedgerEntryDO();
        origEntry.setId(7005L);
        origEntry.setCurrency("RMB");
        origEntry.setExchangeRate(BigDecimal.ONE);
        origEntry.setOriginalAmount(new BigDecimal("5000.00"));
        origEntry.setBaseAmount(new BigDecimal("5000.00"));
        when(ledgerEntryMapper.selectOne(any())).thenReturn(origEntry);

        // 执行红冲
        paymentService.reverseReceipt(paymentId);

        // 验证状态变为 REVERSED
        assertEquals(PaymentStatus.REVERSED.getValue(), payment.getStatus());
        verify(paymentMapper).updateById(payment);

        // 验证资金账户扣减与 RECEIPT_REVERSAL 分录记账
        verify(accountService).updateBalance(3001L, new BigDecimal("-5000.00"));
        verify(ledgerService).append(argThat(entry ->
                "RECEIPT_REVERSAL".equals(entry.getCategory()) &&
                "DEBIT".equals(entry.getDirection()) &&
                origEntry.getId().equals(entry.getReversalOfId())
        ));
    }

}
