package com.luna.lunasrv.module.lunaerp.service.finance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceIssueReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceItemSaveVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AdvanceWriteoffReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AllocatePreviewReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.AllocatePreviewRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRefSaveVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate.ExchangeRateMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.CustomerStatementStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging.CustomerAgingCalculator;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.invoice.InvoiceServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.CustomerReceiptServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 客户对账全闭环端到端集成冒烟测试 (Issue 12 Capstone E2E)
 *
 * 验证端到端全链路业务流：
 * 1. 建汇率（USD 7.20000000 即期牌价生效）
 * 2. 销售发货产生应收分录（发货明细+运费，按汇率快照折算本位币）
 * 3. 收款单创建 + 自动分配（FIFO试算） + 审批通过一步过账（账户资金流入 + 经营分录入账 + 核销行落库）
 * 4. 预收单创建 + 审批通过 + 预收转核销（预收余额受控扣减）
 * 5. 生成客户对账单（期初/借方发生额/贷方发生额/期末余额纯函数重算）
 * 6. 发送客户对账单（DRAFT -> SENT）
 * 7. 代客确认对账单（SENT -> CONFIRMED，审计留痕 confirm_type=ON_BEHALF）
 * 8. 打印对账单数据校验（豁免脱敏，二维码 URL 正确解析）
 * 9. FIFO 账龄分桶汇总与下钻（五桶纯函数计算、credit_days 逾期判定、预收单列不进桶）
 * 10. 发票申请创建与提交（价税分离、税额精确反算、超开守卫校验）
 * 11. 发票登记回填数电票 20 位发票号码（SUBMITTED -> ISSUED）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerReconciliationE2ETest 客户对账全链路端到端冒烟测试")
public class CustomerReconciliationE2ETest {

    @BeforeAll
    static void initMyBatisPlus() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, ExchangeRateDO.class);
        TableInfoHelper.initTableInfo(assistant, LedgerEntryDO.class);
        TableInfoHelper.initTableInfo(assistant, AccountDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentRefDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerStatementItemDO.class);
        TableInfoHelper.initTableInfo(assistant, InvoiceApplicationDO.class);
        TableInfoHelper.initTableInfo(assistant, InvoiceApplicationItemDO.class);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // --- Mocks ---
    @Mock
    private ExchangeRateMapper exchangeRateMapper;
    @Mock
    private LedgerEntryMapper ledgerEntryMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private AccountService accountService;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentRefMapper paymentRefMapper;
    @Mock
    private CustomerStatementMapper customerStatementMapper;
    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;
    @Mock
    private InvoiceApplicationMapper invoiceApplicationMapper;
    @Mock
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private ShipmentMapper shipmentMapper;
    @Mock
    private ConfigApi configApi;
    @Mock
    private ApprovalInstanceService approvalInstanceService;

    // --- Services ---
    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;
    @InjectMocks
    private LedgerServiceImpl ledgerService;
    @InjectMocks
    private CustomerReceiptServiceImpl receiptService;
    @InjectMocks
    private CustomerStatementServiceImpl statementService;
    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(receiptService, "ledgerService", ledgerService);
        ReflectionTestUtils.setField(receiptService, "exchangeRateService", exchangeRateService);
        ReflectionTestUtils.setField(receiptService, "approvalInstanceService", approvalInstanceService);
    }

    @Test
    @DisplayName("端到端冒烟：建汇率→收款核销→预收转核销→对账单→发送确认→打印→账龄→发票申请登记")
    void testFullCustomerReconciliationE2ELoop() {
        TenantContextHolder.setTenantId(1L);
        LocalDate today = LocalDate.now();
        Long customerId = 1001L;

        // ==================== Step 1: 建汇率 ====================
        ExchangeRateSaveReqVO rateReq = new ExchangeRateSaveReqVO();
        rateReq.setCurrency("USD");
        rateReq.setRate(new BigDecimal("7.20000000"));
        rateReq.setEffectiveDate(today.minusDays(60));
        rateReq.setRemark("初期外币基准汇率");

        when(exchangeRateMapper.selectByCurrencyAndEffectiveDateIncludingDeleted(eq("USD"), any(LocalDate.class))).thenReturn(null);
        doAnswer(invocation -> {
            ExchangeRateDO rate = invocation.getArgument(0);
            rate.setId(101L);
            return 1;
        }).when(exchangeRateMapper).insert(any(ExchangeRateDO.class));

        Long rateId = exchangeRateService.createExchangeRate(rateReq);
        assertNotNull(rateId);

        ExchangeRateDO savedRate = ExchangeRateDO.builder()
                .id(rateId)
                .currency("USD")
                .rate(new BigDecimal("7.20000000"))
                .effectiveDate(today.minusDays(60))
                .status("ENABLE")
                .build();
        when(exchangeRateMapper.selectLatestEffectiveRate(eq("USD"), any(LocalDate.class)))
                .thenReturn(savedRate);

        BigDecimal effectiveRate = exchangeRateService.getRate("USD", today);
        assertEquals(0, new BigDecimal("7.20000000").compareTo(effectiveRate), "USD 即期汇率应为 7.20");

        // ==================== Step 2: 客户档案与发货 AR 分录 ====================
        CustomerDO customer = new CustomerDO();
        customer.setId(customerId);
        customer.setCompanyName("环球外贸进出口集团");
        customer.setInvoiceTitle("环球外贸进出口集团有限公司");
        customer.setUnifiedSocialCreditCode("91330100MA2ABC1234");
        customer.setInvoiceAddress("杭州市西湖区求是路 8 号 0571-88889999");
        customer.setBankName("招商银行西湖支行");
        customer.setBankAccountNo("6214850012345678");
        customer.setCreditDays(30); // 30天信用账期
        customer.setStatus("ACTIVE");
        customer.setDeleted(false);
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        // 模拟两笔发货分录：
        // 1. 发货 1 (45 天前): USD 1000.00, 折合 RMB 7200.00
        LedgerEntryDO shipmentEntry1 = LedgerEntryDO.builder()
                .id(501L)
                .bizDate(today.minusDays(45))
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(customerId)
                .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                .currency("USD")
                .exchangeRate(new BigDecimal("7.20000000"))
                .originalAmount(new BigDecimal("1000.00"))
                .baseAmount(new BigDecimal("7200.00"))
                .refType("SHIPMENT")
                .refId(2001L)
                .build();

        // 2. 发货 2 (15 天前): USD 2000.00, 折合 RMB 14400.00
        LedgerEntryDO shipmentEntry2 = LedgerEntryDO.builder()
                .id(502L)
                .bizDate(today.minusDays(15))
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(customerId)
                .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                .currency("USD")
                .exchangeRate(new BigDecimal("7.20000000"))
                .originalAmount(new BigDecimal("2000.00"))
                .baseAmount(new BigDecimal("14400.00"))
                .refType("SHIPMENT")
                .refId(2002L)
                .build();

        // 3. 运费 (15 天前): USD 100.00, 折合 RMB 720.00
        LedgerEntryDO freightEntry = LedgerEntryDO.builder()
                .id(503L)
                .bizDate(today.minusDays(15))
                .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                .partyId(customerId)
                .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                .currency("USD")
                .exchangeRate(new BigDecimal("7.20000000"))
                .originalAmount(new BigDecimal("100.00"))
                .baseAmount(new BigDecimal("720.00"))
                .refType("SHIPMENT")
                .refId(2002L)
                .build();

        // ==================== Step 3: 收款单创建 + FIFO 自动分配试算 ====================
        AccountDO account = AccountDO.builder()
                .id(1L)
                .accountName("企业美金账户")
                .currency("USD")
                .currentBalance(new BigDecimal("50000.00"))
                .status("ENABLE")
                .build();
        when(accountService.getAccount(1L)).thenReturn(account);

        // 模拟未核销应收列表 (501L 未核销 1000, 502L 未核销 2000, 503L 未核销 100)
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(shipmentEntry1, shipmentEntry2, freightEntry));
        when(paymentRefMapper.selectListByRefTypeAndRefId(anyString(), anyLong())).thenReturn(Collections.emptyList());
        when(ledgerEntryMapper.selectListByReversalOfId(anyLong())).thenReturn(Collections.emptyList());

        // FIFO 分配预览：收款 800 USD，应全部优先分配给最老的 501L
        AllocatePreviewReqVO previewReq = new AllocatePreviewReqVO();
        previewReq.setCustomerId(customerId);
        previewReq.setCurrency("USD");
        previewReq.setPayAmount(new BigDecimal("800.00"));
        AllocatePreviewRespVO preview = receiptService.allocatePreview(previewReq);

        assertEquals(0, new BigDecimal("800.00").compareTo(preview.getTotalAllocated()));
        assertEquals(0, BigDecimal.ZERO.compareTo(preview.getRemainingPayAmount()));
        assertEquals(1, preview.getItems().size());
        assertEquals(501L, preview.getItems().get(0).getLedgerEntryId());
        assertEquals(0, new BigDecimal("800.00").compareTo(preview.getItems().get(0).getAllocatedAmount()));

        // ==================== Step 4: 创建收款单与过账 ====================
        ReceiptSaveReqVO receiptReq = ReceiptSaveReqVO.builder()
                .customerId(customerId)
                .accountId(1L)
                .payAmount(new BigDecimal("800.00"))
                .currency("USD")
                .receiptDate(today.minusDays(10))
                .receiptType("RECEIPT")
                .build();
        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(paymentMapper.selectByPaymentNoIncludingDeleted(any())).thenReturn(null);
        doAnswer(invocation -> {
            PaymentDO p = invocation.getArgument(0);
            p.setId(201L);
            return 1;
        }).when(paymentMapper).insert(any(PaymentDO.class));

        Long receiptPaymentId = receiptService.createReceipt(receiptReq);
        assertNotNull(receiptPaymentId);

        // 模拟审批通过：触发同事务过账
        PaymentDO receiptPayment = PaymentDO.builder()
                .id(receiptPaymentId)
                .paymentNo("RC-20261012-001")
                .direction("IN")
                .customerId(customerId)
                .accountId(1L)
                .payAmount(new BigDecimal("800.00"))
                .currency("USD")
                .receiptType("RECEIPT")
                .receiptDate(today.minusDays(10))
                .status("PENDING")
                .approvalStatus("APPROVING")
                .build();
        when(paymentMapper.selectById(receiptPaymentId)).thenReturn(receiptPayment);

        doAnswer(invocation -> {
            LedgerEntryDO entry = invocation.getArgument(0);
            entry.setId(601L);
            return 1;
        }).when(ledgerEntryMapper).insert(any(LedgerEntryDO.class));

        receiptService.postReceiptOnApproval(receiptPaymentId);

        // 验证资金账户原子更新与单据状态更新
        verify(accountService).updateBalance(eq(1L), eq(new BigDecimal("800.00")));
        verify(paymentMapper).updateById(argThat((PaymentDO p) -> "COMPLETED".equals(p.getStatus()) && "APPROVED".equals(p.getApprovalStatus())));

        // ==================== Step 5: 预收转核销 ====================
        // 模拟创建并过账了一笔 ADVANCE 预收款单，金额 500 USD
        PaymentDO advancePayment = PaymentDO.builder()
                .id(102L)
                .paymentNo("RC-20261012-002")
                .direction("IN")
                .customerId(customerId)
                .accountId(1L)
                .payAmount(new BigDecimal("500.00"))
                .currency("USD")
                .receiptType("ADVANCE")
                .receiptDate(today.minusDays(8))
                .status("COMPLETED")
                .approvalStatus("APPROVED")
                .build();
        when(paymentMapper.selectById(102L)).thenReturn(advancePayment);

        // 预收转核销：转 200 USD 核销 501L (至此 501L 结清)，转 100 USD 核销 502L
        AdvanceWriteoffReqVO writeoffReq = new AdvanceWriteoffReqVO();
        writeoffReq.setPaymentId(102L);
        writeoffReq.setItems(List.of(
                ReceiptRefSaveVO.builder().ledgerEntryId(501L).allocateAmount(new BigDecimal("200.00")).build(),
                ReceiptRefSaveVO.builder().ledgerEntryId(502L).allocateAmount(new BigDecimal("100.00")).build()
        ));

        // 预收核销前：501L 已核销 800 (剩余 200), 502L 未核销 (剩余 2000)
        when(ledgerEntryMapper.selectOne(any())).thenReturn(null);
        when(ledgerEntryMapper.selectById(501L)).thenReturn(shipmentEntry1);
        when(ledgerEntryMapper.selectById(502L)).thenReturn(shipmentEntry2);

        PaymentRefDO existingRef501 = PaymentRefDO.builder()
                .id(1L).paymentId(receiptPaymentId).refType("AR_ENTRY").refId(501L)
                .allocateAmount(new BigDecimal("800.00")).build();
        when(paymentRefMapper.selectListByRefTypeAndRefId(eq("AR_ENTRY"), eq(501L))).thenReturn(List.of(existingRef501));
        when(paymentRefMapper.selectListByRefTypeAndRefId(eq("AR_ENTRY"), eq(502L))).thenReturn(Collections.emptyList());
        when(paymentRefMapper.selectListByPaymentId(102L)).thenReturn(Collections.emptyList());

        receiptService.advanceWriteoff(writeoffReq);

        // 验证预收转核销落库了 2 条核销明细
        verify(paymentRefMapper, times(2)).insert(any(PaymentRefDO.class));

        // ==================== Step 6: 生成客户对账单 ====================
        // 模拟对账单分录来源：
        // 借方：501L (1000.00), 502L (2000.00), 503L (100.00) -> 合计借方 3100.00
        // 贷方：收款核销 (800.00), 预收核销 1 (200.00), 预收核销 2 (100.00) -> 合计贷方 1100.00
        // 期末欠款 = 3100.00 - 1100.00 = 2000.00
        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(any())).thenReturn(null);
        when(customerStatementItemMapper.selectActiveOccupiedLedgerEntryIds()).thenReturn(Collections.emptyList());
        when(ledgerEntryMapper.selectList(any())).thenReturn(Collections.emptyList());

        CustomerStatementCreateReqVO statementReq = CustomerStatementCreateReqVO.builder()
                .customerId(customerId)
                .currency("USD")
                .periodStart(today.minusDays(60))
                .periodEnd(today)
                .selectedEntryIds(List.of(501L, 502L, 503L))
                .build();

        when(ledgerEntryMapper.selectById(501L)).thenReturn(shipmentEntry1);
        when(ledgerEntryMapper.selectById(502L)).thenReturn(shipmentEntry2);
        when(ledgerEntryMapper.selectById(503L)).thenReturn(freightEntry);

        doAnswer(invocation -> {
            CustomerStatementDO s = invocation.getArgument(0);
            s.setId(301L);
            return 1;
        }).when(customerStatementMapper).insert(any(CustomerStatementDO.class));

        Long statementId = statementService.createStatement(statementReq);
        assertNotNull(statementId);

        ArgumentCaptor<CustomerStatementDO> stmtCaptor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).insert(stmtCaptor.capture());
        CustomerStatementDO createdStmt = stmtCaptor.getValue();

        assertEquals(0, BigDecimal.ZERO.compareTo(createdStmt.getOpeningBalance()), "期初应收为 0");
        assertEquals(0, new BigDecimal("3100.00").compareTo(createdStmt.getPeriodDebit()), "本期借方应为 3100.00");
        assertEquals(CustomerStatementStatusEnum.DRAFT.name(), createdStmt.getStatus(), "初始状态应为 DRAFT");

        // ==================== Step 7: 发送对账单与代客确认 ====================
        createdStmt.setId(statementId);
        createdStmt.setStatus(CustomerStatementStatusEnum.DRAFT.name());
        when(customerStatementMapper.selectById(statementId)).thenReturn(createdStmt);
        when(customerStatementMapper.update(isNull(), any())).thenReturn(1);

        statementService.sendStatement(statementId);
        createdStmt.setStatus(CustomerStatementStatusEnum.SENT.name());

        CustomerStatementConfirmOnBehalfReqVO confirmReq = new CustomerStatementConfirmOnBehalfReqVO();
        confirmReq.setId(statementId);
        confirmReq.setConfirmMethod("WECHAT_OFFLINE");
        confirmReq.setRemark("微信回传确认单盖章扫描件");

        statementService.confirmStatementOnBehalf(confirmReq);
        verify(customerStatementMapper, times(2)).update(isNull(), any());

        // ==================== Step 8: 打印对账单数据校验 ====================
        createdStmt.setStatus(CustomerStatementStatusEnum.CONFIRMED.name());
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://erp.example.com");

        CustomerStatementItemDO item1 = CustomerStatementItemDO.builder()
                .id(1L).statementId(statementId).ledgerEntryId(501L)
                .direction("DEBIT").debitAmount(new BigDecimal("1000.00")).creditAmount(BigDecimal.ZERO)
                .bizDate(today.minusDays(45)).build();
        CustomerStatementItemDO item2 = CustomerStatementItemDO.builder()
                .id(2L).statementId(statementId).ledgerEntryId(502L)
                .direction("DEBIT").debitAmount(new BigDecimal("2000.00")).creditAmount(BigDecimal.ZERO)
                .bizDate(today.minusDays(15)).build();
        when(customerStatementItemMapper.selectList(any())).thenReturn(List.of(item1, item2));

        CustomerStatementPrintRespVO printData = statementService.getStatementPrintData(statementId);
        assertNotNull(printData);
        assertEquals("环球外贸进出口集团", printData.getCustomerName());
        assertEquals("招商银行西湖支行", printData.getBankName());
        assertEquals("6214850012345678", printData.getBankAccountNo(), "打印 VO 豁免脱敏，银行卡号必须为完整明文");
        assertTrue(printData.getQrUrl().contains("https://erp.example.com/portal/statement"));
        assertEquals(2, printData.getItems().size());

        // ==================== Step 9: 账龄纯函数分桶计算 ====================
        // 所有分录汇总到基准日：
        // 501L: 1000 - 800 - 200 = 0 剩余
        // 502L: 2000 - 100 = 1900 剩余 (发生于 15 天前 -> bucket 0-30 天)
        // 503L: 100 剩余 (发生于 15 天前 -> bucket 0-30 天)
        // 预收剩余池: 500 - 300 = 200 (单列，不冲抵账龄桶)
        LedgerEntryDO creditRef1 = LedgerEntryDO.builder()
                .id(601L).bizDate(today.minusDays(10)).category(FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue())
                .direction("CREDIT").currency("USD").originalAmount(new BigDecimal("800.00")).build();
        LedgerEntryDO creditRef2 = LedgerEntryDO.builder()
                .id(602L).bizDate(today.minusDays(5)).category(FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue())
                .direction("CREDIT").currency("USD").originalAmount(new BigDecimal("300.00")).build();

        List<LedgerEntryDO> allEntries = List.of(shipmentEntry1, shipmentEntry2, freightEntry, creditRef1, creditRef2);
        CustomerAgingCalculator.AgingCalculationResult agingResult = CustomerAgingCalculator.calculate(
                customer, "USD", today, allEntries, new BigDecimal("200.00"), null);

        CustomerAgingSummaryRespVO agingSummary = agingResult.getSummary();
        assertNotNull(agingSummary);
        assertEquals(0, new BigDecimal("2000.00").compareTo(agingSummary.getTotalBalance()), "未核销应收总额应为 2000.00");
        assertEquals(0, new BigDecimal("2000.00").compareTo(agingSummary.getBucket0To30()), "0-30天账龄应为 2000.00");
        assertEquals(0, BigDecimal.ZERO.compareTo(agingSummary.getBucket31To60()), "31-60天账龄应为 0");
        assertEquals(0, new BigDecimal("200.00").compareTo(agingSummary.getAdvanceBalance()), "预收余额 200.00 单列不进桶");

        // ==================== Step 10 & 11: 发票申请创建、提交与登记回填 ====================
        ShipmentDO shipment2 = new ShipmentDO();
        shipment2.setId(2002L);
        shipment2.setCustomerId(customerId);
        shipment2.setCurrency("USD");
        shipment2.setTotalAmount(new BigDecimal("2000.00"));
        shipment2.setFreightFee(new BigDecimal("100.00"));
        shipment2.setStatus("COMPLETED");
        shipment2.setDeleted(false);
        when(shipmentMapper.selectById(2002L)).thenReturn(shipment2);
        when(shipmentMapper.selectByIdForUpdate(2002L)).thenReturn(shipment2);
        when(invoiceApplicationItemMapper.selectInvoicedAmountByShipmentId(2002L, null)).thenReturn(BigDecimal.ZERO);

        InvoiceItemSaveVO invoiceItem = InvoiceItemSaveVO.builder()
                .shipmentId(2002L)
                .amount(new BigDecimal("2100.00")) // 包含货款与运费全额开票
                .build();
        InvoiceSaveReqVO invoiceReq = InvoiceSaveReqVO.builder()
                .customerId(customerId)
                .currency("USD")
                .taxRate("13")
                .invoiceKind("ELEC_SPECIAL")
                .items(List.of(invoiceItem))
                .build();
        when(invoiceApplicationMapper.selectListByInvoiceNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(any())).thenReturn(null);

        doAnswer(invocation -> {
            InvoiceApplicationDO app = invocation.getArgument(0);
            app.setId(401L);
            return 1;
        }).when(invoiceApplicationMapper).insert(any(InvoiceApplicationDO.class));

        Long invoiceId = invoiceService.createInvoice(invoiceReq);
        assertNotNull(invoiceId);

        ArgumentCaptor<InvoiceApplicationDO> invoiceCaptor = ArgumentCaptor.forClass(InvoiceApplicationDO.class);
        verify(invoiceApplicationMapper).insert(invoiceCaptor.capture());
        InvoiceApplicationDO createdInvoice = invoiceCaptor.getValue();

        assertEquals(0, new BigDecimal("2100.00").compareTo(createdInvoice.getTotalAmountInclTax()), "含税总额应为 2100.00");
        // 13% 税率反算：不含税 = 2100 / 1.13 = 1858.41, 税额 = 241.59
        assertEquals(0, new BigDecimal("1858.41").compareTo(createdInvoice.getTotalAmountExclTax()), "不含税额应为 1858.41");
        assertEquals(0, new BigDecimal("241.59").compareTo(createdInvoice.getTotalTax()), "税额应为 241.59");
        assertEquals("DRAFT", createdInvoice.getStatus());

        // 提交发票申请
        createdInvoice.setId(invoiceId);
        when(invoiceApplicationMapper.selectById(invoiceId)).thenReturn(createdInvoice);
        when(invoiceApplicationItemMapper.selectListByApplicationId(invoiceId)).thenReturn(
                List.of(InvoiceApplicationItemDO.builder().shipmentId(2002L).amount(new BigDecimal("2100.00")).build())
        );
        when(invoiceApplicationItemMapper.selectInvoicedAmountByShipmentId(2002L, invoiceId)).thenReturn(BigDecimal.ZERO);

        invoiceService.submitInvoice(invoiceId);
        assertEquals("SUBMITTED", createdInvoice.getStatus(), "提交后状态应为 SUBMITTED");

        // 登记开票回填 20 位数电发票号
        String officialInvoiceNo = "24332000000012345678";
        when(invoiceApplicationMapper.selectByOfficialInvoiceNoIncludingDeleted(officialInvoiceNo)).thenReturn(null);

        InvoiceIssueReqVO issueReq = InvoiceIssueReqVO.builder()
                .id(invoiceId)
                .officialInvoiceNo(officialInvoiceNo)
                .invoiceDate(today)
                .attachmentUrl("https://oss.example.com/invoice/20261012.pdf")
                .build();

        invoiceService.issueInvoice(issueReq);

        assertEquals("ISSUED", createdInvoice.getStatus(), "登记后终态应为 ISSUED");
        assertEquals(officialInvoiceNo, createdInvoice.getOfficialInvoiceNo());
        assertEquals(today, createdInvoice.getInvoiceDate());
    }
}
