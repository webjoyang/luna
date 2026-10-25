package com.luna.lunasrv.module.lunaerp.finance.smoke;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.handler.ExpenseApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.handler.PayrollApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.AccountController;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.OtherTxnController;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.TransferController;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.finance.expense.ExpenseController;
import com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.OverviewController;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.PayrollController;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.OtherTxnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.TransferDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.AccountDirectionAmountDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.CategoryAmountDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionconfirmation.InspectionConfirmationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.OtherTxnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.TransferMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.*;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionconfirmation.InspectionConfirmationMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationResponsibilityEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationStatusEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.account.*;
import com.luna.lunasrv.module.lunaerp.service.finance.expense.*;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.overview.FinanceOverviewService;
import com.luna.lunasrv.module.lunaerp.service.finance.overview.FinanceOverviewServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.payroll.*;
import com.luna.lunasrv.module.lunaerp.support.TableInfoTestHelper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 资金、报销与工资全链路业务冒烟与越权矩阵测试（Issue 12 Capstone）
 *
 * <p>核心覆盖：
 * 1. 资金链（建户→期初修改及分录锁定期初→转账双边过账→其他收支→流水视图→收支总览）
 * 2. 报销链（建草稿→多行费用→审批驱动一步过账与余额扣减→A4套打数据）
 * 3. 工资链（建草稿→导次品扣款防重占用→审批通过→分批发放扣款→已发清派生）
 * 4. 经营毛利卡片（有 AR 数据与无 AR 数据平稳降级两态）
 * 5. 回收站进出与恢复语义（账户/报销/工资三类进出、恢复不补账、次品指纹防抢占）
 * 6. 租户隔离横向越权矩阵测试
 * 7. 纵向越权权限码规范逐端点核对矩阵
 * 8. 敏感字段 @Sensitive 脱敏注解与打印 VO 豁免审查
 * </p>
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TreasuryExpensePayrollFullChainSmokeTest {

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final Long LOGIN_USER_ID = 666L;
    private static final Long EMPLOYEE_USER_ID = 701L;

    // In-memory repositories
    private final Map<Long, AccountDO> accountRepo = new LinkedHashMap<>();
    private final Map<Long, TransferDO> transferRepo = new LinkedHashMap<>();
    private final Map<Long, OtherTxnDO> otherTxnRepo = new LinkedHashMap<>();
    private final Map<Long, LedgerEntryDO> ledgerRepo = new LinkedHashMap<>();
    private final Map<Long, ExpenseDO> expenseRepo = new LinkedHashMap<>();
    private final Map<Long, ExpenseItemDO> expenseItemRepo = new LinkedHashMap<>();
    private final Map<Long, PayrollDO> payrollRepo = new LinkedHashMap<>();
    private final Map<Long, PayrollEmployeeDO> payrollEmployeeRepo = new LinkedHashMap<>();
    private final Map<Long, PayrollLineItemDO> payrollLineItemRepo = new LinkedHashMap<>();
    private final Map<Long, PayrollDefectRefDO> payrollDefectRefRepo = new LinkedHashMap<>();
    private final Map<Long, PayrollPaymentDO> payrollPaymentRepo = new LinkedHashMap<>();
    private final Map<Long, InspectionConfirmationDO> confirmationRepo = new LinkedHashMap<>();

    private final AtomicLong idSeq = new AtomicLong(10000);

    // Mocks
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private TransferMapper transferMapper;
    @Mock
    private OtherTxnMapper otherTxnMapper;
    @Mock
    private LedgerEntryMapper ledgerEntryMapper;
    @Mock
    private ExpenseMapper expenseMapper;
    @Mock
    private ExpenseItemMapper expenseItemMapper;
    @Mock
    private PayrollMapper payrollMapper;
    @Mock
    private PayrollEmployeeMapper payrollEmployeeMapper;
    @Mock
    private PayrollLineItemMapper payrollLineItemMapper;
    @Mock
    private PayrollDefectRefMapper payrollDefectRefMapper;
    @Mock
    private PayrollPaymentMapper payrollPaymentMapper;
    @Mock
    private InspectionConfirmationMapper inspectionConfirmationMapper;
    @Mock
    private SupplierReconMapper supplierReconMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ApprovalInstanceService approvalInstanceService;
    @Mock
    private ExchangeRateService exchangeRateService;

    // Services under test
    private AccountServiceImpl accountService;
    private TransferServiceImpl transferService;
    private OtherTxnServiceImpl otherTxnService;
    private FinancePostingServiceImpl postingService;
    private LedgerServiceImpl ledgerService;
    private FinanceOverviewServiceImpl overviewService;
    private ExpenseServiceImpl expenseService;
    private ExpenseApprovalHandler expenseApprovalHandler;
    private ExpenseRecycleBinHandler expenseRecycleBinHandler;
    private PayrollServiceImpl payrollService;
    private PayrollDefectServiceImpl payrollDefectService;
    private PayrollApprovalHandler payrollApprovalHandler;
    private PayrollRecycleBinHandler payrollRecycleBinHandler;
    private AccountRecycleBinHandler accountRecycleBinHandler;

    // Controllers under test
    private AccountController accountController;
    private TransferController transferController;
    private OtherTxnController otherTxnController;
    private OverviewController overviewController;
    private ExpenseController expenseController;
    private PayrollController payrollController;

    private MockedStatic<SecurityFrameworkUtils> mockedSecurity;

    @BeforeAll
    static void initTableInfo() {
        TableInfoTestHelper.initAllEntities();
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        mockedSecurity = mockStatic(SecurityFrameworkUtils.class);
        mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);

        // Instantiate services
        accountService = new AccountServiceImpl();
        transferService = new TransferServiceImpl();
        otherTxnService = new OtherTxnServiceImpl();
        postingService = new FinancePostingServiceImpl();
        ledgerService = new LedgerServiceImpl();
        overviewService = new FinanceOverviewServiceImpl();
        expenseService = new ExpenseServiceImpl();
        expenseApprovalHandler = new ExpenseApprovalHandler();
        expenseRecycleBinHandler = new ExpenseRecycleBinHandler();
        payrollService = new PayrollServiceImpl();
        payrollDefectService = new PayrollDefectServiceImpl();
        payrollApprovalHandler = new PayrollApprovalHandler();
        payrollRecycleBinHandler = new PayrollRecycleBinHandler();
        accountRecycleBinHandler = new AccountRecycleBinHandler();

        // Wire ledgerService
        ReflectionTestUtils.setField(ledgerService, "ledgerEntryMapper", ledgerEntryMapper);
        ReflectionTestUtils.setField(ledgerService, "accountMapper", accountMapper);

        // Wire postingService
        ReflectionTestUtils.setField(postingService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(postingService, "ledgerService", ledgerService);

        // Wire accountService
        ReflectionTestUtils.setField(accountService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(accountService, "ledgerService", ledgerService);

        // Wire transferService
        ReflectionTestUtils.setField(transferService, "transferMapper", transferMapper);
        ReflectionTestUtils.setField(transferService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(transferService, "financePostingService", postingService);
        ReflectionTestUtils.setField(transferService, "exchangeRateService", exchangeRateService);

        // Wire otherTxnService
        ReflectionTestUtils.setField(otherTxnService, "otherTxnMapper", otherTxnMapper);
        ReflectionTestUtils.setField(otherTxnService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(otherTxnService, "financePostingService", postingService);
        ReflectionTestUtils.setField(otherTxnService, "exchangeRateService", exchangeRateService);

        // Wire overviewService
        ReflectionTestUtils.setField(overviewService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(overviewService, "ledgerEntryMapper", ledgerEntryMapper);
        ReflectionTestUtils.setField(overviewService, "supplierReconMapper", supplierReconMapper);

        // Wire expenseService
        ReflectionTestUtils.setField(expenseService, "expenseMapper", expenseMapper);
        ReflectionTestUtils.setField(expenseService, "expenseItemMapper", expenseItemMapper);
        ReflectionTestUtils.setField(expenseService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(expenseService, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(expenseService, "financePostingService", postingService);
        ReflectionTestUtils.setField(expenseService, "exchangeRateService", exchangeRateService);
        ReflectionTestUtils.setField(expenseService, "approvalInstanceService", approvalInstanceService);

        // Wire expenseApprovalHandler
        ReflectionTestUtils.setField(expenseApprovalHandler, "expenseService", expenseService);

        // Wire expenseRecycleBinHandler
        ReflectionTestUtils.setField(expenseRecycleBinHandler, "expenseMapper", expenseMapper);
        ReflectionTestUtils.setField(expenseRecycleBinHandler, "expenseItemMapper", expenseItemMapper);

        // Wire accountRecycleBinHandler
        ReflectionTestUtils.setField(accountRecycleBinHandler, "accountMapper", accountMapper);

        // Wire payrollDefectService
        ReflectionTestUtils.setField(payrollDefectService, "inspectionConfirmationMapper", inspectionConfirmationMapper);
        ReflectionTestUtils.setField(payrollDefectService, "payrollDefectRefMapper", payrollDefectRefMapper);
        ReflectionTestUtils.setField(payrollDefectService, "payrollLineItemMapper", payrollLineItemMapper);
        ReflectionTestUtils.setField(payrollDefectService, "payrollMapper", payrollMapper);
        ReflectionTestUtils.setField(payrollDefectService, "adminUserApi", adminUserApi);

        // Wire payrollService
        ReflectionTestUtils.setField(payrollService, "payrollMapper", payrollMapper);
        ReflectionTestUtils.setField(payrollService, "payrollEmployeeMapper", payrollEmployeeMapper);
        ReflectionTestUtils.setField(payrollService, "payrollLineItemMapper", payrollLineItemMapper);
        ReflectionTestUtils.setField(payrollService, "payrollDefectRefMapper", payrollDefectRefMapper);
        ReflectionTestUtils.setField(payrollService, "payrollPaymentMapper", payrollPaymentMapper);
        ReflectionTestUtils.setField(payrollService, "payrollDefectService", payrollDefectService);
        ReflectionTestUtils.setField(payrollService, "accountMapper", accountMapper);
        ReflectionTestUtils.setField(payrollService, "financePostingService", postingService);
        ReflectionTestUtils.setField(payrollService, "exchangeRateService", exchangeRateService);
        ReflectionTestUtils.setField(payrollService, "approvalInstanceService", approvalInstanceService);
        ReflectionTestUtils.setField(payrollService, "adminUserApi", adminUserApi);

        // Wire payrollApprovalHandler (empty implementation in production)

        // Wire payrollRecycleBinHandler
        ReflectionTestUtils.setField(payrollRecycleBinHandler, "payrollMapper", payrollMapper);
        ReflectionTestUtils.setField(payrollRecycleBinHandler, "payrollEmployeeMapper", payrollEmployeeMapper);
        ReflectionTestUtils.setField(payrollRecycleBinHandler, "payrollLineItemMapper", payrollLineItemMapper);
        ReflectionTestUtils.setField(payrollRecycleBinHandler, "payrollDefectRefMapper", payrollDefectRefMapper);
        ReflectionTestUtils.setField(payrollRecycleBinHandler, "payrollDefectService", payrollDefectService);

        // Instantiate and wire controllers
        accountController = new AccountController();
        ReflectionTestUtils.setField(accountController, "accountService", accountService);

        transferController = new TransferController();
        ReflectionTestUtils.setField(transferController, "transferService", transferService);

        otherTxnController = new OtherTxnController();
        ReflectionTestUtils.setField(otherTxnController, "otherTxnService", otherTxnService);

        overviewController = new OverviewController();
        ReflectionTestUtils.setField(overviewController, "overviewService", overviewService);

        expenseController = new ExpenseController();
        ReflectionTestUtils.setField(expenseController, "expenseService", expenseService);

        payrollController = new PayrollController();
        ReflectionTestUtils.setField(payrollController, "payrollService", payrollService);
        ReflectionTestUtils.setField(payrollController, "payrollDefectService", payrollDefectService);

        setupMockBehaviors();
    }

    @AfterEach
    void tearDown() {
        if (mockedSecurity != null) {
            mockedSecurity.close();
        }
        TenantContextHolder.clear();
        accountRepo.clear();
        transferRepo.clear();
        otherTxnRepo.clear();
        ledgerRepo.clear();
        expenseRepo.clear();
        expenseItemRepo.clear();
        payrollRepo.clear();
        payrollEmployeeRepo.clear();
        payrollLineItemRepo.clear();
        payrollDefectRefRepo.clear();
        payrollPaymentRepo.clear();
        confirmationRepo.clear();
    }

    private void setupMockBehaviors() {
        // Admin user mock
        AdminUserRespDTO adminUser = new AdminUserRespDTO();
        adminUser.setId(LOGIN_USER_ID);
        adminUser.setNickname("张三");
        when(adminUserApi.getUser(LOGIN_USER_ID)).thenReturn(adminUser);

        AdminUserRespDTO employeeUser = new AdminUserRespDTO();
        employeeUser.setId(EMPLOYEE_USER_ID);
        employeeUser.setNickname("李四");
        when(adminUserApi.getUser(EMPLOYEE_USER_ID)).thenReturn(employeeUser);
        when(adminUserApi.getUserList(anyCollection())).thenReturn(List.of(adminUser, employeeUser));
        Map<Long, AdminUserRespDTO> userMap = Map.of(LOGIN_USER_ID, adminUser, EMPLOYEE_USER_ID, employeeUser);
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(userMap);

        // ApprovalInstance mock
        when(approvalInstanceService.submitInstance(any(ApprovalInstanceSubmitReqVO.class))).thenReturn(8888L);

        // ExchangeRate mock
        lenient().when(exchangeRateService.getRate(any(), any())).thenReturn(BigDecimal.ONE);

        // AccountMapper mocks
        when(accountMapper.insert(any(AccountDO.class))).thenAnswer(inv -> {
            AccountDO a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(idSeq.incrementAndGet());
            }
            if (a.getTenantId() == null) {
                a.setTenantId(TenantContextHolder.getTenantId());
            }
            a.setDeleted(false);
            accountRepo.put(a.getId(), a);
            return 1;
        });
        when(accountMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            AccountDO a = accountRepo.get(id);
            if (a == null || Boolean.TRUE.equals(a.getDeleted())) {
                return null;
            }
            Long currentTenant = TenantContextHolder.getTenantId();
            if (currentTenant != null && !currentTenant.equals(a.getTenantId())) {
                return null; // Tenant isolation
            }
            return a;
        });
        when(accountMapper.updateById(any(AccountDO.class))).thenAnswer(inv -> {
            AccountDO a = inv.getArgument(0);
            accountRepo.put(a.getId(), a);
            return 1;
        });
        when(accountMapper.deleteById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            AccountDO a = accountRepo.get(id);
            if (a != null) {
                a.setDeleted(true);
            }
            return 1;
        });
        when(accountMapper.updateBalanceIncrease(anyLong(), any(BigDecimal.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            BigDecimal amount = inv.getArgument(1);
            AccountDO a = accountRepo.get(id);
            if (a == null || !"ENABLE".equals(a.getStatus())) {
                return 0;
            }
            BigDecimal cur = a.getCurrentBalance() != null ? a.getCurrentBalance() : BigDecimal.ZERO;
            a.setCurrentBalance(cur.add(amount));
            return 1;
        });
        when(accountMapper.updateBalanceDecrease(anyLong(), any(BigDecimal.class), any(BigDecimal.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            BigDecimal amountDelta = inv.getArgument(1);
            BigDecimal absAmount = inv.getArgument(2);
            AccountDO a = accountRepo.get(id);
            if (a == null || !"ENABLE".equals(a.getStatus())) {
                return 0;
            }
            BigDecimal cur = a.getCurrentBalance() != null ? a.getCurrentBalance() : BigDecimal.ZERO;
            if (cur.compareTo(absAmount) < 0) {
                return 0; // Balance not enough
            }
            a.setCurrentBalance(cur.add(amountDelta)); // amountDelta is negative
            return 1;
        });
        when(accountMapper.selectDeletedById(anyLong(), anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Long tenantId = inv.getArgument(1);
            AccountDO a = accountRepo.get(id);
            if (a != null && Boolean.TRUE.equals(a.getDeleted()) && Objects.equals(a.getTenantId(), tenantId)) {
                return a;
            }
            return null;
        });
        when(accountMapper.restoreAccount(anyLong(), anyLong(), anyString(), any(LocalDateTime.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Long tenantId = inv.getArgument(1);
            AccountDO a = accountRepo.get(id);
            if (a != null && Boolean.TRUE.equals(a.getDeleted()) && Objects.equals(a.getTenantId(), tenantId)) {
                a.setDeleted(false);
                return 1;
            }
            return 0;
        });
        when(accountMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Long currentTenant = TenantContextHolder.getTenantId();
            return accountRepo.values().stream()
                    .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                    .filter(a -> currentTenant == null || currentTenant.equals(a.getTenantId()))
                    .collect(Collectors.toList());
        });

        // TransferMapper mocks
        when(transferMapper.insert(any(TransferDO.class))).thenAnswer(inv -> {
            TransferDO t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(idSeq.incrementAndGet());
            }
            if (t.getTenantId() == null) {
                t.setTenantId(TenantContextHolder.getTenantId());
            }
            t.setDeleted(false);
            transferRepo.put(t.getId(), t);
            return 1;
        });
        when(transferMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            TransferDO t = transferRepo.get(id);
            if (t == null || Boolean.TRUE.equals(t.getDeleted())) {
                return null;
            }
            Long currentTenant = TenantContextHolder.getTenantId();
            if (currentTenant != null && !currentTenant.equals(t.getTenantId())) {
                return null;
            }
            return t;
        });

        // OtherTxnMapper mocks
        when(otherTxnMapper.insert(any(OtherTxnDO.class))).thenAnswer(inv -> {
            OtherTxnDO o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(idSeq.incrementAndGet());
            }
            if (o.getTenantId() == null) {
                o.setTenantId(TenantContextHolder.getTenantId());
            }
            o.setDeleted(false);
            otherTxnRepo.put(o.getId(), o);
            return 1;
        });
        when(otherTxnMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            OtherTxnDO o = otherTxnRepo.get(id);
            if (o == null || Boolean.TRUE.equals(o.getDeleted())) {
                return null;
            }
            Long currentTenant = TenantContextHolder.getTenantId();
            if (currentTenant != null && !currentTenant.equals(o.getTenantId())) {
                return null;
            }
            return o;
        });

        // LedgerEntryMapper mocks
        when(ledgerEntryMapper.insert(any(LedgerEntryDO.class))).thenAnswer(inv -> {
            LedgerEntryDO entry = inv.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(idSeq.incrementAndGet());
            }
            if (entry.getTenantId() == null) {
                entry.setTenantId(TenantContextHolder.getTenantId());
            }
            entry.setDeleted(false);
            ledgerRepo.put(entry.getId(), entry);
            return 1;
        });
        when(ledgerEntryMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Long currentTenant = TenantContextHolder.getTenantId();
            return ledgerRepo.values().stream()
                    .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                    .filter(e -> currentTenant == null || currentTenant.equals(e.getTenantId()))
                    .collect(Collectors.toList());
        });
        when(ledgerEntryMapper.selectCount(any(Wrapper.class))).thenAnswer(inv -> {
            Long currentTenant = TenantContextHolder.getTenantId();
            long count = ledgerRepo.values().stream()
                    .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                    .filter(e -> currentTenant == null || currentTenant.equals(e.getTenantId()))
                    .count();
            return count;
        });
        when(ledgerEntryMapper.selectCountByAccountId(anyLong())).thenAnswer(inv -> {
            Long accId = inv.getArgument(0);
            Long currentTenant = TenantContextHolder.getTenantId();
            return ledgerRepo.values().stream()
                    .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                    .filter(e -> currentTenant == null || currentTenant.equals(e.getTenantId()))
                    .filter(e -> Objects.equals(e.getAccountId(), accId))
                    .count();
        });

        // Overview Mapper mocks
        when(ledgerEntryMapper.selectAccountDirectionSummaryBeforeDate(any(), any())).thenReturn(Collections.emptyList());
        when(ledgerEntryMapper.selectAccountDirectionSummary(any(), any(), any())).thenAnswer(inv -> {
            Long accId = inv.getArgument(0);
            Map<String, BigDecimal> accDirSum = new HashMap<>();
            for (LedgerEntryDO e : ledgerRepo.values()) {
                if (e.getAccountId() != null && (accId == null || accId.equals(e.getAccountId()))) {
                    String key = e.getAccountId() + "_" + e.getDirection();
                    BigDecimal cur = accDirSum.getOrDefault(key, BigDecimal.ZERO);
                    accDirSum.put(key, cur.add(e.getOriginalAmount() != null ? e.getOriginalAmount() : BigDecimal.ZERO));
                }
            }
            List<AccountDirectionAmountDTO> dtoList = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> entry : accDirSum.entrySet()) {
                String[] parts = entry.getKey().split("_");
                AccountDirectionAmountDTO dto = new AccountDirectionAmountDTO();
                dto.setAccountId(Long.valueOf(parts[0]));
                dto.setDirection(parts[1]);
                dto.setTotalAmount(entry.getValue());
                dtoList.add(dto);
            }
            return dtoList;
        });
        when(ledgerEntryMapper.selectCategorySummary(any(), any(), any())).thenAnswer(inv -> {
            List<CategoryAmountDTO> list = new ArrayList<>();
            for (LedgerEntryDO e : ledgerRepo.values()) {
                CategoryAmountDTO dto = new CategoryAmountDTO();
                dto.setCategory(e.getCategory());
                dto.setDirection(e.getDirection());
                dto.setOriginalAmount(e.getOriginalAmount());
                dto.setBaseAmount(e.getBaseAmount());
                dto.setCount(1L);
                list.add(dto);
            }
            return list;
        });

        // ExpenseMapper mocks
        when(expenseMapper.insert(any(ExpenseDO.class))).thenAnswer(inv -> {
            ExpenseDO e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(idSeq.incrementAndGet());
            }
            if (e.getTenantId() == null) {
                e.setTenantId(TenantContextHolder.getTenantId());
            }
            e.setDeleted(false);
            expenseRepo.put(e.getId(), e);
            return 1;
        });
        when(expenseMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            ExpenseDO e = expenseRepo.get(id);
            if (e == null || Boolean.TRUE.equals(e.getDeleted())) {
                return null;
            }
            Long currentTenant = TenantContextHolder.getTenantId();
            if (currentTenant != null && !currentTenant.equals(e.getTenantId())) {
                return null;
            }
            return e;
        });
        when(expenseMapper.updateById(any(ExpenseDO.class))).thenAnswer(inv -> {
            ExpenseDO e = inv.getArgument(0);
            expenseRepo.put(e.getId(), e);
            return 1;
        });
        when(expenseMapper.deleteById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            ExpenseDO e = expenseRepo.get(id);
            if (e != null) {
                e.setDeleted(true);
            }
            return 1;
        });
        when(expenseMapper.selectDeletedById(anyLong(), anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Long tenantId = inv.getArgument(1);
            ExpenseDO e = expenseRepo.get(id);
            if (e != null && Boolean.TRUE.equals(e.getDeleted()) && Objects.equals(e.getTenantId(), tenantId)) {
                return e;
            }
            return null;
        });
        when(expenseMapper.restoreExpense(anyLong(), anyLong(), anyString(), any(LocalDateTime.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Long tenantId = inv.getArgument(1);
            ExpenseDO e = expenseRepo.get(id);
            if (e != null && Boolean.TRUE.equals(e.getDeleted()) && Objects.equals(e.getTenantId(), tenantId)) {
                e.setDeleted(false);
                return 1;
            }
            return 0;
        });
        when(expenseMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Long currentTenant = TenantContextHolder.getTenantId();
            return expenseRepo.values().stream()
                    .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                    .filter(e -> currentTenant == null || currentTenant.equals(e.getTenantId()))
                    .collect(Collectors.toList());
        });

        // ExpenseItemMapper mocks
        when(expenseItemMapper.insert(any(ExpenseItemDO.class))).thenAnswer(inv -> {
            ExpenseItemDO item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(idSeq.incrementAndGet());
            }
            item.setDeleted(false);
            expenseItemRepo.put(item.getId(), item);
            return 1;
        });
        when(expenseItemMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            return new ArrayList<>(expenseItemRepo.values());
        });

        // PayrollMapper mocks
        when(payrollMapper.insert(any(PayrollDO.class))).thenAnswer(inv -> {
            PayrollDO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(idSeq.incrementAndGet());
            }
            if (p.getTenantId() == null) {
                p.setTenantId(TenantContextHolder.getTenantId());
            }
            p.setDeleted(false);
            payrollRepo.put(p.getId(), p);
            return 1;
        });
        when(payrollMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            PayrollDO p = payrollRepo.get(id);
            if (p == null || Boolean.TRUE.equals(p.getDeleted())) {
                return null;
            }
            Long currentTenant = TenantContextHolder.getTenantId();
            if (currentTenant != null && !currentTenant.equals(p.getTenantId())) {
                return null;
            }
            return p;
        });
        when(payrollMapper.selectOneForUpdate(any(), anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(1);
            PayrollDO p = payrollRepo.get(id);
            if (p == null || Boolean.TRUE.equals(p.getDeleted())) {
                return null;
            }
            Long currentTenant = TenantContextHolder.getTenantId();
            if (currentTenant != null && !currentTenant.equals(p.getTenantId())) {
                return null;
            }
            return p;
        });
        when(payrollMapper.updateById(any(PayrollDO.class))).thenAnswer(inv -> {
            PayrollDO p = inv.getArgument(0);
            payrollRepo.put(p.getId(), p);
            return 1;
        });
        when(payrollMapper.deleteById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            PayrollDO p = payrollRepo.get(id);
            if (p != null) {
                p.setDeleted(true);
            }
            return 1;
        });
        when(payrollMapper.selectDeletedById(anyLong(), anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Long tenantId = inv.getArgument(1);
            PayrollDO p = payrollRepo.get(id);
            if (p != null && Boolean.TRUE.equals(p.getDeleted()) && Objects.equals(p.getTenantId(), tenantId)) {
                return p;
            }
            return null;
        });
        when(payrollMapper.restorePayroll(anyLong(), anyLong(), anyString(), any(LocalDateTime.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Long tenantId = inv.getArgument(1);
            PayrollDO p = payrollRepo.get(id);
            if (p != null && Boolean.TRUE.equals(p.getDeleted()) && Objects.equals(p.getTenantId(), tenantId)) {
                p.setDeleted(false);
                return 1;
            }
            return 0;
        });
        when(payrollMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(payrollMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Long currentTenant = TenantContextHolder.getTenantId();
            return payrollRepo.values().stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                    .filter(p -> currentTenant == null || currentTenant.equals(p.getTenantId()))
                    .collect(Collectors.toList());
        });

        // PayrollEmployeeMapper mocks
        when(payrollEmployeeMapper.insert(any(PayrollEmployeeDO.class))).thenAnswer(inv -> {
            PayrollEmployeeDO emp = inv.getArgument(0);
            if (emp.getId() == null) {
                emp.setId(idSeq.incrementAndGet());
            }
            emp.setDeleted(false);
            payrollEmployeeRepo.put(emp.getId(), emp);
            return 1;
        });
        when(payrollEmployeeMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            return new ArrayList<>(payrollEmployeeRepo.values());
        });
        when(payrollEmployeeMapper.delete(any(Wrapper.class))).thenAnswer(inv -> {
            payrollEmployeeRepo.clear();
            return 1;
        });

        // PayrollLineItemMapper mocks
        when(payrollLineItemMapper.insert(any(PayrollLineItemDO.class))).thenAnswer(inv -> {
            PayrollLineItemDO item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(idSeq.incrementAndGet());
            }
            item.setDeleted(false);
            payrollLineItemRepo.put(item.getId(), item);
            return 1;
        });
        when(payrollLineItemMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            return new ArrayList<>(payrollLineItemRepo.values());
        });
        when(payrollLineItemMapper.delete(any(Wrapper.class))).thenAnswer(inv -> {
            payrollLineItemRepo.clear();
            return 1;
        });

        // PayrollDefectRefMapper mocks
        when(payrollDefectRefMapper.insert(any(PayrollDefectRefDO.class))).thenAnswer(inv -> {
            PayrollDefectRefDO ref = inv.getArgument(0);
            if (ref.getId() == null) {
                ref.setId(idSeq.incrementAndGet());
            }
            if (ref.getTenantId() == null) {
                ref.setTenantId(TenantContextHolder.getTenantId());
            }
            ref.setDeleted(false);
            payrollDefectRefRepo.put(ref.getId(), ref);
            return 1;
        });
        when(payrollDefectRefMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            return payrollDefectRefRepo.values().stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                    .collect(Collectors.toList());
        });
        when(payrollDefectRefMapper.selectAllByPayrollIdIncludingDeleted(anyLong(), anyLong())).thenAnswer(inv -> {
            return new ArrayList<>(payrollDefectRefRepo.values());
        });
        when(payrollDefectRefMapper.delete(any(Wrapper.class))).thenAnswer(inv -> {
            payrollDefectRefRepo.clear();
            return 1;
        });

        // PayrollPaymentMapper mocks
        when(payrollPaymentMapper.insert(any(PayrollPaymentDO.class))).thenAnswer(inv -> {
            PayrollPaymentDO pp = inv.getArgument(0);
            if (pp.getId() == null) {
                pp.setId(idSeq.incrementAndGet());
            }
            pp.setDeleted(false);
            payrollPaymentRepo.put(pp.getId(), pp);
            return 1;
        });
        when(payrollPaymentMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            return new ArrayList<>(payrollPaymentRepo.values());
        });
        when(payrollPaymentMapper.selectCount(any(Wrapper.class))).thenAnswer(inv -> {
            return (long) payrollPaymentRepo.size();
        });
        when(payrollPaymentMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenAnswer(inv -> {
            IPage<PayrollPaymentDO> page = inv.getArgument(0);
            page.setRecords(new ArrayList<>(payrollPaymentRepo.values()));
            page.setTotal(payrollPaymentRepo.size());
            return page;
        });
    }

    // ==========================================
    // 1. 资金链完整闭环冒烟
    // ==========================================
    @Test
    @DisplayName("冒烟 1：资金链（建户→期初修改及分录锁定期初→转账双边过账→其他收支→流水视图→收支总览）")
    void testTreasuryChain_FullLifecycle() {
        // Step 1: 建公户与私户
        AccountSaveReqVO publicAccReq = new AccountSaveReqVO();
        publicAccReq.setAccountName("招商银行基本户");
        publicAccReq.setAccountType("BANK");
        publicAccReq.setAccountNo("6225880123456789");
        publicAccReq.setAccountNature("PUBLIC");
        publicAccReq.setCurrency("RMB");
        publicAccReq.setOpeningBalance(new BigDecimal("50000.00"));
        Long publicAccId = accountService.createAccount(publicAccReq);
        assertNotNull(publicAccId);

        AccountDO publicAcc = accountRepo.get(publicAccId);
        assertEquals(new BigDecimal("50000.00"), publicAcc.getCurrentBalance());
        assertEquals("PUBLIC", publicAcc.getAccountNature());

        AccountSaveReqVO privateAccReq = new AccountSaveReqVO();
        privateAccReq.setAccountName("张三支付宝经营私户");
        privateAccReq.setAccountType("ALIPAY");
        privateAccReq.setAccountNo("zhangsan@company.com");
        privateAccReq.setAccountNature("PRIVATE");
        privateAccReq.setCurrency("RMB");
        privateAccReq.setOpeningBalance(new BigDecimal("10000.00"));
        Long privateAccId = accountService.createAccount(privateAccReq);
        assertNotNull(privateAccId);

        // Step 2: 账户无分录时允许修改期初余额，且与 current_balance 同步
        AccountUpdateReqVO updateOpeningReq = new AccountUpdateReqVO();
        updateOpeningReq.setId(publicAccId);
        updateOpeningReq.setOpeningBalance(new BigDecimal("60000.00"));
        accountService.updateAccount(updateOpeningReq);
        assertEquals(new BigDecimal("60000.00"), accountRepo.get(publicAccId).getCurrentBalance());

        // Step 3: 转账测试（从公户转入私户 10,000 元）
        TransferCreateReqVO transferReq = new TransferCreateReqVO();
        transferReq.setFromAccountId(publicAccId);
        transferReq.setToAccountId(privateAccId);
        transferReq.setAmount(new BigDecimal("10000.00"));
        transferReq.setCurrency("RMB");
        transferReq.setTransferDate(LocalDate.now());
        transferReq.setRemark("公户向经营私户备用金划转");

        Long transferId = transferService.createTransfer(transferReq);
        assertNotNull(transferId);
        TransferDO transferDO = transferRepo.get(transferId);
        assertNotNull(transferDO);
        assertTrue(transferDO.getTransferNo().startsWith("ZZ"));

        // 验证双方余额物化条件更新
        assertEquals(new BigDecimal("50000.00"), accountRepo.get(publicAccId).getCurrentBalance());
        assertEquals(new BigDecimal("20000.00"), accountRepo.get(privateAccId).getCurrentBalance());

        // 验证同事务写入两条经营分录：TRANSFER_OUT (CREDIT) + TRANSFER_IN (DEBIT)
        List<LedgerEntryDO> transferEntries = ledgerRepo.values().stream()
                .filter(e -> "TRANSFER".equals(e.getRefType()))
                .collect(Collectors.toList());
        assertEquals(2, transferEntries.size());
        assertTrue(transferEntries.stream().anyMatch(e -> "CREDIT".equals(e.getDirection()) && e.getAccountId().equals(publicAccId)));
        assertTrue(transferEntries.stream().anyMatch(e -> "DEBIT".equals(e.getDirection()) && e.getAccountId().equals(privateAccId)));

        // Step 4: 产生分录后，再次修改期初余额应被锁定拦截抛 020_104
        AccountUpdateReqVO lockedOpeningReq = new AccountUpdateReqVO();
        lockedOpeningReq.setId(publicAccId);
        lockedOpeningReq.setOpeningBalance(new BigDecimal("70000.00"));
        ServiceException lockEx = assertThrows(ServiceException.class, () -> accountService.updateAccount(lockedOpeningReq));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_OPENING_BALANCE_IMMUTABLE.getCode(), lockEx.getCode());

        // Step 5: 其他收支单登记测试
        // 5.1 登记其他收入：废品收入 2,000 元 (DEBIT/WASTE_IN)
        OtherTxnCreateReqVO scrapIncomeReq = new OtherTxnCreateReqVO();
        scrapIncomeReq.setAccountId(publicAccId);
        scrapIncomeReq.setDirection("DEBIT");
        scrapIncomeReq.setCategory("WASTE_IN");
        scrapIncomeReq.setOriginalAmount(new BigDecimal("2000.00"));
        scrapIncomeReq.setTxnDate(LocalDate.now());
        scrapIncomeReq.setRemark("车间废边料处理收入");
        Long incomeId = otherTxnService.createOtherTxn(scrapIncomeReq);
        assertNotNull(incomeId);
        assertEquals(new BigDecimal("52000.00"), accountRepo.get(publicAccId).getCurrentBalance());

        // 5.2 登记其他支出：银行手续费 100 元 (CREDIT/FEE_OUT)
        OtherTxnCreateReqVO feeExpenseReq = new OtherTxnCreateReqVO();
        feeExpenseReq.setAccountId(publicAccId);
        feeExpenseReq.setDirection("CREDIT");
        feeExpenseReq.setCategory("FEE_OUT");
        feeExpenseReq.setOriginalAmount(new BigDecimal("100.00"));
        feeExpenseReq.setTxnDate(LocalDate.now());
        feeExpenseReq.setRemark("网银月费手续费");
        Long feeId = otherTxnService.createOtherTxn(feeExpenseReq);
        assertNotNull(feeId);
        assertEquals(new BigDecimal("51900.00"), accountRepo.get(publicAccId).getCurrentBalance());

        // 5.3 类别与方向不一致校验拦截（020_110）
        OtherTxnCreateReqVO mismatchReq = new OtherTxnCreateReqVO();
        mismatchReq.setAccountId(publicAccId);
        mismatchReq.setDirection("CREDIT");
        mismatchReq.setCategory("INTEREST_IN"); // 支出方向但填了收入类别
        mismatchReq.setOriginalAmount(new BigDecimal("500.00"));
        mismatchReq.setTxnDate(LocalDate.now());
        ServiceException mismatchEx = assertThrows(ServiceException.class, () -> otherTxnService.createOtherTxn(mismatchReq));
        assertEquals(FinanceErrorCodeConstants.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH.getCode(), mismatchEx.getCode());

        // Step 6: 收支总览查询验证
        OverviewSummaryReqVO summaryReq = new OverviewSummaryReqVO();
        summaryReq.setAccountId(publicAccId);
        OverviewSummaryRespVO summary = overviewService.getOverviewSummary(summaryReq);
        assertNotNull(summary);
        assertEquals(1, summary.getAccountList().size());
        AccountSummaryRespVO accSummary = summary.getAccountList().get(0);
        assertEquals(publicAccId, accSummary.getAccountId());
        assertEquals(new BigDecimal("51900.00"), accSummary.getCurrentBalance());

        // Step 7: 账户停用与删除守卫验证
        // 7.1 有分录的账户删除被拦截（020_103）
        ServiceException delEx = assertThrows(ServiceException.class, () -> accountService.deleteAccount(publicAccId));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN.getCode(), delEx.getCode());

        // 7.2 停用私户，停用后发起转出被拦截（020_101）
        AccountUpdateReqVO disableReq = new AccountUpdateReqVO();
        disableReq.setId(privateAccId);
        disableReq.setStatus("DISABLE");
        accountService.updateAccount(disableReq);
        assertEquals("DISABLE", accountRepo.get(privateAccId).getStatus());

        TransferCreateReqVO disabledTransferReq = new TransferCreateReqVO();
        disabledTransferReq.setFromAccountId(privateAccId);
        disabledTransferReq.setToAccountId(publicAccId);
        disabledTransferReq.setAmount(new BigDecimal("1000.00"));
        disabledTransferReq.setCurrency("RMB");
        disabledTransferReq.setTransferDate(LocalDate.now());
        ServiceException disabledEx = assertThrows(ServiceException.class, () -> transferService.createTransfer(disabledTransferReq));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode(), disabledEx.getCode());
    }

    // ==========================================
    // 2. 报销链完整闭环冒烟
    // ==========================================
    @Test
    @DisplayName("冒烟 2：报销链（建草稿→多行费用→审批驱动一步过账与余额扣减→A4套打数据）")
    void testExpenseChain_FullLifecycle() {
        // 前置准备资金账户
        AccountDO account = new AccountDO();
        account.setId(301L);
        account.setAccountName("招商银行付款专用户");
        account.setAccountType("BANK");
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        account.setCurrentBalance(new BigDecimal("20000.00"));
        account.setTenantId(TENANT_ID);
        account.setDeleted(false);
        accountRepo.put(account.getId(), account);

        // Step 1: 创建报销申请草稿
        ExpenseCreateReqVO createReq = new ExpenseCreateReqVO();
        createReq.setApplicantUserId(LOGIN_USER_ID);
        createReq.setAccountId(account.getId());
        createReq.setRemark("销售出差绍兴柯桥客户拜访");

        ExpenseItemSaveReqVO item1 = new ExpenseItemSaveReqVO();
        item1.setCategory("TRAVEL");
        item1.setOriginalAmount(new BigDecimal("1200.00"));
        item1.setOccurDate(LocalDate.now().minusDays(1));
        item1.setRemark("往返高铁票");

        ExpenseItemSaveReqVO item2 = new ExpenseItemSaveReqVO();
        item2.setCategory("ENTERTAIN");
        item2.setOriginalAmount(new BigDecimal("300.00"));
        item2.setOccurDate(LocalDate.now());
        item2.setRemark("客户工作餐");

        createReq.setItems(List.of(item1, item2));

        Long expenseId = expenseService.createExpense(createReq);
        assertNotNull(expenseId);
        ExpenseDO expense = expenseRepo.get(expenseId);
        assertNotNull(expense);
        assertTrue(expense.getExpenseNo().startsWith("BX"));
        assertEquals(ApplyStatus.PENDING.getValue(), expense.getApprovalStatus());
        assertEquals(new BigDecimal("1500.00"), expense.getTotalAmount());

        // Step 2: 草稿态获取套打数据被拦截（024_001）
        ServiceException printEx = assertThrows(ServiceException.class, () -> expenseService.getExpensePrintData(expenseId));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_STATUS_NOT_ALLOWED.getCode(), printEx.getCode());

        // Step 3: 模拟审批通过驱动一步过账
        expenseApprovalHandler.onApproved(expenseId);

        // 验证报销单状态流转为 APPROVED，记录过账时间
        ExpenseDO approvedExpense = expenseRepo.get(expenseId);
        assertEquals(ApplyStatus.APPROVED.getValue(), approvedExpense.getApprovalStatus());
        assertNotNull(approvedExpense.getApproveTime());

        // 验证资金账户扣款：20,000 - 1,500 = 18,500
        assertEquals(new BigDecimal("18500.00"), accountRepo.get(account.getId()).getCurrentBalance());

        // 验证经营分录账写入一条 OUT_EXPENSE (CREDIT) 分录
        List<LedgerEntryDO> expenseEntries = ledgerRepo.values().stream()
                .filter(e -> "EXPENSE".equals(e.getRefType()) && Objects.equals(e.getRefId(), expenseId))
                .collect(Collectors.toList());
        assertEquals(1, expenseEntries.size());
        LedgerEntryDO entry = expenseEntries.get(0);
        assertEquals("CREDIT", entry.getDirection());
        assertEquals("OUT_EXPENSE", entry.getCategory());
        assertEquals(new BigDecimal("1500.00"), entry.getOriginalAmount());
        assertEquals(expense.getExpenseNo(), entry.getRefNo());

        // Step 4: 终态成功获取 A4 套打数据
        ExpensePrintRespVO printData = expenseService.getExpensePrintData(expenseId);
        assertNotNull(printData);
        assertEquals("EXPENSE", printData.getBizType());
        assertEquals("员工费用报销单", printData.getTitle());
        assertEquals(new BigDecimal("1500.00"), printData.getTotalAmount());
        assertEquals(2, printData.getItems().size());
        assertNotNull(printData.getPrintTime());

        // Step 5: 账户余额不足时一步过账回滚拦截（023_004）
        ExpenseDO overExpense = new ExpenseDO();
        overExpense.setId(idSeq.incrementAndGet());
        overExpense.setExpenseNo("BX20261020999");
        overExpense.setAccountId(account.getId());
        overExpense.setCurrency("RMB");
        overExpense.setOriginalTotal(new BigDecimal("999999.00"));
        overExpense.setBaseTotal(new BigDecimal("999999.00"));
        overExpense.setTotalAmount(new BigDecimal("999999.00"));
        overExpense.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        overExpense.setTenantId(TENANT_ID);
        overExpense.setDeleted(false);
        expenseRepo.put(overExpense.getId(), overExpense);

        ServiceException overEx = assertThrows(ServiceException.class, () -> expenseApprovalHandler.onApproved(overExpense.getId()));
        assertEquals(FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode(), overEx.getCode());
    }

    // ==========================================
    // 3. 工资链完整闭环冒烟
    // ==========================================
    @Test
    @DisplayName("冒烟 3：工资链（建草稿→导次品扣款防重占用→审批通过→分批发放扣款→已发清派生）")
    void testPayrollChain_FullLifecycle() {
        // 前置准备资金账户
        AccountDO account = new AccountDO();
        account.setId(401L);
        account.setAccountName("工商银行代发工资专户");
        account.setAccountType("BANK");
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        account.setCurrentBalance(new BigDecimal("50000.00"));
        account.setTenantId(TENANT_ID);
        account.setDeleted(false);
        accountRepo.put(account.getId(), account);

        // 前置准备质检确认次品记录
        Long confirmationId = 9001L;
        InspectionConfirmationDO confirmation = new InspectionConfirmationDO();
        confirmation.setId(confirmationId);
        confirmation.setInspectionOrderCode("QC20261019001");
        confirmation.setRollNo("ROLL-001");
        confirmation.setGrade("C");
        confirmation.setWorkerUserId(EMPLOYEE_USER_ID);
        confirmation.setWorkerName("李四");
        confirmation.setResponsibility(InspectionConfirmationResponsibilityEnum.OPERATOR.getValue());
        confirmation.setStatus(InspectionConfirmationStatusEnum.CONFIRMED.getValue());
        confirmation.setConfirmedAt(LocalDateTime.now().minusDays(2));
        confirmation.setTenantId(TENANT_ID);
        confirmationRepo.put(confirmationId, confirmation);

        when(inspectionConfirmationMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            return new ArrayList<>(confirmationRepo.values());
        });
        when(inspectionConfirmationMapper.selectById(confirmationId)).thenReturn(confirmation);

        // Step 1: 创建工资单草稿
        PayrollCreateReqVO createReq = new PayrollCreateReqVO();
        createReq.setPeriod("2026-10");
        createReq.setRemark("2026年10月份织造车间工资");

        PayrollEmployeeSaveReqVO emp = new PayrollEmployeeSaveReqVO();
        emp.setUserId(EMPLOYEE_USER_ID);

        PayrollLineItemSaveReqVO lineBasic = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING")
                .itemType("BASE")
                .amount(new BigDecimal("5000.00"))
                .build();

        PayrollLineItemSaveReqVO linePiece = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING")
                .itemType("PIECE")
                .qty(new BigDecimal("100.00"))
                .unitPrice(new BigDecimal("20.00"))
                .amount(new BigDecimal("2000.00")) // 100 * 20 = 2000
                .build();

        emp.setItems(List.of(lineBasic, linePiece));
        createReq.setEmployees(List.of(emp));

        Long payrollId = payrollService.createPayroll(createReq);
        assertNotNull(payrollId);
        PayrollDO payroll = payrollRepo.get(payrollId);
        assertNotNull(payroll);
        assertTrue(payroll.getPayrollNo().startsWith("GZ"));
        assertEquals("2026-10", payroll.getPeriod());
        assertEquals(new BigDecimal("7000.00"), payroll.getNetAmount());

        // Step 2: 预览次品扣款并导入更新
        PayrollDefectPreviewRespVO preview = payrollDefectService.getDefectPreview("2026-10", payrollId);
        assertNotNull(preview);
        assertEquals(1, preview.getGroups().size());

        PayrollDefectRefSaveReqVO refReq = PayrollDefectRefSaveReqVO.builder()
                .userId(EMPLOYEE_USER_ID)
                .nickname("李四")
                .confirmationId(confirmationId)
                .confirmationNo("QC20261019001")
                .rollNo("ROLL-001")
                .grade("C")
                .confirmedAt(LocalDateTime.now().minusDays(2))
                .deductPrice(new BigDecimal("50.00"))
                .build();

        PayrollLineItemSaveReqVO lineDefect = PayrollLineItemSaveReqVO.builder()
                .itemKind("DEDUCTION")
                .itemType("DEFECT")
                .qty(new BigDecimal("4.00"))
                .unitPrice(new BigDecimal("50.00"))
                .amount(new BigDecimal("200.00"))
                .defectRefs(List.of(refReq))
                .build();

        emp.setItems(List.of(lineBasic, linePiece, lineDefect));
        PayrollUpdateReqVO updateReq = PayrollUpdateReqVO.builder()
                .id(payrollId)
                .employees(List.of(emp))
                .build();
        payrollService.updatePayroll(updateReq);

        // 验证重算后实发工资：7,000 - 200 = 6,800
        PayrollDO payrollAfterDefect = payrollRepo.get(payrollId);
        assertEquals(new BigDecimal("6800.00"), payrollAfterDefect.getNetAmount());
        assertEquals(new BigDecimal("200.00"), payrollAfterDefect.getDeductionTotal());

        // 验证次品防重检测：其他单据尝试引用已占用的 confirmationId 抛 025_005
        Long otherPayrollId = idSeq.incrementAndGet();
        ServiceException duplicateDefectEx = assertThrows(ServiceException.class,
                () -> payrollDefectService.validateDefectRefsAvailability(otherPayrollId, List.of(confirmationId)));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_DEFECT_ALREADY_REFERENCED.getCode(), duplicateDefectEx.getCode());

        // Step 3: 审批通过（PayrollApprovalHandler 仅更新状态，零资金记账）
        payrollService.submitPayroll(payrollId);
        payrollRepo.get(payrollId).setApprovalStatus(ApplyStatus.APPROVED.getValue());
        payrollApprovalHandler.onApproved(payrollId);
        PayrollDO approvedPayroll = payrollRepo.get(payrollId);
        assertEquals(ApplyStatus.APPROVED.getValue(), approvedPayroll.getApprovalStatus());

        // Step 4: 出纳分批发放登记
        // 4.1 第一批部分发放：3,000 元
        PayrollPaymentRegisterReqVO batch1Req = PayrollPaymentRegisterReqVO.builder()
                .payrollId(payrollId)
                .accountId(account.getId())
                .amount(new BigDecimal("3000.00"))
                .payDate(LocalDate.now())
                .remark("第一批银行代发")
                .build();
        Long payment1Id = payrollService.registerPayrollPayment(batch1Req);
        assertNotNull(payment1Id);

        // 验证资金账户扣款与分录账写入：50,000 - 3,000 = 47,000
        assertEquals(new BigDecimal("47000.00"), accountRepo.get(account.getId()).getCurrentBalance());
        List<LedgerEntryDO> batch1Entries = ledgerRepo.values().stream()
                .filter(e -> "PAYROLL_PAYMENT".equals(e.getRefType()) && Objects.equals(e.getRefId(), payment1Id))
                .collect(Collectors.toList());
        assertEquals(1, batch1Entries.size());
        assertEquals("CREDIT", batch1Entries.get(0).getDirection());
        assertEquals("OUT_SALARY", batch1Entries.get(0).getCategory());
        assertEquals(new BigDecimal("3000.00"), batch1Entries.get(0).getOriginalAmount());

        // 4.2 已有发放记录时尝试作废工资单被拦截（025_006）
        ServiceException cancelEx = assertThrows(ServiceException.class, () -> payrollService.cancelPayroll(payrollId, "需要重新核算"));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_HAS_PAYMENT_NO_CANCEL.getCode(), cancelEx.getCode());

        // 4.3 超出剩余实发金额发放拦截（025_007）：剩余 3,800，尝试发 4,000
        PayrollPaymentRegisterReqVO overBatchReq = PayrollPaymentRegisterReqVO.builder()
                .payrollId(payrollId)
                .accountId(account.getId())
                .amount(new BigDecimal("4000.00"))
                .payDate(LocalDate.now())
                .build();
        ServiceException overPayEx = assertThrows(ServiceException.class, () -> payrollService.registerPayrollPayment(overBatchReq));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_PAYMENT_AMOUNT_EXCEED.getCode(), overPayEx.getCode());

        // 4.4 第二批结清发放：剩余 3,800 元全部发清
        PayrollPaymentRegisterReqVO batch2Req = PayrollPaymentRegisterReqVO.builder()
                .payrollId(payrollId)
                .accountId(account.getId())
                .amount(new BigDecimal("3800.00"))
                .payDate(LocalDate.now())
                .remark("第二批现金尾款发放")
                .build();
        Long payment2Id = payrollService.registerPayrollPayment(batch2Req);
        assertNotNull(payment2Id);

        // 验证账户余额：47,000 - 3,800 = 43,200
        assertEquals(new BigDecimal("43200.00"), accountRepo.get(account.getId()).getCurrentBalance());

        // 验证派生状态已发清 (PAID)
        PayrollRespVO finalPayrollVO = payrollService.getPayroll(payrollId);
        assertEquals("PAID", finalPayrollVO.getPaymentStatus());
        assertEquals(new BigDecimal("6800.00"), finalPayrollVO.getPaidAmount());

        // Step 5: A4 套打数据获取
        PayrollPrintRespVO printData = payrollService.getPayrollPrintData(payrollId);
        assertNotNull(printData);
        assertEquals("PAYROLL", printData.getBizType());
        assertEquals("2026-10 工资表", printData.getTitle());
        assertEquals(new BigDecimal("7000.00"), printData.getEarningTotal());
        assertEquals(new BigDecimal("200.00"), printData.getDeductionTotal());
        assertEquals(new BigDecimal("6800.00"), printData.getNetAmount());
    }

    // ==========================================
    // 4. 经营毛利卡片两态冒烟
    // ==========================================
    @Test
    @DisplayName("冒烟 4：经营毛利参考卡片（有 AR 数据与无 AR 数据平稳降级两态）")
    void testGrossMarginCard_BothStates() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        // 状态 A：无 AR 数据状态（分录账中无发货应收分录）
        when(ledgerEntryMapper.selectCountArEntriesByBizDateRange(any(), any())).thenReturn(0L);
        when(supplierReconMapper.selectSumConfirmedTotalAmount(any(), any())).thenReturn(new BigDecimal("20000.00"));

        GrossMarginReqVO req = new GrossMarginReqVO();
        req.setPeriodStart(start);
        req.setPeriodEnd(end);

        GrossMarginRespVO emptyArResp = overviewService.getGrossMargin(req);
        assertNotNull(emptyArResp);
        assertEquals(BigDecimal.ZERO.setScale(2), emptyArResp.getRevenue());
        assertEquals(new BigDecimal("20000.00"), emptyArResp.getCost());
        assertEquals(new BigDecimal("-20000.00"), emptyArResp.getGrossProfit());
        assertEquals(BigDecimal.ZERO.setScale(4), emptyArResp.getGrossMarginRate());
        assertEquals(FinanceOverviewServiceImpl.EMPTY_AR_NOTICE, emptyArResp.getEmptyStateNotice());
        assertEquals(FinanceOverviewServiceImpl.DISCLAIMER_TEXT, emptyArResp.getDisclaimer());

        // 状态 B：有 AR 数据状态（应收分录：AR_INC 100,000 元，AR_INC_REVERSAL 10,000 元，净额 90,000 元）
        when(ledgerEntryMapper.selectCountArEntriesByBizDateRange(any(), any())).thenReturn(5L);
        when(ledgerEntryMapper.selectSumBaseAmountByCategoryAndBizDateRange(eq(LedgerCategory.AR_INC.getValue()), any(), any()))
                .thenReturn(new BigDecimal("100000.00"));
        when(ledgerEntryMapper.selectSumBaseAmountByCategoryAndBizDateRange(eq(LedgerCategory.AR_INC_REVERSAL.getValue()), any(), any()))
                .thenReturn(new BigDecimal("10000.00"));
        when(supplierReconMapper.selectSumConfirmedTotalAmount(any(), any())).thenReturn(new BigDecimal("45000.00"));

        GrossMarginRespVO withArResp = overviewService.getGrossMargin(req);
        assertNotNull(withArResp);
        assertEquals(new BigDecimal("90000.00"), withArResp.getRevenue());
        assertEquals(new BigDecimal("45000.00"), withArResp.getCost());
        assertEquals(new BigDecimal("45000.00"), withArResp.getGrossProfit());
        // 毛利率：45,000 / 90,000 = 0.5000
        assertEquals(new BigDecimal("0.5000"), withArResp.getGrossMarginRate());
        assertNull(withArResp.getEmptyStateNotice());
        assertEquals(FinanceOverviewServiceImpl.DISCLAIMER_TEXT, withArResp.getDisclaimer());
    }

    // ==========================================
    // 5. 回收站进出与恢复语义
    // ==========================================
    @Test
    @DisplayName("冒烟 5：回收站进出与恢复语义（账户/报销/工资三类进出、恢复不补账、次品指纹防抢占）")
    void testRecycleBinSemantics_AllThreeTypes() {
        // 1. 资金账户回收站：仅无分录账户可进，恢复保持原状态
        AccountDO account = new AccountDO();
        account.setId(501L);
        account.setAccountName("临时测试现金户");
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        account.setDeleted(false);
        account.setTenantId(TENANT_ID);
        accountRepo.put(account.getId(), account);

        // 软删除进入回收站
        accountService.deleteAccount(account.getId());
        assertTrue(accountRepo.get(account.getId()).getDeleted());

        // 恢复
        accountRecycleBinHandler.restore(account.getId());
        assertFalse(accountRepo.get(account.getId()).getDeleted());
        assertEquals("ENABLE", accountRepo.get(account.getId()).getStatus());

        // 2. 报销单回收站：草稿报销软删，恢复为 PENDING 状态（恢复不补账）
        ExpenseDO expense = new ExpenseDO();
        expense.setId(502L);
        expense.setExpenseNo("BX20261020055");
        expense.setApprovalStatus(ApplyStatus.PENDING.getValue());
        expense.setDeleted(false);
        expense.setTenantId(TENANT_ID);
        expenseRepo.put(expense.getId(), expense);

        expenseService.deleteExpense(expense.getId());
        assertTrue(expenseRepo.get(expense.getId()).getDeleted());

        expenseRecycleBinHandler.restore(expense.getId());
        assertFalse(expenseRepo.get(expense.getId()).getDeleted());
        assertEquals(ApplyStatus.PENDING.getValue(), expenseRepo.get(expense.getId()).getApprovalStatus());

        // 3. 工资单回收站：草稿工资单软删随之释放次品占用；恢复重新占用次品指纹
        Long payrollId = 503L;
        PayrollDO payroll = new PayrollDO();
        payroll.setId(payrollId);
        payroll.setPayrollNo("GZ20261020055");
        payroll.setPeriod("2026-10");
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        payroll.setDeleted(false);
        payroll.setTenantId(TENANT_ID);
        payrollRepo.put(payrollId, payroll);

        PayrollDefectRefDO refDO = new PayrollDefectRefDO();
        refDO.setId(601L);
        refDO.setLineItemId(2001L);
        refDO.setConfirmationId(9999L);
        refDO.setTenantId(TENANT_ID);
        refDO.setDeleted(false);
        payrollDefectRefRepo.put(refDO.getId(), refDO);

        // 软删草稿工资单
        payrollService.deletePayroll(payrollId);
        assertTrue(payrollRepo.get(payrollId).getDeleted());

        // 恢复前校验次品可用性
        payrollRecycleBinHandler.validateRestore(payrollId);

        // 恢复草稿工资单
        payrollRecycleBinHandler.restore(payrollId);
        assertFalse(payrollRepo.get(payrollId).getDeleted());
    }

    // ==========================================
    // 6. 租户隔离横向越权矩阵
    // ==========================================
    @Test
    @DisplayName("越权矩阵 1：租户隔离横向越权（切换至他租户查询/修改/删除报不存在）")
    void testTenantIsolation_HorizontalUnauthorized() {
        // 在租户 1 创建资金账户、报销单与工资单
        AccountDO account = new AccountDO();
        account.setId(701L);
        account.setAccountName("租户1私有账户");
        account.setStatus("ENABLE");
        account.setCurrency("RMB");
        account.setTenantId(TENANT_ID);
        account.setDeleted(false);
        accountRepo.put(account.getId(), account);

        ExpenseDO expense = new ExpenseDO();
        expense.setId(702L);
        expense.setExpenseNo("BX_TENANT_1");
        expense.setApprovalStatus(ApplyStatus.PENDING.getValue());
        expense.setTenantId(TENANT_ID);
        expense.setDeleted(false);
        expenseRepo.put(expense.getId(), expense);

        PayrollDO payroll = new PayrollDO();
        payroll.setId(703L);
        payroll.setPayrollNo("GZ_TENANT_1");
        payroll.setPeriod("2026-10");
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        payroll.setTenantId(TENANT_ID);
        payroll.setDeleted(false);
        payrollRepo.put(payroll.getId(), payroll);

        // 切换租户至 OTHER_TENANT_ID
        TenantContextHolder.setTenantId(OTHER_TENANT_ID);

        // 横向越权查询资金账户 -> 020_100
        ServiceException accEx = assertThrows(ServiceException.class, () -> accountService.getAccount(account.getId()));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), accEx.getCode());

        // 横向越权删除资金账户 -> 020_100
        ServiceException accDelEx = assertThrows(ServiceException.class, () -> accountService.deleteAccount(account.getId()));
        assertEquals(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode(), accDelEx.getCode());

        // 横向越权查询报销单 -> 024_000
        ServiceException expEx = assertThrows(ServiceException.class, () -> expenseService.getExpense(expense.getId()));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_NOT_EXISTS.getCode(), expEx.getCode());

        // 横向越权查询工资单 -> 025_000
        ServiceException payEx = assertThrows(ServiceException.class, () -> payrollService.getPayroll(payroll.getId()));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_NOT_EXISTS.getCode(), payEx.getCode());
    }

    // ==========================================
    // 7. 纵向越权权限码规范逐端点核对矩阵
    // ==========================================
    @Test
    @DisplayName("越权矩阵 2：纵向越权权限码规范逐端点审查（Controller 全部方法必须标注 @PreAuthorize 且以 lunaerp:finance: 开头）")
    void testVerticalAuthorization_PreAuthorizeMatrix() {
        Class<?>[] controllerClasses = new Class<?>[] {
                AccountController.class,
                TransferController.class,
                OtherTxnController.class,
                OverviewController.class,
                ExpenseController.class,
                PayrollController.class
        };

        for (Class<?> clazz : controllerClasses) {
            for (Method method : clazz.getDeclaredMethods()) {
                // 仅审查暴露的 Web 映射方法
                boolean isMappingMethod = method.isAnnotationPresent(GetMapping.class)
                        || method.isAnnotationPresent(PostMapping.class)
                        || method.isAnnotationPresent(PutMapping.class)
                        || method.isAnnotationPresent(DeleteMapping.class)
                        || method.isAnnotationPresent(RequestMapping.class);

                if (!isMappingMethod) {
                    continue;
                }

                PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
                assertNotNull(preAuth, String.format("端点方法 %s#%s 必须标注 @PreAuthorize 防纵向越权",
                        clazz.getSimpleName(), method.getName()));

                String authValue = preAuth.value();
                assertTrue(authValue.contains("lunaerp:finance:"),
                        String.format("端点方法 %s#%s 的权限码必须遵循 lunaerp:finance: 前缀规范，当前为: %s",
                                clazz.getSimpleName(), method.getName(), authValue));
            }
        }
    }

    // ==========================================
    // 8. 脱敏注解与套打豁免审查
    // ==========================================
    @Test
    @DisplayName("DoD 审查：金额字段统一标注 @Sensitive，A4 套打 VO 严格豁免脱敏")
    void testSensitiveAndPrintExemption() {
        // 1. 业务响应 VO 金额字段标注 @Sensitive
        Class<?>[] businessRespVOs = new Class<?>[] {
                AccountRespVO.class,
                OtherTxnRespVO.class,
                ExpenseRespVO.class,
                ExpenseItemRespVO.class,
                PayrollRespVO.class,
                PayrollEmployeeRespVO.class,
                PayrollLineItemRespVO.class,
                PayrollPaymentRespVO.class
        };

        for (Class<?> clazz : businessRespVOs) {
            for (Field field : clazz.getDeclaredFields()) {
                if (BigDecimal.class.isAssignableFrom(field.getType())) {
                    String name = field.getName().toLowerCase();
                    if (name.contains("amount") || name.contains("total") || name.contains("balance") || name.contains("price")) {
                        Sensitive sensitive = field.getAnnotation(Sensitive.class);
                        assertNotNull(sensitive, String.format("业务 VO 字段 %s.%s 必须标注 @Sensitive 序列化脱敏",
                                clazz.getSimpleName(), field.getName()));
                        assertEquals(SensitiveType.AMOUNT, sensitive.type());
                    }
                }
            }
        }

        // 2. 套打 VO 必须严格豁免脱敏注解
        Class<?>[] printVOs = new Class<?>[] {
                ExpensePrintRespVO.class,
                ExpensePrintItemRespVO.class,
                PayrollPrintRespVO.class,
                PayrollPrintEmployeeRespVO.class
        };

        for (Class<?> clazz : printVOs) {
            for (Field field : clazz.getDeclaredFields()) {
                assertFalse(field.isAnnotationPresent(Sensitive.class),
                        String.format("套打 VO 字段 %s.%s 必须豁免脱敏，严禁标注 @Sensitive",
                                clazz.getSimpleName(), field.getName()));
            }
        }
    }
}
