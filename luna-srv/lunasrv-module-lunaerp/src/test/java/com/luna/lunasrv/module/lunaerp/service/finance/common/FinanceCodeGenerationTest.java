package com.luna.lunasrv.module.lunaerp.service.finance.common;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceItemSaveVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.service.finance.invoice.InvoiceServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.CustomerReceiptServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.INVOICE_STATUS_NOT_ALLOW;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.RECEIPT_STATUS_NOT_ALLOW;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 财务单号生成专项测试：
 * 验证 CS (客户对账单) / RC (收款单) / IA (发票申请单) 的
 * 1. 编号格式 (CS{yyyy}0001, RC-{yyyyMMdd}-001, IA{yyyy}0001)
 * 2. 软删行扫描 (软删行流水不释放、继续向上递增)
 * 3. 撞号重试 (候选碰撞时重试顺延)
 * 4. 手填撞号报错 (手动输入已存在单号包括软删行时拒绝)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FinanceCodeGenerationTest 财务单据取号与冲突防御测试")
public class FinanceCodeGenerationTest {

    @BeforeAll
    static void initMyBatisPlus() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerStatementItemDO.class);
        TableInfoHelper.initTableInfo(assistant, LedgerEntryDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentDO.class);
        TableInfoHelper.initTableInfo(assistant, InvoiceApplicationDO.class);
    }

    private CustomerDO createCustomer(Long id, String name) {
        CustomerDO c = new CustomerDO();
        c.setId(id);
        c.setCompanyName(name);
        c.setStatus("ACTIVE");
        c.setDeleted(false);
        return c;
    }

    private ShipmentDO createShipment(Long id, Long customerId, BigDecimal totalAmount) {
        ShipmentDO s = new ShipmentDO();
        s.setId(id);
        s.setCustomerId(customerId);
        s.setTotalAmount(totalAmount);
        s.setCurrency("CNY");
        s.setStatus("COMPLETED");
        s.setDeleted(false);
        return s;
    }

    // --- CustomerStatement mocks ---
    @Mock
    private CustomerStatementMapper customerStatementMapper;
    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private LedgerEntryMapper ledgerEntryMapper;
    @Mock
    private ShipmentMapper shipmentMapper;

    @InjectMocks
    private CustomerStatementServiceImpl statementService;

    // --- CustomerReceipt mocks ---
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentRefMapper paymentRefMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private AccountService accountService;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private CustomerReceiptServiceImpl receiptService;

    // --- InvoiceApplication mocks ---
    @Mock
    private InvoiceApplicationMapper invoiceApplicationMapper;
    @Mock
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    // ==================== 1. 对账单 (CS) 取号测试 ====================

    @Test
    @DisplayName("CS取号：软删行流水扫描，跳过软删最大号并生成 CS{yyyy}0006")
    void testStatementNo_FormatAndSoftDeleteScan() {
        CustomerDO customer = createCustomer(101L, "测试客户A");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String prefix = "CS" + year;

        CustomerStatementDO softDeletedDo = CustomerStatementDO.builder()
                .id(50L)
                .statementNo(prefix + "0005")
                .customerId(101L)
                .build();
        softDeletedDo.setDeleted(true);

        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(softDeletedDo));
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(prefix + "0006"))
                .thenReturn(null);
        when(customerStatementItemMapper.selectActiveOccupiedLedgerEntryIds())
                .thenReturn(Collections.emptyList());

        LedgerEntryDO entry = LedgerEntryDO.builder()
                .id(1001L)
                .partyType("CUSTOMER")
                .partyId(101L)
                .direction("DEBIT")
                .category("AR_INC")
                .originalAmount(new BigDecimal("1000.00"))
                .currency("CNY")
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(new BigDecimal("1000.00"))
                .bizDate(LocalDate.now().minusDays(10))
                .build();
        when(ledgerEntryMapper.selectById(1001L)).thenReturn(entry);

        CustomerStatementCreateReqVO req = CustomerStatementCreateReqVO.builder()
                .customerId(101L)
                .currency("CNY")
                .periodStart(LocalDate.now().minusDays(30))
                .periodEnd(LocalDate.now())
                .selectedEntryIds(List.of(1001L))
                .build();

        statementService.createStatement(req);

        ArgumentCaptor<CustomerStatementDO> captor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).insert(captor.capture());
        assertEquals(prefix + "0006", captor.getValue().getStatementNo(), "对账单流水号应扫描软删记录并递增到 0006");
    }

    @Test
    @DisplayName("CS取号：撞号重试机制生效，碰撞后顺延取下一个唯一单号")
    void testStatementNo_CollisionRetry() {
        CustomerDO customer = createCustomer(101L, "测试客户A");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String prefix = "CS" + year;

        CustomerStatementDO prevDo = CustomerStatementDO.builder()
                .id(1L).statementNo(prefix + "0001").customerId(101L).build();
        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(prevDo));

        // 第一次顺延 0002 产生并发碰撞 (non-null)
        CustomerStatementDO collidedDo = CustomerStatementDO.builder()
                .id(2L).statementNo(prefix + "0002").customerId(101L).build();
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(prefix + "0002"))
                .thenReturn(collidedDo);
        // 第二次重试 0003 无碰撞 (null)
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(prefix + "0003"))
                .thenReturn(null);
        when(customerStatementItemMapper.selectActiveOccupiedLedgerEntryIds())
                .thenReturn(Collections.emptyList());

        LedgerEntryDO entry = LedgerEntryDO.builder()
                .id(1001L)
                .partyType("CUSTOMER")
                .partyId(101L)
                .direction("DEBIT")
                .category("AR_INC")
                .originalAmount(new BigDecimal("1000.00"))
                .currency("CNY")
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(new BigDecimal("1000.00"))
                .bizDate(LocalDate.now().minusDays(10))
                .build();
        when(ledgerEntryMapper.selectById(1001L)).thenReturn(entry);

        CustomerStatementCreateReqVO req = CustomerStatementCreateReqVO.builder()
                .customerId(101L)
                .currency("CNY")
                .periodStart(LocalDate.now().minusDays(30))
                .periodEnd(LocalDate.now())
                .selectedEntryIds(List.of(1001L))
                .build();

        statementService.createStatement(req);

        ArgumentCaptor<CustomerStatementDO> captor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).insert(captor.capture());
        assertEquals(prefix + "0003", captor.getValue().getStatementNo(), "碰撞后重试应顺延至 0003");
    }

    @Test
    @DisplayName("CS取号：手填已存在单号（含软删行）报错拒绝")
    void testStatementNo_ManualDuplicateRejected() {
        CustomerDO customer = createCustomer(101L, "测试客户A");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        String manualNo = "CS20268888";
        CustomerStatementDO existing = CustomerStatementDO.builder()
                .id(99L).statementNo(manualNo).customerId(101L).build();
        existing.setDeleted(true);

        when(customerStatementMapper.selectByStatementNoIncludingDeleted(manualNo)).thenReturn(existing);

        CustomerStatementCreateReqVO req = CustomerStatementCreateReqVO.builder()
                .statementNo(manualNo)
                .customerId(101L)
                .currency("CNY")
                .periodStart(LocalDate.now().minusDays(30))
                .periodEnd(LocalDate.now())
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> statementService.createStatement(req));
        assertEquals(400, ex.getCode());
    }

    // ==================== 2. 收款单 (RC) 取号测试 ====================

    @Test
    @DisplayName("RC取号：软删行流水扫描，生成 RC-{yyyyMMdd}-006")
    void testReceiptNo_FormatAndSoftDeleteScan() {
        CustomerDO customer = createCustomer(201L, "测试客户B");
        when(customerMapper.selectById(201L)).thenReturn(customer);
        AccountDO account = AccountDO.builder().id(1L).currency("CNY").status("ENABLE").build();
        when(accountService.getAccount(1L)).thenReturn(account);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RC-" + dateStr + "-";

        PaymentDO softDeleted = PaymentDO.builder()
                .id(10L).paymentNo(prefix + "005").customerId(201L).build();
        softDeleted.setDeleted(true);

        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(softDeleted));
        when(paymentMapper.selectByPaymentNoIncludingDeleted(prefix + "006"))
                .thenReturn(null);

        ReceiptSaveReqVO req = ReceiptSaveReqVO.builder()
                .customerId(201L)
                .accountId(1L)
                .payAmount(new BigDecimal("1000.00"))
                .currency("CNY")
                .receiptDate(LocalDate.now())
                .receiptType("RECEIPT")
                .build();

        receiptService.createReceipt(req);

        ArgumentCaptor<PaymentDO> captor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).insert(captor.capture());
        assertEquals(prefix + "006", captor.getValue().getPaymentNo(), "收款单流水应扫描软删并顺延至 006");
    }

    @Test
    @DisplayName("RC取号：撞号重试机制生效，碰撞后顺延")
    void testReceiptNo_CollisionRetry() {
        CustomerDO customer = createCustomer(201L, "测试客户B");
        when(customerMapper.selectById(201L)).thenReturn(customer);
        AccountDO account = AccountDO.builder().id(1L).currency("CNY").status("ENABLE").build();
        when(accountService.getAccount(1L)).thenReturn(account);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RC-" + dateStr + "-";

        PaymentDO prev = PaymentDO.builder().id(1L).paymentNo(prefix + "001").customerId(201L).build();
        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(prev));

        // 002 产生并发碰撞
        PaymentDO collided = PaymentDO.builder().id(2L).paymentNo(prefix + "002").customerId(201L).build();
        when(paymentMapper.selectByPaymentNoIncludingDeleted(prefix + "002")).thenReturn(collided);
        // 003 成功
        when(paymentMapper.selectByPaymentNoIncludingDeleted(prefix + "003")).thenReturn(null);

        ReceiptSaveReqVO req = ReceiptSaveReqVO.builder()
                .customerId(201L)
                .accountId(1L)
                .payAmount(new BigDecimal("1000.00"))
                .currency("CNY")
                .receiptDate(LocalDate.now())
                .receiptType("RECEIPT")
                .build();

        receiptService.createReceipt(req);

        ArgumentCaptor<PaymentDO> captor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).insert(captor.capture());
        assertEquals(prefix + "003", captor.getValue().getPaymentNo(), "收款单碰撞后重试顺延至 003");
    }

    @Test
    @DisplayName("RC取号：手填已存在收款单号（含软删）抛 RECEIPT_STATUS_NOT_ALLOW 报错")
    void testReceiptNo_ManualDuplicateRejected() {
        CustomerDO customer = createCustomer(201L, "测试客户B");
        when(customerMapper.selectById(201L)).thenReturn(customer);
        AccountDO account = AccountDO.builder().id(1L).currency("CNY").status("ENABLE").build();
        when(accountService.getAccount(1L)).thenReturn(account);

        String manualNo = "RC-20261012-999";
        PaymentDO existing = PaymentDO.builder().id(99L).paymentNo(manualNo).build();
        existing.setDeleted(true);

        when(paymentMapper.selectByPaymentNoIncludingDeleted(manualNo)).thenReturn(existing);

        ReceiptSaveReqVO req = ReceiptSaveReqVO.builder()
                .paymentNo(manualNo)
                .customerId(201L)
                .accountId(1L)
                .payAmount(new BigDecimal("1000.00"))
                .currency("CNY")
                .receiptDate(LocalDate.now())
                .receiptType("RECEIPT")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.createReceipt(req));
        assertEquals(RECEIPT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== 3. 发票申请 (IA) 取号测试 ====================

    @Test
    @DisplayName("IA取号：软删行流水扫描，生成 IA{yyyy}0004")
    void testInvoiceNo_FormatAndSoftDeleteScan() {
        CustomerDO customer = createCustomer(301L, "测试客户C");
        customer.setUnifiedSocialCreditCode("91330100MA2XXXXX");
        when(customerMapper.selectById(301L)).thenReturn(customer);

        ShipmentDO shipment = createShipment(501L, 301L, new BigDecimal("5000.00"));
        when(shipmentMapper.selectById(501L)).thenReturn(shipment);

        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String prefix = "IA" + year;

        InvoiceApplicationDO softDeleted = InvoiceApplicationDO.builder()
                .id(30L).invoiceNo(prefix + "0003").customerId(301L).build();
        softDeleted.setDeleted(true);

        when(invoiceApplicationMapper.selectListByInvoiceNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(softDeleted));
        when(invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(prefix + "0004"))
                .thenReturn(null);

        InvoiceItemSaveVO itemVo = InvoiceItemSaveVO.builder()
                .shipmentId(501L)
                .amount(new BigDecimal("2000.00"))
                .build();
        InvoiceSaveReqVO req = InvoiceSaveReqVO.builder()
                .customerId(301L)
                .currency("CNY")
                .taxRate("13")
                .invoiceKind("ELEC_SPECIAL")
                .items(List.of(itemVo))
                .build();

        invoiceService.createInvoice(req);

        ArgumentCaptor<InvoiceApplicationDO> captor = ArgumentCaptor.forClass(InvoiceApplicationDO.class);
        verify(invoiceApplicationMapper).insert(captor.capture());
        assertEquals(prefix + "0004", captor.getValue().getInvoiceNo(), "发票申请应扫描软删记录并生成 0004");
    }

    @Test
    @DisplayName("IA取号：撞号重试机制生效，碰撞后重试顺延")
    void testInvoiceNo_CollisionRetry() {
        CustomerDO customer = createCustomer(301L, "测试客户C");
        when(customerMapper.selectById(301L)).thenReturn(customer);

        ShipmentDO shipment = createShipment(501L, 301L, new BigDecimal("5000.00"));
        when(shipmentMapper.selectById(501L)).thenReturn(shipment);

        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String prefix = "IA" + year;

        when(invoiceApplicationMapper.selectListByInvoiceNoPrefixIncludingDeleted(prefix))
                .thenReturn(Collections.emptyList());

        // 第一次生成 0001 碰撞
        InvoiceApplicationDO collided = InvoiceApplicationDO.builder().id(1L).invoiceNo(prefix + "0001").build();
        when(invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(prefix + "0001"))
                .thenReturn(collided)
                .thenReturn(null); // 第二次重试通过

        InvoiceItemSaveVO itemVo = InvoiceItemSaveVO.builder()
                .shipmentId(501L)
                .amount(new BigDecimal("2000.00"))
                .build();
        InvoiceSaveReqVO req = InvoiceSaveReqVO.builder()
                .customerId(301L)
                .currency("CNY")
                .taxRate("13")
                .invoiceKind("ELEC_SPECIAL")
                .items(List.of(itemVo))
                .build();

        invoiceService.createInvoice(req);

        ArgumentCaptor<InvoiceApplicationDO> captor = ArgumentCaptor.forClass(InvoiceApplicationDO.class);
        verify(invoiceApplicationMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getInvoiceNo());
    }

    @Test
    @DisplayName("IA取号：手填已存在单号（含软删行）报错拒绝")
    void testInvoiceNo_ManualDuplicateRejected() {
        CustomerDO customer = createCustomer(301L, "测试客户C");
        when(customerMapper.selectById(301L)).thenReturn(customer);

        ShipmentDO shipment = createShipment(501L, 301L, new BigDecimal("5000.00"));
        when(shipmentMapper.selectById(501L)).thenReturn(shipment);

        String manualNo = "IA20268888";
        InvoiceApplicationDO existing = InvoiceApplicationDO.builder().id(88L).invoiceNo(manualNo).build();
        existing.setDeleted(true);

        when(invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(manualNo)).thenReturn(existing);

        InvoiceItemSaveVO itemVo = InvoiceItemSaveVO.builder()
                .shipmentId(501L)
                .amount(new BigDecimal("2000.00"))
                .build();
        InvoiceSaveReqVO req = InvoiceSaveReqVO.builder()
                .invoiceNo(manualNo)
                .customerId(301L)
                .currency("CNY")
                .taxRate("13")
                .invoiceKind("ELEC_SPECIAL")
                .items(List.of(itemVo))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> invoiceService.createInvoice(req));
        assertEquals(INVOICE_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }
}
