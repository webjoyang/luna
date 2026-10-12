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

        // 2. 经营分录入账
        ArgumentCaptor<LedgerEntryDO> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntryDO.class);
        verify(ledgerService).append(ledgerCaptor.capture());
        LedgerEntryDO postedEntry = ledgerCaptor.getValue();
        assertEquals("RECEIPT", postedEntry.getCategory());
        assertEquals("CREDIT", postedEntry.getDirection());
        assertEquals(new BigDecimal("1000.00"), postedEntry.getOriginalAmount());
        assertEquals(new BigDecimal("7250.00"), postedEntry.getBaseAmount()); // 1000 * 7.25

        // 3. 核销行更新 baseAmount
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

}
