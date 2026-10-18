package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceApplicationCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceApplicationItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import com.luna.lunasrv.module.lunaerp.service.finance.invoice.InvoiceApplicationServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 财务应收域发号测试（Issue 12 验收标准）
 *
 * 覆盖：
 * 1. CS/RC/IA 格式验证（CS{yyyy}{4位流水}、RC-{yyyyMMdd}-{3位日流水}、IA{yyyy}{4位流水}）
 * 2. 含软删行扫描验证（即使最大流水号记录已被软删，新号依然在其基础上顺延递增）
 * 3. 撞号重试与重试耗尽报错验证
 * 4. 手填单号存在（含软删行）即报错验证
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CustomerReconNumberGenerationTest {

    private static final Long TENANT_ID = 1L;
    private static final Long CUSTOMER_ID = 1001L;

    // Services under test
    private CustomerStatementServiceImpl customerStatementService;
    private PaymentServiceImpl paymentService;
    private InvoiceApplicationServiceImpl invoiceApplicationService;

    // Mocks
    @Mock
    private CustomerStatementMapper customerStatementMapper;
    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;
    @Mock
    private LedgerEntryMapper ledgerEntryMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private ShipmentMapper shipmentMapper;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentRefMapper paymentRefMapper;
    @Mock
    private PaymentApplyMapper paymentApplyMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private InvoiceApplicationMapper invoiceApplicationMapper;
    @Mock
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerStatementItemDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentRefDO.class);
        TableInfoHelper.initTableInfo(assistant, InvoiceApplicationDO.class);
        TableInfoHelper.initTableInfo(assistant, InvoiceApplicationItemDO.class);
        TableInfoHelper.initTableInfo(assistant, LedgerEntryDO.class);
        TableInfoHelper.initTableInfo(assistant, AccountDO.class);
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);

        customerStatementService = new CustomerStatementServiceImpl();
        ReflectionTestUtils.setField(customerStatementService, "customerStatementMapper", customerStatementMapper);
        ReflectionTestUtils.setField(customerStatementService, "customerStatementItemMapper", customerStatementItemMapper);
        ReflectionTestUtils.setField(customerStatementService, "ledgerEntryMapper", ledgerEntryMapper);
        ReflectionTestUtils.setField(customerStatementService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(customerStatementService, "shipmentMapper", shipmentMapper);
        ReflectionTestUtils.setField(customerStatementService, "paymentMapper", paymentMapper);

        paymentService = new PaymentServiceImpl();
        ReflectionTestUtils.setField(paymentService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(paymentService, "paymentRefMapper", paymentRefMapper);
        ReflectionTestUtils.setField(paymentService, "paymentApplyMapper", paymentApplyMapper);
        ReflectionTestUtils.setField(paymentService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(paymentService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(paymentService, "ledgerEntryMapper", ledgerEntryMapper);

        invoiceApplicationService = new InvoiceApplicationServiceImpl();
        ReflectionTestUtils.setField(invoiceApplicationService, "invoiceApplicationMapper", invoiceApplicationMapper);
        ReflectionTestUtils.setField(invoiceApplicationService, "invoiceApplicationItemMapper", invoiceApplicationItemMapper);
        ReflectionTestUtils.setField(invoiceApplicationService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(invoiceApplicationService, "shipmentMapper", shipmentMapper);

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("测试纺织客户");
        customer.setInvoiceTitle("测试开票抬头");
        customer.setCreditDays(30);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);

        AccountDO account = new AccountDO();
        account.setId(201L);
        account.setAccountName("默认公户");
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        when(accountMapper.selectById(201L)).thenReturn(account);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 1. 对账单 CS 取号测试 ====================

    @Test
    @DisplayName("CS 取号：格式 CS{yyyy}{4位流水} 且含软删行扫描递增")
    void testCustomerStatement_generateNo_formatAndScanDeleted() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "CS" + year;

        // 模拟数据库中存在一条最大号为 CS{year}0008 的软删除记录
        CustomerStatementDO softDeleted = new CustomerStatementDO();
        softDeleted.setId(88L);
        softDeleted.setStatementNo(prefix + "0008");
        softDeleted.setDeleted(true);
        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(softDeleted));
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(anyString())).thenReturn(null);

        // 构造创建参数与可用分录
        CustomerStatementCreateReqVO reqVO = new CustomerStatementCreateReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        reqVO.setCurrency("RMB");
        reqVO.setPeriodStart(LocalDate.now().minusMonths(1));
        reqVO.setPeriodEnd(LocalDate.now());

        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(501L);
        entry.setPartyId(CUSTOMER_ID);
        entry.setCategory(LedgerCategory.AR_INC.getValue());
        entry.setCurrency("RMB");
        entry.setDirection("DEBIT");
        entry.setOriginalAmount(new BigDecimal("1000.00"));
        entry.setBizDate(LocalDate.now().minusDays(5));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(entry));

        customerStatementService.createCustomerStatement(reqVO);

        ArgumentCaptor<CustomerStatementDO> captor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).insert(captor.capture());
        CustomerStatementDO saved = captor.getValue();
        // 期望在软删行 0008 的基础上递增为 0009
        assertEquals(prefix + "0009", saved.getStatementNo());
    }

    @Test
    @DisplayName("CS 取号：撞号重试成功与重试耗尽抛出 STATEMENT_NO_EXHAUSTED")
    void testCustomerStatement_retryExhausted() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "CS" + year;

        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(prefix))
                .thenReturn(Collections.emptyList());
        // 模拟前 10 次候选单号全被并发占用
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(anyString()))
                .thenReturn(new CustomerStatementDO());

        CustomerStatementCreateReqVO reqVO = new CustomerStatementCreateReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        reqVO.setCurrency("RMB");
        reqVO.setPeriodStart(LocalDate.now().minusMonths(1));
        reqVO.setPeriodEnd(LocalDate.now());

        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(501L);
        entry.setPartyId(CUSTOMER_ID);
        entry.setCategory(LedgerCategory.AR_INC.getValue());
        entry.setCurrency("RMB");
        entry.setDirection("DEBIT");
        entry.setOriginalAmount(new BigDecimal("1000.00"));
        entry.setBizDate(LocalDate.now().minusDays(5));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(entry));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.createCustomerStatement(reqVO));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_NO_EXHAUSTED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("CS 取号：手填单号撞号（含软删行）报错 STATEMENT_NO_EXHAUSTED")
    void testCustomerStatement_manualDuplicate_throwsError() {
        CustomerStatementCreateReqVO reqVO = new CustomerStatementCreateReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        reqVO.setCurrency("RMB");
        reqVO.setPeriodStart(LocalDate.now().minusMonths(1));
        reqVO.setPeriodEnd(LocalDate.now());
        reqVO.setStatementNo("CS20268888");

        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(501L);
        entry.setPartyId(CUSTOMER_ID);
        entry.setCategory(LedgerCategory.AR_INC.getValue());
        entry.setCurrency("RMB");
        entry.setDirection("DEBIT");
        entry.setOriginalAmount(new BigDecimal("1000.00"));
        entry.setBizDate(LocalDate.now().minusDays(5));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(entry));

        // 模拟已存在相同单号
        CustomerStatementDO existing = new CustomerStatementDO();
        existing.setStatementNo("CS20268888");
        when(customerStatementMapper.selectByStatementNoIncludingDeleted("CS20268888")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.createCustomerStatement(reqVO));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_NO_EXHAUSTED.getCode(), ex.getCode());
    }

    // ==================== 2. 收款单 RC 取号测试 ====================

    @Test
    @DisplayName("RC 取号：格式 RC-{yyyyMMdd}-{3位日流水} 且含软删行扫描递增")
    void testReceipt_generateNo_formatAndScanDeleted() {
        LocalDate today = LocalDate.now();
        String dateStr = today.toString().replace("-", "");
        String prefix = "RC-" + dateStr + "-";

        PaymentDO softDeleted = new PaymentDO();
        softDeleted.setId(99L);
        softDeleted.setPaymentNo(prefix + "005");
        softDeleted.setDeleted(true);
        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(softDeleted));
        when(paymentMapper.selectByPaymentNoIncludingDeleted(anyString())).thenReturn(null);

        ReceiptSaveReqVO reqVO = new ReceiptSaveReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        reqVO.setAccountId(201L);
        reqVO.setCurrency("RMB");
        reqVO.setReceiptType("RECEIPT");
        reqVO.setPayAmount(new BigDecimal("5000.00"));
        reqVO.setReceiptDate(today);

        paymentService.createReceipt(reqVO);

        ArgumentCaptor<PaymentDO> captor = ArgumentCaptor.forClass(PaymentDO.class);
        verify(paymentMapper).insert(captor.capture());
        PaymentDO saved = captor.getValue();
        // 软删行 005 递增为 006
        assertEquals(prefix + "006", saved.getPaymentNo());
    }

    @Test
    @DisplayName("RC 取号：手填单号撞号（含软删行）报错 PAYMENT_NO_EXHAUSTED")
    void testReceipt_manualDuplicate_throwsError() {
        LocalDate today = LocalDate.now();
        ReceiptSaveReqVO reqVO = new ReceiptSaveReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        reqVO.setAccountId(201L);
        reqVO.setCurrency("RMB");
        reqVO.setReceiptType("RECEIPT");
        reqVO.setPayAmount(new BigDecimal("5000.00"));
        reqVO.setReceiptDate(today);
        reqVO.setPaymentNo("RC-20261018-999");

        PaymentDO existing = new PaymentDO();
        existing.setPaymentNo("RC-20261018-999");
        when(paymentMapper.selectByPaymentNoIncludingDeleted("RC-20261018-999")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                paymentService.createReceipt(reqVO));
        assertEquals(FinanceErrorCodeConstants.PAYMENT_NO_EXHAUSTED.getCode(), ex.getCode());
    }

    // ==================== 3. 发票申请 IA 取号测试 ====================

    @Test
    @DisplayName("IA 取号：格式 IA{yyyy}{4位流水} 且含软删行扫描递增")
    void testInvoice_generateNo_formatAndScanDeleted() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "IA" + year;

        InvoiceApplicationDO softDeleted = new InvoiceApplicationDO();
        softDeleted.setId(77L);
        softDeleted.setInvoiceNo(prefix + "0012");
        softDeleted.setDeleted(true);
        when(invoiceApplicationMapper.selectListByInvoiceNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(softDeleted));
        when(invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(anyString())).thenReturn(null);

        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(601L);
        shipment.setShipmentNo("SH20261018001");
        shipment.setTotalAmount(new BigDecimal("1000.00"));
        shipment.setFreightFee(BigDecimal.ZERO);
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(CUSTOMER_ID);
        shipment.setCurrency("RMB");
        when(shipmentMapper.selectById(601L)).thenReturn(shipment);
        when(shipmentMapper.selectBatchIds(any())).thenReturn(List.of(shipment));

        InvoiceApplicationCreateReqVO reqVO = new InvoiceApplicationCreateReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        reqVO.setCurrency("RMB");
        reqVO.setTaxRate(new BigDecimal("13.00"));
        reqVO.setInvoiceKind("DIGITAL_SPECIAL");
        InvoiceApplicationItemSaveReqVO itemReq = new InvoiceApplicationItemSaveReqVO();
        itemReq.setShipmentId(601L);
        itemReq.setAmount(new BigDecimal("1000.00"));
        reqVO.setItems(List.of(itemReq));

        invoiceApplicationService.createInvoiceApplication(reqVO);

        ArgumentCaptor<InvoiceApplicationDO> captor = ArgumentCaptor.forClass(InvoiceApplicationDO.class);
        verify(invoiceApplicationMapper).insert(captor.capture());
        InvoiceApplicationDO saved = captor.getValue();
        // 软删行 0012 递增为 0013
        assertEquals(prefix + "0013", saved.getInvoiceNo());
    }

    @Test
    @DisplayName("IA 取号：手填单号撞号（含软删行）报错 INVOICE_NO_EXHAUSTED")
    void testInvoice_manualDuplicate_throwsError() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(601L);
        shipment.setShipmentNo("SH20261018001");
        shipment.setTotalAmount(new BigDecimal("1000.00"));
        shipment.setFreightFee(BigDecimal.ZERO);
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(CUSTOMER_ID);
        shipment.setCurrency("RMB");
        when(shipmentMapper.selectById(601L)).thenReturn(shipment);
        when(shipmentMapper.selectBatchIds(any())).thenReturn(List.of(shipment));

        InvoiceApplicationCreateReqVO reqVO = new InvoiceApplicationCreateReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        reqVO.setCurrency("RMB");
        reqVO.setTaxRate(new BigDecimal("13.00"));
        reqVO.setInvoiceKind("DIGITAL_SPECIAL");
        reqVO.setInvoiceNo("IA20268888");
        InvoiceApplicationItemSaveReqVO itemReq = new InvoiceApplicationItemSaveReqVO();
        itemReq.setShipmentId(601L);
        itemReq.setAmount(new BigDecimal("1000.00"));
        reqVO.setItems(List.of(itemReq));

        InvoiceApplicationDO existing = new InvoiceApplicationDO();
        existing.setInvoiceNo("IA20268888");
        when(invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted("IA20268888")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.createInvoiceApplication(reqVO));
        assertEquals(FinanceErrorCodeConstants.INVOICE_NO_EXHAUSTED.getCode(), ex.getCode());
    }

}
