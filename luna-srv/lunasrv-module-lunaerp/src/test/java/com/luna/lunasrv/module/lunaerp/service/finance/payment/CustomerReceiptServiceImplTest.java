package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
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
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerReceiptServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CustomerReceiptServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private AccountService accountService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private ApprovalInstanceService approvalInstanceService;

    @InjectMocks
    private CustomerReceiptServiceImpl customerReceiptService;

    @Test
    @DisplayName("创建预收款草稿成功并生成RC单号")
    void testCreateAdvanceReceiptSuccess() {
        CustomerDO customer = new CustomerDO();
        customer.setId(1001L);
        customer.setCompanyName("某客户");
        when(customerMapper.selectById(1001L)).thenReturn(customer);

        AccountDO account = AccountDO.builder().id(2001L).accountName("工行基本户").status("ENABLE").build();
        when(accountService.getAccount(2001L)).thenReturn(account);

        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(paymentMapper.selectByPaymentNoIncludingDeleted(any())).thenReturn(null);

        ReceiptSaveReqVO reqVO = ReceiptSaveReqVO.builder()
                .customerId(1001L)
                .receiptType("ADVANCE")
                .currency("CNY")
                .receiptDate(LocalDate.of(2026, 10, 12))
                .accountId(2001L)
                .payAmount(new BigDecimal("50000.00"))
                .handlerName("李四")
                .build();

        doAnswer(inv -> {
            PaymentDO p = inv.getArgument(0);
            p.setId(999L);
            return 1;
        }).when(paymentMapper).insert(any(PaymentDO.class));

        Long receiptId = customerReceiptService.createReceipt(reqVO);
        assertNotNull(receiptId);
        assertEquals(999L, receiptId);

        ArgumentCaptor<PaymentDO> captor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).insert(captor.capture());
        PaymentDO saved = captor.getValue();
        assertTrue(saved.getPaymentNo().startsWith("RC-"));
        assertEquals("IN", saved.getDirection());
        assertEquals("PENDING", saved.getStatus());
        assertEquals("PENDING", saved.getApprovalStatus());
        assertEquals(new BigDecimal("50000.00"), saved.getPayAmount());
    }

    @Test
    @DisplayName("创建货款收款时币种不匹配应拒绝")
    void testCreateReceiptCurrencyMismatch() {
        CustomerDO customer = new CustomerDO();
        customer.setId(1001L);
        customer.setCompanyName("某客户");
        when(customerMapper.selectById(1001L)).thenReturn(customer);

        AccountDO account = AccountDO.builder().id(2001L).accountName("工行基本户").status("ENABLE").build();
        when(accountService.getAccount(2001L)).thenReturn(account);

        LedgerEntryDO entry = LedgerEntryDO.builder()
                .id(501L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("USD") // 外币分录
                .originalAmount(new BigDecimal("1000.00"))
                .build();
        when(ledgerEntryMapper.selectById(501L)).thenReturn(entry);

        ReceiptRefSaveVO item = ReceiptRefSaveVO.builder()
                .ledgerEntryId(501L)
                .allocateAmount(new BigDecimal("1000.00"))
                .build();

        ReceiptSaveReqVO reqVO = ReceiptSaveReqVO.builder()
                .customerId(1001L)
                .receiptType("RECEIPT")
                .currency("CNY") // 收款为人民币
                .receiptDate(LocalDate.of(2026, 10, 12))
                .accountId(2001L)
                .payAmount(new BigDecimal("1000.00"))
                .items(List.of(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> customerReceiptService.createReceipt(reqVO));
        assertEquals(RECEIPT_CURRENCY_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建货款收款时分配合计超过收款额应拒绝")
    void testCreateReceiptAllocateExceedPayAmount() {
        CustomerDO customer = new CustomerDO();
        customer.setId(1001L);
        customer.setCompanyName("某客户");
        when(customerMapper.selectById(1001L)).thenReturn(customer);

        AccountDO account = AccountDO.builder().id(2001L).accountName("工行基本户").status("ENABLE").build();
        when(accountService.getAccount(2001L)).thenReturn(account);

        LedgerEntryDO entry = LedgerEntryDO.builder()
                .id(501L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("CNY")
                .originalAmount(new BigDecimal("2000.00"))
                .build();
        when(ledgerEntryMapper.selectById(501L)).thenReturn(entry);

        ReceiptRefSaveVO item = ReceiptRefSaveVO.builder()
                .ledgerEntryId(501L)
                .allocateAmount(new BigDecimal("1500.00"))
                .build();

        ReceiptSaveReqVO reqVO = ReceiptSaveReqVO.builder()
                .customerId(1001L)
                .receiptType("RECEIPT")
                .currency("CNY")
                .receiptDate(LocalDate.of(2026, 10, 12))
                .accountId(2001L)
                .payAmount(new BigDecimal("1000.00")) // 收款额仅 1000，核销 1500
                .items(List.of(item))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> customerReceiptService.createReceipt(reqVO));
        assertEquals(RECEIPT_ALLOCATE_EXCEED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批流验证")
    void testSubmitReceipt() {
        PaymentDO payment = PaymentDO.builder()
                .id(100L)
                .direction("IN")
                .status("PENDING")
                .approvalStatus("PENDING")
                .customerId(1001L)
                .currency("CNY")
                .payAmount(new BigDecimal("1000.00"))
                .build();
        when(paymentMapper.selectById(100L)).thenReturn(payment);
        when(paymentRefMapper.selectListByPaymentId(100L)).thenReturn(Collections.emptyList());
        when(approvalInstanceService.submitInstance(any(ApprovalInstanceSubmitReqVO.class))).thenReturn(8888L);

        Long instanceId = customerReceiptService.submitReceipt(100L);
        assertEquals(8888L, instanceId);
        assertEquals("APPROVING", payment.getApprovalStatus());
        verify(paymentMapper).updateById(payment);
    }

    @Test
    @DisplayName("审批通过同事务四写断言")
    void testPostReceiptOnApprovalFourWrites() {
        PaymentDO payment = PaymentDO.builder()
                .id(100L)
                .paymentNo("RC-20261012-001")
                .direction("IN")
                .status("PENDING")
                .approvalStatus("APPROVING")
                .customerId(1001L)
                .accountId(2001L)
                .currency("USD")
                .payAmount(new BigDecimal("1000.00"))
                .receiptDate(LocalDate.of(2026, 10, 12))
                .receiptType("RECEIPT")
                .build();
        when(paymentMapper.selectById(100L)).thenReturn(payment);

        when(exchangeRateService.getRate("USD", LocalDate.of(2026, 10, 12)))
                .thenReturn(new BigDecimal("7.25000000"));

        PaymentRefDO ref = PaymentRefDO.builder()
                .id(1L)
                .paymentId(100L)
                .refType("AR_ENTRY")
                .refId(501L)
                .ledgerEntryId(501L)
                .entryNo("SH20261012001")
                .allocateAmount(new BigDecimal("1000.00"))
                .build();
        when(paymentRefMapper.selectListByPaymentId(100L)).thenReturn(List.of(ref));

        LedgerEntryDO arEntry = LedgerEntryDO.builder()
                .id(501L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("USD")
                .originalAmount(new BigDecimal("1000.00"))
                .build();
        when(ledgerEntryMapper.selectById(501L)).thenReturn(arEntry);
        when(ledgerEntryMapper.selectListByReversalOfId(501L)).thenReturn(Collections.emptyList());
        when(paymentRefMapper.selectListByRefTypeAndRefId("AR_ENTRY", 501L)).thenReturn(List.of(ref));

        // 执行过账
        customerReceiptService.postReceiptOnApproval(100L);

        // 1. 资金账户入账
        verify(accountService).updateBalance(2001L, new BigDecimal("1000.00"));

        // 2. 经营分录入账（包含 1 笔 RECEIPT 入账分录 + 1 笔 RECEIPT_WRITE_OFF 核销分录）
        ArgumentCaptor<LedgerEntryDO> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService, times(2)).append(ledgerCaptor.capture());
        List<LedgerEntryDO> postedEntries = ledgerCaptor.getAllValues();
        LedgerEntryDO receiptEntry = postedEntries.get(0);
        assertEquals("RECEIPT", receiptEntry.getCategory());
        assertEquals("CREDIT", receiptEntry.getDirection());
        assertEquals(new BigDecimal("1000.00"), receiptEntry.getOriginalAmount());
        assertEquals(new BigDecimal("7250.00"), receiptEntry.getBaseAmount()); // 1000 * 7.25

        LedgerEntryDO writeOffEntry = postedEntries.get(1);
        assertEquals("RECEIPT_WRITE_OFF", writeOffEntry.getCategory());
        assertEquals("CREDIT", writeOffEntry.getDirection());
        assertEquals(new BigDecimal("1000.00"), writeOffEntry.getOriginalAmount());
        assertEquals(new BigDecimal("7250.00"), writeOffEntry.getBaseAmount());
        assertEquals(501L, writeOffEntry.getReversalOfId());

        // 3. 核销行更新 baseAmount 与 ledgerEntryId
        verify(paymentRefMapper).updateById(ref);
        assertEquals(new BigDecimal("7250.00"), ref.getBaseAmount());

        // 4. 收款单变更为 COMPLETED / APPROVED
        verify(paymentMapper).updateById(payment);
        assertEquals("COMPLETED", payment.getStatus());
        assertEquals("APPROVED", payment.getApprovalStatus());
        assertEquals(new BigDecimal("7.25000000"), payment.getExchangeRate());
    }

    @Test
    @DisplayName("审批过账时若核销分录被并发消耗抛出 RECEIPT_POST_CONFLICT")
    void testPostReceiptOnApprovalConflict() {
        PaymentDO payment = PaymentDO.builder()
                .id(100L)
                .paymentNo("RC-20261012-001")
                .direction("IN")
                .status("PENDING")
                .approvalStatus("APPROVING")
                .customerId(1001L)
                .accountId(2001L)
                .currency("CNY")
                .payAmount(new BigDecimal("1000.00"))
                .receiptDate(LocalDate.of(2026, 10, 12))
                .receiptType("RECEIPT")
                .build();
        when(paymentMapper.selectById(100L)).thenReturn(payment);
        when(exchangeRateService.getRate("CNY", LocalDate.of(2026, 10, 12))).thenReturn(BigDecimal.ONE);

        PaymentRefDO ref = PaymentRefDO.builder()
                .id(1L)
                .paymentId(100L)
                .refType("AR_ENTRY")
                .ledgerEntryId(501L)
                .entryNo("SH20261012001")
                .allocateAmount(new BigDecimal("1000.00"))
                .build();
        when(paymentRefMapper.selectListByPaymentId(100L)).thenReturn(List.of(ref));

        // 原始金额仅 1000，但已有并发已完成收款单占用了 800，剩余仅 200
        LedgerEntryDO arEntry = LedgerEntryDO.builder()
                .id(501L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("CNY")
                .originalAmount(new BigDecimal("1000.00"))
                .build();
        when(ledgerEntryMapper.selectById(501L)).thenReturn(arEntry);
        when(ledgerEntryMapper.selectListByReversalOfId(501L)).thenReturn(Collections.emptyList());

        PaymentRefDO otherRef = PaymentRefDO.builder()
                .paymentId(200L)
                .allocateAmount(new BigDecimal("800.00"))
                .build();
        when(paymentRefMapper.selectListByRefTypeAndRefId("AR_ENTRY", 501L)).thenReturn(List.of(ref, otherRef));
        when(paymentMapper.selectById(200L)).thenReturn(PaymentDO.builder().id(200L).status("COMPLETED").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> customerReceiptService.postReceiptOnApproval(100L));
        assertEquals(RECEIPT_POST_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("FIFO 自动分配试算验证")
    void testAllocatePreviewFIFO() {
        LedgerEntryDO e1 = LedgerEntryDO.builder()
                .id(101L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("CNY")
                .category("AR_INC")
                .bizDate(LocalDate.of(2026, 10, 1))
                .originalAmount(new BigDecimal("400.00"))
                .remark("SH001")
                .build();

        LedgerEntryDO e2 = LedgerEntryDO.builder()
                .id(102L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("CNY")
                .category("AR_INC")
                .bizDate(LocalDate.of(2026, 10, 5))
                .originalAmount(new BigDecimal("800.00"))
                .remark("SH002")
                .build();

        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(e1, e2));
        when(ledgerEntryMapper.selectListByReversalOfId(any())).thenReturn(Collections.emptyList());
        when(paymentRefMapper.selectListByRefTypeAndRefId(eq("AR_ENTRY"), any())).thenReturn(Collections.emptyList());

        AllocatePreviewReqVO reqVO = AllocatePreviewReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .payAmount(new BigDecimal("700.00"))
                .build();

        AllocatePreviewRespVO preview = customerReceiptService.allocatePreview(reqVO);
        assertNotNull(preview);
        assertEquals(new BigDecimal("700.00"), preview.getTotalPayAmount());
        assertEquals(new BigDecimal("700.00"), preview.getTotalAllocated());
        assertEquals(0, BigDecimal.ZERO.compareTo(preview.getRemainingPayAmount()));
        assertEquals(2, preview.getItems().size());

        // e1 (400) 全部分配
        assertEquals(101L, preview.getItems().get(0).getLedgerEntryId());
        assertEquals(new BigDecimal("400.00"), preview.getItems().get(0).getAllocatedAmount());

        // e2 (800) 分配剩余 300
        assertEquals(102L, preview.getItems().get(1).getLedgerEntryId());
        assertEquals(new BigDecimal("300.00"), preview.getItems().get(1).getAllocatedAmount());
    }

    @Test
    @DisplayName("收款单红冲四写断言验证")
    void testReverseReceiptSuccess() {
        PaymentDO payment = PaymentDO.builder()
                .id(999L)
                .paymentNo("RC-20261012-001")
                .direction("IN")
                .customerId(1001L)
                .accountId(2001L)
                .payAmount(new BigDecimal("5000.00"))
                .currency("CNY")
                .exchangeRate(BigDecimal.ONE)
                .receiptType("RECEIPT")
                .status("COMPLETED")
                .approvalStatus("APPROVED")
                .build();
        when(paymentMapper.selectById(999L)).thenReturn(payment);
        when(ledgerEntryMapper.selectListByRef("PAYMENT", 999L))
                .thenReturn(List.of(LedgerEntryDO.builder().id(777L).build()));

        customerReceiptService.reverseReceipt(999L);

        // 1. 资金账户回扣
        verify(accountService).updateBalance(2001L, new BigDecimal("-5000.00"));

        // 2. 生成 RECEIPT_REVERSAL 反向分录
        ArgumentCaptor<LedgerEntryDO> entryCaptor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService).append(entryCaptor.capture());
        LedgerEntryDO revEntry = entryCaptor.getValue();
        assertNotNull(revEntry);
        assertEquals("RECEIPT_REVERSAL", revEntry.getCategory());
        assertEquals("DEBIT", revEntry.getDirection());
        assertEquals(777L, revEntry.getReversalOfId());
        assertEquals(0, new BigDecimal("5000.00").compareTo(revEntry.getOriginalAmount()));

        // 3. 收款单状态更新为 REVERSED
        assertEquals("REVERSED", payment.getStatus());
        verify(paymentMapper).updateById(payment);
    }

    @Test
    @DisplayName("收款单红冲幂等与状态守卫拒绝验证")
    void testReverseReceiptGuards() {
        // 1. 单据不存在
        when(paymentMapper.selectById(111L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> customerReceiptService.reverseReceipt(111L));
        assertEquals(RECEIPT_NOT_EXISTS.getCode(), ex1.getCode());

        // 2. 非 COMPLETED 状态（如草稿 PENDING）拒绝红冲
        PaymentDO pendingPayment = PaymentDO.builder().id(222L).direction("IN").status("PENDING").build();
        when(paymentMapper.selectById(222L)).thenReturn(pendingPayment);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> customerReceiptService.reverseReceipt(222L));
        assertEquals(RECEIPT_REVERSAL_NOT_ALLOW.getCode(), ex2.getCode());

        // 3. 已红冲状态（REVERSED）再次红冲（幂等硬拦截）
        PaymentDO reversedPayment = PaymentDO.builder().id(333L).direction("IN").status("REVERSED").build();
        when(paymentMapper.selectById(333L)).thenReturn(reversedPayment);
        ServiceException ex3 = assertThrows(ServiceException.class, () -> customerReceiptService.reverseReceipt(333L));
        assertEquals(RECEIPT_REVERSAL_NOT_ALLOW.getCode(), ex3.getCode());

        // 4. 预收款已发生转核销时拒绝红冲
        PaymentDO advPayment = PaymentDO.builder()
                .id(444L)
                .direction("IN")
                .receiptType("ADVANCE")
                .status("COMPLETED")
                .build();
        when(paymentMapper.selectById(444L)).thenReturn(advPayment);
        PaymentRefDO usedRef = PaymentRefDO.builder().paymentId(444L).allocateAmount(new BigDecimal("100.00")).build();
        when(paymentRefMapper.selectListByPaymentId(444L)).thenReturn(List.of(usedRef));
        ServiceException ex4 = assertThrows(ServiceException.class, () -> customerReceiptService.reverseReceipt(444L));
        assertEquals(RECEIPT_REVERSAL_NOT_ALLOW.getCode(), ex4.getCode());
    }

    @Test
    @DisplayName("客户预收转核销成功断言验证")
    void testAdvanceWriteoffSuccess() {
        PaymentDO advancePayment = PaymentDO.builder()
                .id(500L)
                .paymentNo("RC-20261012-099")
                .direction("IN")
                .customerId(1001L)
                .accountId(2001L)
                .payAmount(new BigDecimal("10000.00"))
                .prepayUsedAmount(BigDecimal.ZERO)
                .receiptType("ADVANCE")
                .currency("CNY")
                .status("COMPLETED")
                .build();
        when(paymentMapper.selectById(500L)).thenReturn(advancePayment);
        when(paymentRefMapper.selectListByPaymentId(500L)).thenReturn(Collections.emptyList());

        LedgerEntryDO arEntry = LedgerEntryDO.builder()
                .id(601L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("CNY")
                .category("AR_INC")
                .originalAmount(new BigDecimal("5000.00"))
                .build();
        when(ledgerEntryMapper.selectOne(any())).thenReturn(arEntry);
        when(ledgerEntryMapper.selectListByReversalOfId(601L)).thenReturn(Collections.emptyList());
        when(paymentRefMapper.selectListByRefTypeAndRefId("AR_ENTRY", 601L)).thenReturn(Collections.emptyList());
        when(exchangeRateService.getRate(eq("CNY"), any())).thenReturn(BigDecimal.ONE);

        AdvanceWriteoffReqVO reqVO = AdvanceWriteoffReqVO.builder()
                .paymentId(500L)
                .remark("预收抵扣发货款")
                .items(List.of(ReceiptRefSaveVO.builder()
                        .ledgerEntryId(601L)
                        .allocateAmount(new BigDecimal("3000.00"))
                        .entryNo("SH20261012001")
                        .build()))
                .build();

        customerReceiptService.advanceWriteoff(reqVO);

        // 1. 插入核销行
        verify(paymentRefMapper).insert(any(PaymentRefDO.class));

        // 2. 写入 ADVANCE_WRITE_OFF 分录
        ArgumentCaptor<LedgerEntryDO> entryCaptor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService).append(entryCaptor.capture());
        LedgerEntryDO writeoffEntry = entryCaptor.getValue();
        assertEquals("ADVANCE_WRITE_OFF", writeoffEntry.getCategory());
        assertEquals("CREDIT", writeoffEntry.getDirection());
        assertEquals(0, new BigDecimal("3000.00").compareTo(writeoffEntry.getOriginalAmount()));

        // 3. 更新原预收款单 prepayUsedAmount
        assertEquals(0, new BigDecimal("3000.00").compareTo(advancePayment.getPrepayUsedAmount()));
        verify(paymentMapper).updateById(advancePayment);
    }

    @Test
    @DisplayName("预收转核销守卫：超出预收余额与币种不一致拦截")
    void testAdvanceWriteoffGuards() {
        PaymentDO advancePayment = PaymentDO.builder()
                .id(500L)
                .paymentNo("RC-20261012-099")
                .direction("IN")
                .customerId(1001L)
                .payAmount(new BigDecimal("2000.00"))
                .receiptType("ADVANCE")
                .currency("CNY")
                .status("COMPLETED")
                .build();
        when(paymentMapper.selectById(500L)).thenReturn(advancePayment);
        when(paymentRefMapper.selectListByPaymentId(500L)).thenReturn(Collections.emptyList());

        // 1. 超出预收可用余额拦截（2000 可用，拟分配 3000）
        AdvanceWriteoffReqVO exceedReq = AdvanceWriteoffReqVO.builder()
                .paymentId(500L)
                .items(List.of(ReceiptRefSaveVO.builder().ledgerEntryId(601L).allocateAmount(new BigDecimal("3000.00")).build()))
                .build();
        ServiceException ex1 = assertThrows(ServiceException.class, () -> customerReceiptService.advanceWriteoff(exceedReq));
        assertEquals(RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH.getCode(), ex1.getCode());

        // 2. 币种不匹配拦截（预收 CNY，分录 USD）
        when(exchangeRateService.getRate(eq("CNY"), any())).thenReturn(BigDecimal.ONE);
        LedgerEntryDO usdEntry = LedgerEntryDO.builder()
                .id(602L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("USD")
                .category("AR_INC")
                .originalAmount(new BigDecimal("1000.00"))
                .build();
        when(ledgerEntryMapper.selectOne(any())).thenReturn(usdEntry);

        AdvanceWriteoffReqVO mismatchReq = AdvanceWriteoffReqVO.builder()
                .paymentId(500L)
                .items(List.of(ReceiptRefSaveVO.builder().ledgerEntryId(602L).allocateAmount(new BigDecimal("1000.00")).build()))
                .build();
        ServiceException ex2 = assertThrows(ServiceException.class, () -> customerReceiptService.advanceWriteoff(mismatchReq));
        assertEquals(RECEIPT_CURRENCY_MISMATCH.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("并发重复核销专测：两事务过账同一分录，余额不足抛 RECEIPT_POST_CONFLICT")
    void testConcurrentAllocationConflict() {
        PaymentDO payment = PaymentDO.builder()
                .id(100L)
                .paymentNo("RC-20261012-001")
                .direction("IN")
                .customerId(1001L)
                .accountId(2001L)
                .payAmount(new BigDecimal("2000.00"))
                .currency("CNY")
                .receiptDate(LocalDate.of(2026, 10, 12))
                .status("PENDING")
                .build();
        when(paymentMapper.selectById(100L)).thenReturn(payment);
        when(exchangeRateService.getRate(eq("CNY"), any())).thenReturn(BigDecimal.ONE);

        // 拟核销 2000.00
        PaymentRefDO ref = PaymentRefDO.builder()
                .paymentId(100L)
                .ledgerEntryId(555L)
                .allocateAmount(new BigDecimal("2000.00"))
                .entryNo("SH001")
                .build();
        when(paymentRefMapper.selectListByPaymentId(100L)).thenReturn(List.of(ref));

        // 目标分录原始金额 2000.00，但在加锁查询后发现另一事务已核销 1500.00，剩余可用仅 500.00
        LedgerEntryDO targetEntry = LedgerEntryDO.builder()
                .id(555L)
                .partyType("CUSTOMER")
                .partyId(1001L)
                .currency("CNY")
                .category("AR_INC")
                .originalAmount(new BigDecimal("2000.00"))
                .build();
        when(ledgerEntryMapper.selectOne(any())).thenReturn(targetEntry);
        when(ledgerEntryMapper.selectListByReversalOfId(555L)).thenReturn(Collections.emptyList());

        // 模拟其他已过账付款单占用了 1500.00
        PaymentRefDO otherRef = PaymentRefDO.builder().paymentId(888L).allocateAmount(new BigDecimal("1500.00")).build();
        when(paymentRefMapper.selectListByRefTypeAndRefId("AR_ENTRY", 555L)).thenReturn(List.of(otherRef));
        PaymentDO otherPayment = PaymentDO.builder().id(888L).status("COMPLETED").build();
        when(paymentMapper.selectById(888L)).thenReturn(otherPayment);

        // 执行过账，断言抛出并发冲突 RECEIPT_POST_CONFLICT
        ServiceException ex = assertThrows(ServiceException.class, () -> customerReceiptService.postReceiptOnApproval(100L));
        assertEquals(RECEIPT_POST_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除收款单：仅 PENDING 状态允许删除进回收站")
    void testDeleteReceipt_pendingSuccess() {
        PaymentDO payment = PaymentDO.builder()
                .id(901L)
                .direction("IN")
                .approvalStatus("PENDING")
                .build();
        when(paymentMapper.selectById(901L)).thenReturn(payment);

        assertDoesNotThrow(() -> customerReceiptService.deleteReceipt(901L));

        verify(paymentRefMapper).delete(any());
        verify(paymentMapper).deleteById(901L);
    }

    @Test
    @DisplayName("删除收款单守卫：非 PENDING 状态（如 COMPLETED）拒绝删除抛 RECEIPT_STATUS_NOT_ALLOW")
    void testDeleteReceipt_nonPendingThrows() {
        PaymentDO payment = PaymentDO.builder()
                .id(902L)
                .direction("IN")
                .approvalStatus("APPROVED")
                .status("COMPLETED")
                .build();
        when(paymentMapper.selectById(902L)).thenReturn(payment);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerReceiptService.deleteReceipt(902L));
        assertEquals(RECEIPT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除收款单守卫：单据不存在或方向非 IN 抛 RECEIPT_NOT_EXISTS")
    void testDeleteReceipt_notExistsThrows() {
        when(paymentMapper.selectById(903L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerReceiptService.deleteReceipt(903L));
        assertEquals(RECEIPT_NOT_EXISTS.getCode(), ex.getCode());
    }

}
