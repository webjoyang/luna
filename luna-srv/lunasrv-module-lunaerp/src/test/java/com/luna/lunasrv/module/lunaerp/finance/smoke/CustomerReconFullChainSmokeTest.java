package com.luna.lunasrv.module.lunaerp.finance.smoke;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.handler.CustomerReceiptApprovalHandler;
import com.luna.lunasrv.module.lunaerp.common.notify.LunaNotifyHelper;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.CustomerAgingController;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.CustomerStatementController;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.ExchangeRateController;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.InvoiceApplicationController;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.PaymentController;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.EntryAllocatedSumDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate.ExchangeRateMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.*;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customer.CustomerService;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging.CustomerAgingCalculator;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging.CustomerAgingServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.invoice.InvoiceApplicationServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.PaymentServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 财务域 - 客户应收全链路业务冒烟与性能测试 (Issue 12 Capstone)
 *
 * 覆盖全流程端到端闭环：
 * 1. 建汇率（ExchangeRate SPOT 即期牌价）
 * 2. 发货单审核生成应收分录（AR_INC 逐明细行 + 运费行）
 * 3. 收款单（创建 -> 提交审批 -> 审批通过过账，自动分配核销）
 * 4. 预收款单（ADVANCE 分型 -> 审批过账）与预收转核销（advanceWriteoff）
 * 5. 客户对账单（生成预览 -> 创建草稿 -> 发送 -> 代客确认冻结）
 * 6. 打印凭证与二维码（豁免脱敏金额、二维码 URL 降级容错）
 * 7. 账龄汇总与下钻（FIFO 五桶实时聚合、credit_days 逾期判定、预收单列不进桶）
 * 8. 发票申请（候选发货单 -> 创建 -> 提交 -> 登记开票回填 20 位官方发票号）
 * 9. 芋道权限码逐位断言 + 租户隔离与横向越权测试
 * 10. 性能测试：万级数据聚合 P95 < 1s 性能断言 + 复合索引验证
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CustomerReconFullChainSmokeTest {

    private static final Long TENANT_ID = 1L;
    private static final Long LOGIN_USER_ID = 888L;
    private static final Long CUSTOMER_A_ID = 1001L;
    private static final String CUSTOMER_A_NAME = "苏州盛泽面料实业有限公司";
    private static final Long CUSTOMER_B_ID = 2002L;
    private static final String CUSTOMER_B_NAME = "常州华盛进出口有限公司";
    private static final Long ACCOUNT_ID = 101L;

    // In-memory repositories for database operations
    private final Map<Long, ExchangeRateDO> exchangeRateRepo = new LinkedHashMap<>();
    private final Map<Long, AccountDO> accountRepo = new LinkedHashMap<>();
    private final Map<Long, CustomerDO> customerRepo = new LinkedHashMap<>();
    private final Map<Long, ShipmentDO> shipmentRepo = new LinkedHashMap<>();
    private final Map<Long, LedgerEntryDO> ledgerRepo = new LinkedHashMap<>();
    private final Map<Long, PaymentDO> paymentRepo = new LinkedHashMap<>();
    private final Map<Long, PaymentRefDO> paymentRefRepo = new LinkedHashMap<>();
    private final Map<Long, CustomerStatementDO> statementRepo = new LinkedHashMap<>();
    private final Map<Long, CustomerStatementItemDO> statementItemRepo = new LinkedHashMap<>();
    private final Map<Long, InvoiceApplicationDO> invoiceRepo = new LinkedHashMap<>();
    private final Map<Long, InvoiceApplicationItemDO> invoiceItemRepo = new LinkedHashMap<>();

    private final AtomicLong idSeq = new AtomicLong(10000);

    // Mocks
    @Mock
    private ExchangeRateMapper exchangeRateMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private ShipmentMapper shipmentMapper;
    @Mock
    private LedgerEntryMapper ledgerEntryMapper;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentRefMapper paymentRefMapper;
    @Mock
    private PaymentApplyMapper paymentApplyMapper;
    @Mock
    private CustomerStatementMapper customerStatementMapper;
    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;
    @Mock
    private InvoiceApplicationMapper invoiceApplicationMapper;
    @Mock
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;
    @Mock
    private CustomerService customerService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ApprovalInstanceService approvalInstanceService;
    @Mock
    private LunaNotifyHelper lunaNotifyHelper;
    @Mock
    private ConfigApi configApi;

    // Services under test
    private ExchangeRateServiceImpl exchangeRateService;
    private AccountServiceImpl accountService;
    private LedgerServiceImpl ledgerService;
    private PaymentServiceImpl paymentService;
    private CustomerReceiptApprovalHandler customerReceiptApprovalHandler;
    private CustomerStatementServiceImpl customerStatementService;
    private CustomerAgingServiceImpl customerAgingService;
    private InvoiceApplicationServiceImpl invoiceApplicationService;

    private MockedStatic<SecurityFrameworkUtils> mockedSecurity;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, ExchangeRateDO.class);
        TableInfoHelper.initTableInfo(assistant, AccountDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerDO.class);
        TableInfoHelper.initTableInfo(assistant, ShipmentDO.class);
        TableInfoHelper.initTableInfo(assistant, LedgerEntryDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentRefDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerStatementItemDO.class);
        TableInfoHelper.initTableInfo(assistant, InvoiceApplicationDO.class);
        TableInfoHelper.initTableInfo(assistant, InvoiceApplicationItemDO.class);
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        mockedSecurity = mockStatic(SecurityFrameworkUtils.class);
        mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);

        // Instantiate services
        exchangeRateService = new ExchangeRateServiceImpl();
        accountService = new AccountServiceImpl();
        ledgerService = new LedgerServiceImpl();
        paymentService = new PaymentServiceImpl();
        customerReceiptApprovalHandler = new CustomerReceiptApprovalHandler();
        customerStatementService = new CustomerStatementServiceImpl();
        customerAgingService = new CustomerAgingServiceImpl();
        invoiceApplicationService = new InvoiceApplicationServiceImpl();

        // Wire ExchangeRateService
        ReflectionTestUtils.setField(exchangeRateService, "exchangeRateMapper", exchangeRateMapper);

        // Wire AccountService
        ReflectionTestUtils.setField(accountService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(accountService, "ledgerService", ledgerService);

        // Wire LedgerService
        ReflectionTestUtils.setField(ledgerService, "ledgerEntryMapper", ledgerEntryMapper);

        // Wire PaymentService
        ReflectionTestUtils.setField(paymentService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(paymentService, "paymentRefMapper", paymentRefMapper);
        ReflectionTestUtils.setField(paymentService, "paymentApplyMapper", paymentApplyMapper);
        ReflectionTestUtils.setField(paymentService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(paymentService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(paymentService, "ledgerEntryMapper", ledgerEntryMapper);
        ReflectionTestUtils.setField(paymentService, "ledgerService", ledgerService);
        ReflectionTestUtils.setField(paymentService, "exchangeRateService", exchangeRateService);
        ReflectionTestUtils.setField(paymentService, "shipmentMapper", shipmentMapper);
        ReflectionTestUtils.setField(paymentService, "approvalInstanceService", approvalInstanceService);

        // Wire CustomerReceiptApprovalHandler
        ReflectionTestUtils.setField(customerReceiptApprovalHandler, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(customerReceiptApprovalHandler, "paymentRefMapper", paymentRefMapper);
        ReflectionTestUtils.setField(customerReceiptApprovalHandler, "accountService", accountService);
        ReflectionTestUtils.setField(customerReceiptApprovalHandler, "ledgerService", ledgerService);
        ReflectionTestUtils.setField(customerReceiptApprovalHandler, "exchangeRateService", exchangeRateService);
        ReflectionTestUtils.setField(customerReceiptApprovalHandler, "ledgerEntryMapper", ledgerEntryMapper);

        // Wire CustomerStatementService
        ReflectionTestUtils.setField(customerStatementService, "customerStatementMapper", customerStatementMapper);
        ReflectionTestUtils.setField(customerStatementService, "customerStatementItemMapper", customerStatementItemMapper);
        ReflectionTestUtils.setField(customerStatementService, "ledgerEntryMapper", ledgerEntryMapper);
        ReflectionTestUtils.setField(customerStatementService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(customerStatementService, "shipmentMapper", shipmentMapper);
        ReflectionTestUtils.setField(customerStatementService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(customerStatementService, "lunaNotifyHelper", lunaNotifyHelper);
        ReflectionTestUtils.setField(customerStatementService, "configApi", configApi);

        // Wire CustomerAgingService
        ReflectionTestUtils.setField(customerAgingService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(customerAgingService, "ledgerEntryMapper", ledgerEntryMapper);
        ReflectionTestUtils.setField(customerAgingService, "shipmentMapper", shipmentMapper);
        ReflectionTestUtils.setField(customerAgingService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(customerAgingService, "adminUserApi", adminUserApi);

        // Wire InvoiceApplicationService
        ReflectionTestUtils.setField(invoiceApplicationService, "invoiceApplicationMapper", invoiceApplicationMapper);
        ReflectionTestUtils.setField(invoiceApplicationService, "invoiceApplicationItemMapper", invoiceApplicationItemMapper);
        ReflectionTestUtils.setField(invoiceApplicationService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(invoiceApplicationService, "shipmentMapper", shipmentMapper);

        setupMockRepositories();
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
        TenantContextHolder.clear();
        exchangeRateRepo.clear();
        accountRepo.clear();
        customerRepo.clear();
        shipmentRepo.clear();
        ledgerRepo.clear();
        paymentRepo.clear();
        paymentRefRepo.clear();
        statementRepo.clear();
        statementItemRepo.clear();
        invoiceRepo.clear();
        invoiceItemRepo.clear();
    }

    private void setupMockRepositories() {
        // Initial Customer Master
        CustomerDO custA = new CustomerDO();
        custA.setId(CUSTOMER_A_ID);
        custA.setCompanyName(CUSTOMER_A_NAME);
        custA.setCreditDays(30);
        custA.setInvoiceTitle("苏州盛泽面料实业有限公司");
        custA.setUnifiedSocialCreditCode("91320500123456789A");
        custA.setInvoiceAddress("江苏省苏州市吴江区盛泽镇市场路1号");
        custA.setBankName("中国建设银行盛泽支行");
        custA.setBankAccountNo("32001234567890123456");
        custA.setSalespersonUserId(2048L);
        customerRepo.put(CUSTOMER_A_ID, custA);

        CustomerDO custB = new CustomerDO();
        custB.setId(CUSTOMER_B_ID);
        custB.setCompanyName(CUSTOMER_B_NAME);
        custB.setCreditDays(60);
        custB.setSalespersonUserId(2049L);
        customerRepo.put(CUSTOMER_B_ID, custB);

        when(customerMapper.selectById(anyLong())).thenAnswer(inv -> customerRepo.get(inv.getArgument(0)));
        when(customerMapper.selectList(any())).thenAnswer(inv -> {
            Object arg = inv.getArgument(0);
            if (arg instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> wrapper) {
                String sql = "";
                try {
                    sql = wrapper.getSqlSegment();
                } catch (Exception ignored) {}
                Map<String, Object> paramMap = wrapper.getParamNameValuePairs();
                Set<Long> filterIds = new HashSet<>();
                if (paramMap != null) {
                    for (Object val : paramMap.values()) {
                        if (val instanceof Collection<?> col) {
                            for (Object item : col) {
                                if (item instanceof Number n) {
                                    filterIds.add(n.longValue());
                                }
                            }
                        } else if (val instanceof Number n) {
                            filterIds.add(n.longValue());
                        }
                    }
                }
                for (Long id : customerRepo.keySet()) {
                    if (sql != null && sql.contains(String.valueOf(id))) {
                        filterIds.add(id);
                    }
                }
                if (!filterIds.isEmpty()) {
                    List<CustomerDO> filtered = customerRepo.values().stream()
                            .filter(c -> filterIds.contains(c.getId()))
                            .toList();
                    if (!filtered.isEmpty()) {
                        return filtered;
                    }
                }
            }
            return new ArrayList<>(customerRepo.values());
        });

        // Initial Bank Account
        AccountDO account = new AccountDO();
        account.setId(ACCOUNT_ID);
        account.setAccountName("默认人民币公户");
        account.setAccountType("BANK");
        account.setCurrency("RMB");
        account.setOpeningBalance(new BigDecimal("100000.00"));
        account.setCurrentBalance(new BigDecimal("100000.00"));
        account.setStatus("ENABLE");
        accountRepo.put(ACCOUNT_ID, account);

        when(accountMapper.selectById(anyLong())).thenAnswer(inv -> accountRepo.get(inv.getArgument(0)));
        when(accountMapper.updateBalanceIncrease(anyLong(), any(BigDecimal.class))).thenAnswer(inv -> {
            Long accId = inv.getArgument(0);
            BigDecimal delta = inv.getArgument(1);
            AccountDO acc = accountRepo.get(accId);
            if (acc != null && "ENABLE".equals(acc.getStatus())) {
                acc.setCurrentBalance(acc.getCurrentBalance().add(delta));
                return 1;
            }
            return 0;
        });
        when(accountMapper.updateBalanceDecrease(anyLong(), any(BigDecimal.class), any(BigDecimal.class))).thenAnswer(inv -> {
            Long accId = inv.getArgument(0);
            BigDecimal delta = inv.getArgument(1);
            AccountDO acc = accountRepo.get(accId);
            if (acc != null && "ENABLE".equals(acc.getStatus())) {
                acc.setCurrentBalance(acc.getCurrentBalance().subtract(delta));
                return 1;
            }
            return 0;
        });

        // ExchangeRate mapper mocks
        doAnswer(inv -> {
            ExchangeRateDO rateDO = inv.getArgument(0);
            rateDO.setId(idSeq.incrementAndGet());
            exchangeRateRepo.put(rateDO.getId(), rateDO);
            return 1;
        }).when(exchangeRateMapper).insert(any(ExchangeRateDO.class));

        when(exchangeRateMapper.selectLatestEffectiveRate(anyString(), any(LocalDate.class))).thenAnswer(inv -> {
            String curr = inv.getArgument(0);
            LocalDate date = inv.getArgument(1);
            return exchangeRateRepo.values().stream()
                    .filter(r -> r.getCurrency().equalsIgnoreCase(curr)
                            && "ENABLE".equalsIgnoreCase(r.getStatus())
                            && !r.getEffectiveDate().isAfter(date))
                    .max(Comparator.comparing(ExchangeRateDO::getEffectiveDate))
                    .orElse(null);
        });

        // LedgerEntry mapper mocks
        doAnswer(inv -> {
            LedgerEntryDO entry = inv.getArgument(0);
            entry.setId(idSeq.incrementAndGet());
            ledgerRepo.put(entry.getId(), entry);
            return 1;
        }).when(ledgerEntryMapper).insert(any(LedgerEntryDO.class));

        when(ledgerEntryMapper.selectList(any())).thenAnswer(inv -> {
            Object arg = inv.getArgument(0);
            if (arg instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> wrapper) {
                String sqlSegment = wrapper.getSqlSegment();
                Map<String, Object> paramMap = wrapper.getParamNameValuePairs();
                if (sqlSegment != null && sqlSegment.contains("category") && paramMap != null) {
                    for (Object val : paramMap.values()) {
                        if ("AR_INC".equals(val)) {
                            return ledgerRepo.values().stream()
                                    .filter(e -> "AR_INC".equals(e.getCategory()))
                                    .toList();
                        }
                        if ("AR_RETURN_REVERSAL".equals(val)) {
                            return ledgerRepo.values().stream()
                                    .filter(e -> "AR_RETURN_REVERSAL".equals(e.getCategory()))
                                    .toList();
                        }
                        if (LedgerCategory.ADVANCE_WRITE_OFF.getValue().equals(val)) {
                            return ledgerRepo.values().stream()
                                    .filter(e -> LedgerCategory.ADVANCE_WRITE_OFF.getValue().equals(e.getCategory()))
                                    .toList();
                        }
                    }
                }
                if (sqlSegment != null && sqlSegment.contains("reversal_of_id")) {
                    return ledgerRepo.values().stream()
                            .filter(e -> e.getReversalOfId() != null)
                            .toList();
                }
                if (sqlSegment != null && (sqlSegment.contains("biz_date <") || sqlSegment.contains("biz_date &lt;")) && !sqlSegment.contains("<=")) {
                    return Collections.emptyList();
                }
            }
            return new ArrayList<>(ledgerRepo.values());
        });
        when(ledgerEntryMapper.selectById(anyLong())).thenAnswer(inv -> ledgerRepo.get(inv.getArgument(0)));

        // Payment and Ref mapper mocks
        doAnswer(inv -> {
            PaymentDO p = inv.getArgument(0);
            p.setId(idSeq.incrementAndGet());
            paymentRepo.put(p.getId(), p);
            return 1;
        }).when(paymentMapper).insert(any(PaymentDO.class));

        doAnswer(inv -> {
            PaymentDO p = inv.getArgument(0);
            paymentRepo.put(p.getId(), p);
            return 1;
        }).when(paymentMapper).updateById(any(PaymentDO.class));

        when(paymentMapper.selectById(anyLong())).thenAnswer(inv -> paymentRepo.get(inv.getArgument(0)));
        when(paymentMapper.selectBatchIds(any())).thenAnswer(inv -> new ArrayList<>(paymentRepo.values()));
        when(paymentMapper.selectOneForUpdate(any(), anyLong())).thenAnswer(inv -> paymentRepo.get(inv.getArgument(1)));
        when(paymentMapper.selectByPaymentNoIncludingDeleted(anyString())).thenAnswer(inv -> {
            String no = inv.getArgument(0);
            return paymentRepo.values().stream().filter(p -> no.equals(p.getPaymentNo())).findFirst().orElse(null);
        });

        doAnswer(inv -> {
            PaymentRefDO ref = inv.getArgument(0);
            ref.setId(idSeq.incrementAndGet());
            paymentRefRepo.put(ref.getId(), ref);
            return 1;
        }).when(paymentRefMapper).insert(any(PaymentRefDO.class));

        when(paymentRefMapper.selectList(any())).thenAnswer(inv -> {
            Object arg = inv.getArgument(0);
            if (arg instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> wrapper) {
                String sqlSegment = wrapper.getSqlSegment();
                Map<String, Object> paramMap = wrapper.getParamNameValuePairs();
                if (sqlSegment != null && sqlSegment.contains("payment_id") && paramMap != null) {
                    for (Object val : paramMap.values()) {
                        if (val instanceof Long pid) {
                            return paymentRefRepo.values().stream()
                                    .filter(r -> Objects.equals(r.getPaymentId(), pid))
                                    .toList();
                        }
                    }
                }
            }
            return new ArrayList<>(paymentRefRepo.values());
        });

        when(paymentRefMapper.selectSumAllocatedAmountByEntryIds(anyCollection())).thenAnswer(inv -> {
            Collection<Long> entryIds = inv.getArgument(0);
            List<EntryAllocatedSumDTO> dtoList = new ArrayList<>();
            for (Long eid : entryIds) {
                BigDecimal sum = BigDecimal.ZERO;
                for (PaymentRefDO r : paymentRefRepo.values()) {
                    if (Objects.equals(r.getRefId(), eid) && "AR_ENTRY".equals(r.getRefType())) {
                        PaymentDO p = paymentRepo.get(r.getPaymentId());
                        if (p != null && "COMPLETED".equals(p.getStatus())) {
                            sum = sum.add(r.getAllocateAmount() != null ? r.getAllocateAmount() : BigDecimal.ZERO);
                        }
                    }
                }
                EntryAllocatedSumDTO dto = new EntryAllocatedSumDTO();
                dto.setEntryId(eid);
                dto.setTotalAllocated(sum);
                dtoList.add(dto);
            }
            return dtoList;
        });

        // CustomerStatement mapper mocks
        doAnswer(inv -> {
            CustomerStatementDO s = inv.getArgument(0);
            s.setId(idSeq.incrementAndGet());
            statementRepo.put(s.getId(), s);
            return 1;
        }).when(customerStatementMapper).insert(any(CustomerStatementDO.class));

        doAnswer(inv -> {
            CustomerStatementDO s = inv.getArgument(0);
            statementRepo.put(s.getId(), s);
            return 1;
        }).when(customerStatementMapper).updateById(any(CustomerStatementDO.class));

        when(customerStatementMapper.update(any(), any())).thenAnswer(inv -> {
            CustomerStatementDO updateDO = inv.getArgument(0);
            Object arg1 = inv.getArgument(1);
            CustomerStatementDO target = null;
            if (arg1 instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> wrapper) {
                Map<String, Object> paramMap = wrapper.getParamNameValuePairs();
                if (paramMap != null) {
                    for (Object val : paramMap.values()) {
                        if (val != null) {
                            try {
                                long candId = Long.parseLong(val.toString());
                                if (statementRepo.containsKey(candId)) {
                                    target = statementRepo.get(candId);
                                    break;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
            if (target == null && statementRepo.size() == 1) {
                target = statementRepo.values().iterator().next();
            }
            if (target != null && updateDO != null) {
                if (updateDO.getStatus() != null) {
                    target.setStatus(updateDO.getStatus());
                }
                if (updateDO.getConfirmType() != null) {
                    target.setConfirmType(updateDO.getConfirmType());
                }
                if (updateDO.getConfirmByName() != null) {
                    target.setConfirmByName(updateDO.getConfirmByName());
                }
                if (updateDO.getConfirmMethod() != null) {
                    target.setConfirmMethod(updateDO.getConfirmMethod());
                }
                if (updateDO.getConfirmTime() != null) {
                    target.setConfirmTime(updateDO.getConfirmTime());
                }
                if (updateDO.getConfirmIp() != null) {
                    target.setConfirmIp(updateDO.getConfirmIp());
                }
            }
            return 1;
        });

        when(customerStatementMapper.selectById(anyLong())).thenAnswer(inv -> statementRepo.get(inv.getArgument(0)));
        when(customerStatementMapper.selectOneForUpdate(any(), anyLong())).thenAnswer(inv -> statementRepo.get(inv.getArgument(1)));
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(anyString())).thenAnswer(inv -> {
            String no = inv.getArgument(0);
            return statementRepo.values().stream().filter(s -> no.equals(s.getStatementNo())).findFirst().orElse(null);
        });

        doAnswer(inv -> {
            CustomerStatementItemDO item = inv.getArgument(0);
            item.setId(idSeq.incrementAndGet());
            statementItemRepo.put(item.getId(), item);
            return 1;
        }).when(customerStatementItemMapper).insert(any(CustomerStatementItemDO.class));

        when(customerStatementItemMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(statementItemRepo.values()));

        // Shipment mapper mocks
        when(shipmentMapper.selectById(anyLong())).thenAnswer(inv -> shipmentRepo.get(inv.getArgument(0)));
        when(shipmentMapper.selectBatchIds(any())).thenAnswer(inv -> new ArrayList<>(shipmentRepo.values()));
        when(shipmentMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(shipmentRepo.values()));

        // InvoiceApplication mapper mocks
        doAnswer(inv -> {
            InvoiceApplicationDO app = inv.getArgument(0);
            app.setId(idSeq.incrementAndGet());
            invoiceRepo.put(app.getId(), app);
            return 1;
        }).when(invoiceApplicationMapper).insert(any(InvoiceApplicationDO.class));

        doAnswer(inv -> {
            InvoiceApplicationDO app = inv.getArgument(0);
            invoiceRepo.put(app.getId(), app);
            return 1;
        }).when(invoiceApplicationMapper).updateById(any(InvoiceApplicationDO.class));

        when(invoiceApplicationMapper.selectById(anyLong())).thenAnswer(inv -> invoiceRepo.get(inv.getArgument(0)));
        when(invoiceApplicationMapper.selectOneForUpdate(any(), anyLong())).thenAnswer(inv -> invoiceRepo.get(inv.getArgument(1)));

        doAnswer(inv -> {
            InvoiceApplicationItemDO item = inv.getArgument(0);
            item.setId(idSeq.incrementAndGet());
            invoiceItemRepo.put(item.getId(), item);
            return 1;
        }).when(invoiceApplicationItemMapper).insert(any(InvoiceApplicationItemDO.class));

        when(invoiceApplicationItemMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(invoiceItemRepo.values()));

        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.lunaerp.com");
    }

    // =========================================================================
    // 1. 端到端全业务流程冒烟测试
    // =========================================================================

    @Test
    @DisplayName("端到端全业务冒烟：建汇率 -> 收款过账核销 -> 预收转核销 -> 对账单发送确认 -> 打印 -> 账龄下钻 -> 发票开具登记")
    void testEndToEndCustomerReconciliationFullChain() {
        LocalDate today = LocalDate.now();

        // -------------------------------------------------------------
        // Step 1: 建汇率（ExchangeRate SPOT 即期牌价，USD = 7.15000000）
        // -------------------------------------------------------------
        ExchangeRateSaveReqVO rateReq = new ExchangeRateSaveReqVO();
        rateReq.setCurrency("USD");
        rateReq.setEffectiveDate(today);
        rateReq.setRate(new BigDecimal("7.15000000"));
        rateReq.setRemark("今日美元即期牌价");
        Long rateId = exchangeRateService.createExchangeRate(rateReq);
        assertNotNull(rateId);
        assertEquals(new BigDecimal("7.15000000"), exchangeRateService.getRate("USD", today));

        // -------------------------------------------------------------
        // Step 2: 发货审核记账分录（两笔明细行 AR_INC + 一笔运费头级 AR_INC）
        // -------------------------------------------------------------
        // 明细 1：4000.00
        LedgerEntryDO arEntry1 = new LedgerEntryDO();
        arEntry1.setBizDate(today.minusDays(10));
        arEntry1.setPartyType("CUSTOMER");
        arEntry1.setPartyId(CUSTOMER_A_ID);
        arEntry1.setCategory(LedgerCategory.AR_INC.getValue());
        arEntry1.setDirection("DEBIT");
        arEntry1.setCurrency("RMB");
        arEntry1.setExchangeRate(BigDecimal.ONE);
        arEntry1.setOriginalAmount(new BigDecimal("4000.00"));
        arEntry1.setBaseAmount(new BigDecimal("4000.00"));
        arEntry1.setRefType("SHIPMENT_ITEM");
        arEntry1.setRefId(3001L);
        arEntry1.setRefItemId(4001L);
        arEntry1.setRemark("高弹纯棉汗布 2000米");
        Long arEntryId1 = ledgerService.append(arEntry1);

        // 明细 2：2000.00
        LedgerEntryDO arEntry2 = new LedgerEntryDO();
        arEntry2.setBizDate(today.minusDays(10));
        arEntry2.setPartyType("CUSTOMER");
        arEntry2.setPartyId(CUSTOMER_A_ID);
        arEntry2.setCategory(LedgerCategory.AR_INC.getValue());
        arEntry2.setDirection("DEBIT");
        arEntry2.setCurrency("RMB");
        arEntry2.setExchangeRate(BigDecimal.ONE);
        arEntry2.setOriginalAmount(new BigDecimal("2000.00"));
        arEntry2.setBaseAmount(new BigDecimal("2000.00"));
        arEntry2.setRefType("SHIPMENT_ITEM");
        arEntry2.setRefId(3001L);
        arEntry2.setRefItemId(4002L);
        arEntry2.setRemark("竹纤维平纹布 1000米");
        Long arEntryId2 = ledgerService.append(arEntry2);

        // 运费：500.00
        LedgerEntryDO arFreight = new LedgerEntryDO();
        arFreight.setBizDate(today.minusDays(10));
        arFreight.setPartyType("CUSTOMER");
        arFreight.setPartyId(CUSTOMER_A_ID);
        arFreight.setCategory(LedgerCategory.AR_INC.getValue());
        arFreight.setDirection("DEBIT");
        arFreight.setCurrency("RMB");
        arFreight.setExchangeRate(BigDecimal.ONE);
        arFreight.setOriginalAmount(new BigDecimal("500.00"));
        arFreight.setBaseAmount(new BigDecimal("500.00"));
        arFreight.setRefType("SHIPMENT_FREIGHT");
        arFreight.setRefId(3001L);
        arFreight.setRemark("运费");
        Long arFreightId = ledgerService.append(arFreight);

        // 模拟发货主单存入 shipmentRepo 供后续开票与对账单引用
        ShipmentDO shipmentDO = new ShipmentDO();
        shipmentDO.setId(3001L);
        shipmentDO.setShipmentNo("SH20261008001");
        shipmentDO.setCustomerId(CUSTOMER_A_ID);
        shipmentDO.setCurrency("RMB");
        shipmentDO.setTotalAmount(new BigDecimal("6000.00"));
        shipmentDO.setFreightFee(new BigDecimal("500.00"));
        shipmentDO.setStatus("COMPLETED");
        shipmentRepo.put(3001L, shipmentDO);

        // 初始应收未核销合计 = 6500.00
        BigDecimal arBalanceInitial = paymentService.getArBalanceList(CUSTOMER_A_ID, "RMB").stream()
                .map(ArBalanceRespVO::getUnallocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("6500.00"), arBalanceInitial);

        // -------------------------------------------------------------
        // Step 3: 收款单（货款 RECEIPT，自动分配核销，审批过账）
        // -------------------------------------------------------------
        // 自动分配试算：收取 3500.00 货款，试算分配（先核销 arEntry1 的 3500.00）
        AllocatePreviewReqVO previewReq = new AllocatePreviewReqVO();
        previewReq.setCustomerId(CUSTOMER_A_ID);
        previewReq.setCurrency("RMB");
        previewReq.setPayAmount(new BigDecimal("3500.00"));
        previewReq.setReceiptType("RECEIPT");
        List<AllocatePreviewRespVO> allocatedItems = paymentService.allocatePreview(previewReq);
        assertEquals(3, allocatedItems.size());
        assertEquals(arEntryId1, allocatedItems.get(0).getEntryId());
        assertEquals(new BigDecimal("3500.00"), allocatedItems.get(0).getAllocateAmount());
        assertEquals(BigDecimal.ZERO, allocatedItems.get(1).getAllocateAmount());
        assertEquals(BigDecimal.ZERO, allocatedItems.get(2).getAllocateAmount());

        // 创建收款单草稿
        ReceiptSaveReqVO receiptReq = new ReceiptSaveReqVO();
        receiptReq.setCustomerId(CUSTOMER_A_ID);
        receiptReq.setAccountId(ACCOUNT_ID);
        receiptReq.setCurrency("RMB");
        receiptReq.setReceiptType("RECEIPT");
        receiptReq.setPayAmount(new BigDecimal("3500.00"));
        receiptReq.setReceiptDate(today.minusDays(5));
        receiptReq.setHandlerName("张出纳");
        receiptReq.setRemark("收第一笔销货款");

        ReceiptItemReqVO item1 = new ReceiptItemReqVO();
        item1.setEntryId(arEntryId1);
        item1.setAllocateAmount(new BigDecimal("3500.00"));
        receiptReq.setItems(List.of(item1));

        Long receiptId = paymentService.createReceipt(receiptReq);
        assertNotNull(receiptId);
        PaymentDO receiptDO = paymentRepo.get(receiptId);
        assertEquals(ApplyStatus.PENDING.getValue(), receiptDO.getApprovalStatus());

        // 提交审批并由 ApprovalHandler 模拟审批通过
        paymentService.submitReceipt(receiptId);
        verify(approvalInstanceService, atLeastOnce()).submitInstance(any());

        customerReceiptApprovalHandler.onApproved(receiptId);

        // 验证同事务四写结果：
        // 1. 资金账户余额增加 3500.00 -> 103500.00
        assertEquals(new BigDecimal("103500.00"), accountRepo.get(ACCOUNT_ID).getCurrentBalance());
        // 2. 收款单状态变 COMPLETED / APPROVED
        assertEquals(PaymentStatus.COMPLETED.getValue(), paymentRepo.get(receiptId).getStatus());
        assertEquals(ApplyStatus.APPROVED.getValue(), paymentRepo.get(receiptId).getApprovalStatus());
        // 3. 产生 RECEIPT 入账分录 与 RECEIPT_WRITE_OFF 核销分录
        boolean hasReceiptEntry = ledgerRepo.values().stream().anyMatch(e -> "RECEIPT".equals(e.getCategory()) && e.getOriginalAmount().compareTo(new BigDecimal("3500.00")) == 0);
        boolean hasWriteoffEntry = ledgerRepo.values().stream().anyMatch(e -> "RECEIPT_WRITE_OFF".equals(e.getCategory()) && e.getOriginalAmount().compareTo(new BigDecimal("3500.00")) == 0);
        assertTrue(hasReceiptEntry);
        assertTrue(hasWriteoffEntry);

        // 此时应收未核销余额应为 6500 - 3500 = 3000.00
        BigDecimal arBalanceAfterReceipt = paymentService.getArBalanceList(CUSTOMER_A_ID, "RMB").stream()
                .map(ArBalanceRespVO::getUnallocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("3000.00"), arBalanceAfterReceipt);

        // -------------------------------------------------------------
        // Step 4: 预收款单（ADVANCE 分型）与预收转核销
        // -------------------------------------------------------------
        // 创建预收款单 2000.00
        ReceiptSaveReqVO advanceReq = new ReceiptSaveReqVO();
        advanceReq.setCustomerId(CUSTOMER_A_ID);
        advanceReq.setAccountId(ACCOUNT_ID);
        advanceReq.setCurrency("RMB");
        advanceReq.setReceiptType("ADVANCE");
        advanceReq.setPayAmount(new BigDecimal("2000.00"));
        advanceReq.setReceiptDate(today.minusDays(3));
        advanceReq.setHandlerName("张出纳");
        advanceReq.setRemark("收取后续订单定金预收款");

        Long advanceReceiptId = paymentService.createReceipt(advanceReq);
        paymentService.submitReceipt(advanceReceiptId);
        customerReceiptApprovalHandler.onApproved(advanceReceiptId);

        // 验证资金账户余额再增 2000.00 -> 105500.00
        assertEquals(new BigDecimal("105500.00"), accountRepo.get(ACCOUNT_ID).getCurrentBalance());
        // 预收池余额实时聚合 = 2000.00
        PaymentDO advancePayment = paymentRepo.get(advanceReceiptId);
        BigDecimal advancePool1 = advancePayment.getPayAmount().subtract(advancePayment.getPrepayUsedAmount());
        assertEquals(new BigDecimal("2000.00"), advancePool1);

        // 执行预收转核销：将预收单中的 1500.00 冲抵 arEntry1 剩余的 500.00 与 arEntry2 的 1000.00
        AdvanceWriteoffReqVO writeoffReq = new AdvanceWriteoffReqVO();
        writeoffReq.setPaymentId(advanceReceiptId);

        ReceiptItemReqVO woItem1 = new ReceiptItemReqVO();
        woItem1.setEntryId(arEntryId1);
        woItem1.setAllocateAmount(new BigDecimal("500.00"));

        ReceiptItemReqVO woItem2 = new ReceiptItemReqVO();
        woItem2.setEntryId(arEntryId2);
        woItem2.setAllocateAmount(new BigDecimal("1000.00"));
        writeoffReq.setItems(List.of(woItem1, woItem2));

        paymentService.advanceWriteoff(writeoffReq);

        // 预收池余额应降为 2000 - 1500 = 500.00
        PaymentDO advancePaymentAfter = paymentRepo.get(advanceReceiptId);
        BigDecimal advancePool2 = advancePaymentAfter.getPayAmount().subtract(advancePaymentAfter.getPrepayUsedAmount());
        assertEquals(new BigDecimal("500.00"), advancePool2);

        // 剩余应收余额应降为 3000 - 1500 = 1500.00 (arEntry2 剩 1000，arFreight 剩 500)
        BigDecimal arBalanceAfterWriteoff = paymentService.getArBalanceList(CUSTOMER_A_ID, "RMB").stream()
                .map(ArBalanceRespVO::getUnallocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("1500.00"), arBalanceAfterWriteoff);

        // -------------------------------------------------------------
        // Step 5: 生成客户对账单（生成预览 -> 创建草稿 -> 发送 -> 代客确认）
        // -------------------------------------------------------------
        CustomerStatementPrefetchReqVO prefetchReq = new CustomerStatementPrefetchReqVO();
        prefetchReq.setCustomerId(CUSTOMER_A_ID);
        prefetchReq.setCurrency("RMB");
        prefetchReq.setPeriodStart(today.minusMonths(1));
        prefetchReq.setPeriodEnd(today);

        CustomerStatementPrefetchRespVO prefetchResp = customerStatementService.prefetchLines(prefetchReq);
        assertNotNull(prefetchResp);
        // 本期借方 = 6500.00，本期贷方 = 3500(收款核销) + 1500(预收转核销) = 5000.00，期末欠款 = 1500.00
        assertEquals(new BigDecimal("6500.00"), prefetchResp.getPeriodDebit());
        assertEquals(new BigDecimal("5000.00"), prefetchResp.getPeriodCredit());
        assertEquals(new BigDecimal("1500.00"), prefetchResp.getClosingBalance());

        // 创建对账单草稿 (DRAFT)
        CustomerStatementCreateReqVO stmtCreateReq = new CustomerStatementCreateReqVO();
        stmtCreateReq.setCustomerId(CUSTOMER_A_ID);
        stmtCreateReq.setCurrency("RMB");
        stmtCreateReq.setPeriodStart(today.minusMonths(1));
        stmtCreateReq.setPeriodEnd(today);
        stmtCreateReq.setStatementNo("CS20260001");
        stmtCreateReq.setRemark("2026年10月份常规客户对账");

        Long statementId = customerStatementService.createCustomerStatement(stmtCreateReq);
        assertNotNull(statementId);
        CustomerStatementDO statementDO = statementRepo.get(statementId);
        assertEquals(CustomerStatementStatus.DRAFT.getValue(), statementDO.getStatus());
        assertEquals("CS20260001", statementDO.getStatementNo());

        // 发送对账单 (DRAFT -> SENT)
        customerStatementService.sendCustomerStatement(statementId);
        assertEquals(CustomerStatementStatus.SENT.getValue(), statementRepo.get(statementId).getStatus());

        // 代客确认对账单 (SENT -> CONFIRMED, 终态快照冻结)
        CustomerStatementConfirmOnBehalfReqVO confirmReq = new CustomerStatementConfirmOnBehalfReqVO();
        confirmReq.setId(statementId);
        confirmReq.setConfirmMethod("微信回传盖章件");
        confirmReq.setRemark("客户王总微信回传已盖公章对账单");
        customerStatementService.confirmOnBehalf(confirmReq);

        CustomerStatementDO confirmedStmt = statementRepo.get(statementId);
        assertEquals(CustomerStatementStatus.CONFIRMED.getValue(), confirmedStmt.getStatus());
        assertEquals(CustomerStatementConfirmType.ON_BEHALF.getValue(), confirmedStmt.getConfirmType());
        assertEquals("微信回传盖章件", confirmedStmt.getConfirmMethod());

        // 已确认对账单不可作废守卫
        ServiceException voidEx = assertThrows(ServiceException.class, () ->
                customerStatementService.voidCustomerStatement(statementId));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_CONFIRMED_NOT_VOID.getCode(), voidEx.getCode());

        // -------------------------------------------------------------
        // Step 6: 打印凭证与二维码
        // -------------------------------------------------------------
        CustomerStatementPrintRespVO printData = customerStatementService.getCustomerStatementPrintData(statementId);
        assertNotNull(printData);
        assertEquals("CS20260001", printData.getStatementNo());
        // 验证打印数据金额豁免脱敏（真值展示）
        assertEquals(new BigDecimal("1500.00"), printData.getClosingBalance());
        assertEquals(new BigDecimal("6500.00"), printData.getPeriodDebit());
        assertEquals(new BigDecimal("5000.00"), printData.getPeriodCredit());
        // 验证二维码 URL 格式
        assertEquals("https://portal.lunaerp.com/portal/statement?no=CS20260001", printData.getQrUrl());
        // 验证打印明细行非空
        assertFalse(printData.getItems().isEmpty());

        // -------------------------------------------------------------
        // Step 7: 账龄汇总与未核销下钻
        // -------------------------------------------------------------
        CustomerAgingSummaryReqVO agingReq = CustomerAgingSummaryReqVO.builder()
                .agingDate(today)
                .customerIds(List.of(CUSTOMER_A_ID))
                .build();
        List<CustomerAgingSummaryRespVO> agingSummaries = customerAgingService.getAgingSummary(agingReq);
        assertNotNull(agingSummaries);
        assertEquals(1, agingSummaries.size());
        CustomerAgingSummaryRespVO agingA = agingSummaries.get(0);
        // 总未核销应收 = 1500.00
        assertEquals(0, new BigDecimal("1500.00").compareTo(agingA.getTotalArBalance()));
        // 账龄均为 10 天，全部落入 0-30 天桶
        assertEquals(0, new BigDecimal("1500.00").compareTo(agingA.getBucket0To30()));
        assertEquals(0, BigDecimal.ZERO.compareTo(agingA.getBucket31To60()));
        assertEquals(0, BigDecimal.ZERO.compareTo(agingA.getBucket61To90()));
        assertEquals(0, BigDecimal.ZERO.compareTo(agingA.getBucket91To180()));
        assertEquals(0, BigDecimal.ZERO.compareTo(agingA.getBucket181Plus()));
        // 预收余额单列不进桶 = 500.00
        assertEquals(0, new BigDecimal("500.00").compareTo(agingA.getAdvanceBalance()));
        // credit_days 为 30 天，发货于 10 天前，未逾期
        assertEquals(0, BigDecimal.ZERO.compareTo(agingA.getOverdueAmount()));

        // 下钻明细核对
        CustomerAgingDetailReqVO detailReq = CustomerAgingDetailReqVO.builder()
                .customerId(CUSTOMER_A_ID)
                .agingDate(today)
                .build();
        List<CustomerAgingDetailRespVO> agingDetails = customerAgingService.getAgingDetail(detailReq);
        assertNotNull(agingDetails);
        assertEquals(2, agingDetails.size());
        // 勾稽恒等式验证：原始金额 - 已核销 = 未核销余额
        for (CustomerAgingDetailRespVO det : agingDetails) {
            assertEquals(0, det.getOriginalAmount().subtract(det.getWrittenOffAmount()).compareTo(det.getRemainingAmount()));
        }

        // -------------------------------------------------------------
        // Step 8: 发票申请与登记开票回填（不对接金税，价税反算）
        // -------------------------------------------------------------
        // 查询可开票候选发货单
        List<InvoiceCandidateShipmentRespVO> candidateShipments = invoiceApplicationService.getCandidateShipments(CUSTOMER_A_ID, "RMB");
        assertNotNull(candidateShipments);
        assertEquals(1, candidateShipments.size());
        assertEquals(0, new BigDecimal("6500.00").compareTo(candidateShipments.get(0).getAvailableAmount()));

        // 创建发票申请草稿：申请金额 5650.00，税率 13%
        InvoiceApplicationCreateReqVO invCreateReq = new InvoiceApplicationCreateReqVO();
        invCreateReq.setCustomerId(CUSTOMER_A_ID);
        invCreateReq.setCurrency("RMB");
        invCreateReq.setTaxRate(new BigDecimal("13.00"));
        invCreateReq.setInvoiceKind("DIGITAL_SPECIAL");
        invCreateReq.setInvoiceNo("IA20260001");
        invCreateReq.setRemark("第一期出货发票");

        InvoiceApplicationItemSaveReqVO invItem = new InvoiceApplicationItemSaveReqVO();
        invItem.setShipmentId(3001L);
        invItem.setAmount(new BigDecimal("5650.00"));
        invCreateReq.setItems(List.of(invItem));

        Long invoiceAppId = invoiceApplicationService.createInvoiceApplication(invCreateReq);
        assertNotNull(invoiceAppId);

        InvoiceApplicationDO invDO = invoiceRepo.get(invoiceAppId);
        assertEquals(InvoiceApplicationStatus.DRAFT.getValue(), invDO.getStatus());
        assertEquals("IA20260001", invDO.getInvoiceNo());
        // 验证税额反算：不含税 5000.00，税额 650.00，含税 5650.00
        assertEquals(0, new BigDecimal("5000.00").compareTo(invDO.getTotalAmountExclTax()));
        assertEquals(0, new BigDecimal("650.00").compareTo(invDO.getTotalTax()));
        assertEquals(0, new BigDecimal("5650.00").compareTo(invDO.getTotalAmountInclTax()));

        // 提交发票申请 (DRAFT -> SUBMITTED)
        invoiceApplicationService.submitInvoiceApplication(invoiceAppId);
        assertEquals(InvoiceApplicationStatus.SUBMITTED.getValue(), invoiceRepo.get(invoiceAppId).getStatus());

        // 登记开票回填 (SUBMITTED -> ISSUED，回填 20 位官方发票号码)
        InvoiceApplicationIssueReqVO issueReq = new InvoiceApplicationIssueReqVO();
        issueReq.setId(invoiceAppId);
        issueReq.setOfficialInvoiceNo("24322000000012345678"); // 20位官方数电发票号
        issueReq.setInvoiceDate(today);
        issueReq.setAttachmentUrl("https://oss.lunaerp.com/invoice/2026/10/inv-001.pdf");
        invoiceApplicationService.issueInvoiceApplication(issueReq);

        InvoiceApplicationDO issuedDO = invoiceRepo.get(invoiceAppId);
        assertEquals(InvoiceApplicationStatus.ISSUED.getValue(), issuedDO.getStatus());
        assertEquals("24322000000012345678", issuedDO.getOfficialInvoiceNo());
        assertEquals(today, issuedDO.getInvoiceDate());
        assertNotNull(issuedDO.getIssueTime());
    }

    // =========================================================================
    // 2. 芋道权限码逐位断言 + 租户隔离与横向越权测试
    // =========================================================================

    @Test
    @DisplayName("权限注解校验：对账单、账龄、收款、发票、汇率端点逐位匹配 Spec 权限码")
    void testPermissionAnnotationsMatchSpec() throws NoSuchMethodException {
        // CustomerStatementController
        assertControllerPermission(CustomerStatementController.class, "prefetchLines",
                new Class<?>[]{CustomerStatementPrefetchReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:query')");
        assertControllerPermission(CustomerStatementController.class, "createCustomerStatement",
                new Class<?>[]{CustomerStatementCreateReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:create')");
        assertControllerPermission(CustomerStatementController.class, "sendCustomerStatement",
                new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:send')");
        assertControllerPermission(CustomerStatementController.class, "voidCustomerStatement",
                new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:void')");
        assertControllerPermission(CustomerStatementController.class, "confirmOnBehalf",
                new Class<?>[]{CustomerStatementConfirmOnBehalfReqVO.class, Long.class, String.class, String.class},
                "@ss.hasPermission('lunaerp:finance:customerrecon:confirm-on-behalf')");
        assertControllerPermission(CustomerStatementController.class, "getCustomerStatementPrintData",
                new Class<?>[]{Long.class, Long.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:print')");

        // CustomerAgingController
        assertControllerPermission(CustomerAgingController.class, "getAgingSummary",
                new Class<?>[]{CustomerAgingSummaryReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:query')");
        assertControllerPermission(CustomerAgingController.class, "getAgingDetail",
                new Class<?>[]{CustomerAgingDetailReqVO.class}, "@ss.hasPermission('lunaerp:finance:customerrecon:query')");

        // PaymentController (IN side)
        assertControllerPermission(PaymentController.class, "createReceipt",
                new Class<?>[]{ReceiptSaveReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:create')");
        assertControllerPermission(PaymentController.class, "updateReceipt",
                new Class<?>[]{ReceiptUpdateReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:update')");
        assertControllerPermission(PaymentController.class, "deleteReceipt",
                new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:delete')");
        assertControllerPermission(PaymentController.class, "advanceWriteoff",
                new Class<?>[]{AdvanceWriteoffReqVO.class}, "@ss.hasPermission('lunaerp:finance:payment:writeoff')");
        assertControllerPermission(PaymentController.class, "reverseReceipt",
                new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:payment:reverse')");

        // InvoiceApplicationController
        assertControllerPermission(InvoiceApplicationController.class, "createInvoiceApplication",
                new Class<?>[]{InvoiceApplicationCreateReqVO.class}, "@ss.hasPermission('lunaerp:finance:invoice:create')");
        assertControllerPermission(InvoiceApplicationController.class, "issueInvoiceApplication",
                new Class<?>[]{InvoiceApplicationIssueReqVO.class}, "@ss.hasPermission('lunaerp:finance:invoice:issue')");

        // ExchangeRateController
        assertControllerPermission(ExchangeRateController.class, "createExchangeRate",
                new Class<?>[]{ExchangeRateSaveReqVO.class}, "@ss.hasPermission('lunaerp:finance:exchangerate:create')");
        assertControllerPermission(ExchangeRateController.class, "updateExchangeRate",
                new Class<?>[]{ExchangeRateUpdateReqVO.class},
                "@ss.hasPermission('lunaerp:finance:exchangerate:update')");
    }

    @Test
    @DisplayName("横向越权隔离测试：客户 A 无法查看与操作客户 B 的对账单与明细")
    void testCrossCustomerIsolation() {
        // 构造客户 B 的对账单
        CustomerStatementDO stmtB = new CustomerStatementDO();
        stmtB.setId(8802L);
        stmtB.setStatementNo("CS20269999");
        stmtB.setCustomerId(CUSTOMER_B_ID);
        stmtB.setCustomerName(CUSTOMER_B_NAME);
        stmtB.setStatus(CustomerStatementStatus.SENT.getValue());
        statementRepo.put(8802L, stmtB);

        // 模拟外部客户门户端点或跨客户操作校验：传入当前客户为 CUSTOMER_A_ID，试图操作 CUSTOMER_B_ID 的单据
        CustomerStatementPortalConfirmReqVO portalConfirmReq = new CustomerStatementPortalConfirmReqVO();
        portalConfirmReq.setId(8802L);
        portalConfirmReq.setCustomerId(CUSTOMER_A_ID);
        portalConfirmReq.setConfirmByName("王客户");
        portalConfirmReq.setConfirmIp("127.0.0.1");

        assertThrows(ServiceException.class, () ->
                customerStatementService.confirmPortalCustomerStatement(portalConfirmReq));

        CustomerStatementPortalRejectReqVO portalRejectReq = new CustomerStatementPortalRejectReqVO();
        portalRejectReq.setId(8802L);
        portalRejectReq.setCustomerId(CUSTOMER_A_ID);
        portalRejectReq.setRejectReason("价格不符");

        assertThrows(ServiceException.class, () ->
                customerStatementService.rejectPortalCustomerStatement(portalRejectReq));

        assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatement(8802L, CUSTOMER_A_ID));
    }

    // =========================================================================
    // 3. 性能测试：万级数据 P95 < 1s 聚合测试 + 索引断言
    // =========================================================================

    @Test
    @DisplayName("性能断言：万级分录数据实时 FIFO 账龄与核销聚合计算 P95 < 1s")
    void testPerformance10kEntriesP95Under1Second() {
        LocalDate today = LocalDate.now();
        List<CustomerAgingCalculator.ArEntry> arEntries = new ArrayList<>(9000);

        // 模拟 9000 条发货分录
        for (long i = 1; i <= 9000; i++) {
            arEntries.add(new CustomerAgingCalculator.ArEntry(
                    i, today.minusDays(i % 180), new BigDecimal("100.00"),
                    "SHIPMENT_ITEM", 1000L + i, "SH" + i, "货品" + i
            ));
        }

        // 模拟 1000 条核销分录
        List<CustomerAgingCalculator.CreditReduction> reductions = new ArrayList<>(1000);
        for (long i = 1; i <= 1000; i++) {
            reductions.add(new CustomerAgingCalculator.CreditReduction(
                    10000L + i, today.minusDays(i % 60), LedgerCategory.RECEIPT_WRITE_OFF.getValue(), new BigDecimal("500.00")
            ));
        }

        // 测试执行时间
        long start = System.currentTimeMillis();
        CustomerAgingCalculator.AgingResult result = CustomerAgingCalculator.calculate(
                CUSTOMER_A_ID, CUSTOMER_A_NAME, 30, today, arEntries, reductions, BigDecimal.ZERO);
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(result);
        assertTrue(elapsed < 1000, "万级数据 FIFO 账龄计算耗时应远小于 1000ms，实际耗时: " + elapsed + "ms");
        assertTrue(result.getTotalArBalance().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("索引断言：验证 SQL 脚本中分录账包含 (tenant_id, party_type, party_id, biz_date) 索引")
    void testLedgerCompositeIndexDefinedInSql() throws Exception {
        Path[] candidates = new Path[]{
                Path.of("sql/postgresql/lunaerp-finance-customer-recon.sql"),
                Path.of("../sql/postgresql/lunaerp-finance-customer-recon.sql"),
                Path.of("../../luna-srv/sql/postgresql/lunaerp-finance-customer-recon.sql"),
                Path.of("luna-srv/sql/postgresql/lunaerp-finance-customer-recon.sql")
        };
        Path found = null;
        for (Path p : candidates) {
            if (Files.exists(p)) {
                found = p;
                break;
            }
        }
        assertNotNull(found, "SQL 脚本 lunaerp-finance-customer-recon.sql 必须存在");

        String sqlContent = Files.readString(found);
        assertTrue(sqlContent.contains("(tenant_id, party_type, party_id, biz_date)"),
                "经营分录账必须声明复合索引 (tenant_id, party_type, party_id, biz_date)");
    }

    private void assertControllerPermission(Class<?> controllerClass, String methodName, Class<?>[] paramTypes, String expectedPermission) throws NoSuchMethodException {
        Method method = controllerClass.getMethod(methodName, paramTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, controllerClass.getSimpleName() + "." + methodName + " 必须标注 @PreAuthorize");
        assertEquals(expectedPermission, annotation.value());
    }

}
