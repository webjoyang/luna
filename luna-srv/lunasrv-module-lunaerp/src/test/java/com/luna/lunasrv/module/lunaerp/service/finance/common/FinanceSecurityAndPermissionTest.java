package com.luna.lunasrv.module.lunaerp.service.finance.common;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.CustomerStatementController;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.ExchangeRateController;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.InvoiceController;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.PaymentController;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo.PortalStatementConfirmReqVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo.PortalStatementRejectReqVO;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging.CustomerAgingCalculator;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalStatementServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 财务模块权限码逐位断言、脱敏权限位、租户隔离与越权防御综合测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FinanceSecurityAndPermissionTest 财务权限码与跨租户/越权防御测试")
public class FinanceSecurityAndPermissionTest {

    @BeforeAll
    static void initMyBatisPlus() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentDO.class);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
        PortalContext.clear();
    }

    @Mock
    private CustomerStatementMapper customerStatementMapper;
    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;
    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerStatementServiceImpl statementService;

    @InjectMocks
    private PortalStatementServiceImpl portalStatementService;

    // ==================== 1. 权限码逐位断言 ====================

    @Test
    @DisplayName("权限码逐位断言：CustomerStatementController 权限码")
    void testCustomerStatementControllerPermissions() throws Exception {
        assertPreAuthorize(CustomerStatementController.class, "getStatementPage", "lunaerp:finance:customer-reconciliation:query");
        assertPreAuthorize(CustomerStatementController.class, "getStatement", "lunaerp:finance:customer-reconciliation:query");
        assertPreAuthorize(CustomerStatementController.class, "createStatement", "lunaerp:finance:customer-reconciliation:create");
        assertPreAuthorize(CustomerStatementController.class, "updateStatement", "lunaerp:finance:customer-reconciliation:update");
        assertPreAuthorize(CustomerStatementController.class, "deleteStatement", "lunaerp:finance:customer-reconciliation:delete");
        assertPreAuthorize(CustomerStatementController.class, "sendStatement", "lunaerp:finance:customer-reconciliation:send");
        assertPreAuthorize(CustomerStatementController.class, "voidStatement", "lunaerp:finance:customer-reconciliation:void");
        assertPreAuthorize(CustomerStatementController.class, "confirmStatementOnBehalf", "lunaerp:finance:customer-reconciliation:confirm-on-behalf");
        assertPreAuthorize(CustomerStatementController.class, "getStatementPrintData", "lunaerp:finance:customer-reconciliation:print");
        assertPreAuthorize(CustomerStatementController.class, "getAgingSummary", "lunaerp:finance:customer-reconciliation:query");
        assertPreAuthorize(CustomerStatementController.class, "getAgingDetail", "lunaerp:finance:customer-reconciliation:query");
    }

    @Test
    @DisplayName("权限码逐位断言：PaymentController 收付款权限码")
    void testPaymentControllerPermissions() throws Exception {
        assertPreAuthorize(PaymentController.class, "getPaymentPage", "lunaerp:finance:payment:query");
        assertPreAuthorize(PaymentController.class, "getPayment", "lunaerp:finance:payment:query");
        assertPreAuthorize(PaymentController.class, "getArBalance", "lunaerp:finance:payment:query");
        assertPreAuthorize(PaymentController.class, "createReceipt", "lunaerp:finance:payment:register");
        assertPreAuthorize(PaymentController.class, "updateReceipt", "lunaerp:finance:payment:register");
        assertPreAuthorize(PaymentController.class, "deleteReceipt", "lunaerp:finance:payment:register");
        assertPreAuthorize(PaymentController.class, "submitReceipt", "lunaerp:finance:payment:register");
        assertPreAuthorize(PaymentController.class, "reverseReceipt", "lunaerp:finance:payment:reverse");
        assertPreAuthorize(PaymentController.class, "advanceWriteoff", "lunaerp:finance:payment:writeoff");
    }

    @Test
    @DisplayName("权限码逐位断言：InvoiceController 发票申请权限码")
    void testInvoiceControllerPermissions() throws Exception {
        assertPreAuthorize(InvoiceController.class, "getInvoicePage", "lunaerp:finance:invoice:query");
        assertPreAuthorize(InvoiceController.class, "getInvoice", "lunaerp:finance:invoice:query");
        assertPreAuthorize(InvoiceController.class, "createInvoice", "lunaerp:finance:invoice:create");
        assertPreAuthorize(InvoiceController.class, "updateInvoice", "lunaerp:finance:invoice:update");
        assertPreAuthorize(InvoiceController.class, "deleteInvoice", "lunaerp:finance:invoice:delete");
        assertPreAuthorize(InvoiceController.class, "issueInvoice", "lunaerp:finance:invoice:issue");
        assertPreAuthorize(InvoiceController.class, "rejectInvoice", "lunaerp:finance:invoice:issue");
    }

    @Test
    @DisplayName("权限码逐位断言：ExchangeRateController 汇率主档权限码")
    void testExchangeRateControllerPermissions() throws Exception {
        assertPreAuthorize(ExchangeRateController.class, "getExchangeRatePage", "lunaerp:finance:exchange-rate:query");
        assertPreAuthorize(ExchangeRateController.class, "getExchangeRate", "lunaerp:finance:exchange-rate:query");
        assertPreAuthorize(ExchangeRateController.class, "createExchangeRate", "lunaerp:finance:exchange-rate:create");
        assertPreAuthorize(ExchangeRateController.class, "updateExchangeRate", "lunaerp:finance:exchange-rate:update");
    }

    @Test
    @DisplayName("脱敏权限常量：金额脱敏与联系人脱敏权限位定义准确")
    void testSensitivePermissionConstants() {
        assertEquals("lunaerp:sensitive:amount", SensitiveConstants.PERMISSION_AMOUNT);
        assertEquals("lunaerp:sensitive:contact", SensitiveConstants.PERMISSION_CONTACT);
    }

    // ==================== 2. 租户隔离与越权防御测试 ====================

    @Test
    @DisplayName("越权防御：获取打印数据时，跨租户或不存在单据抛 STATEMENT_NOT_EXISTS")
    void testPrintData_TenantAndCrossIdIsolation() {
        TenantContextHolder.setTenantId(1L);
        // 单据在 Tenant 2，Tenant 1 查询时返回 null
        when(customerStatementMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> statementService.getStatementPrintData(999L));
        assertEquals(STATEMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("越权防御：客户门户 A 客户令牌访问 B 客户对账单，抛 PORTAL_DATA_ACCESS_DENIED 拦截")
    void testPortalStatement_CrossCustomerAccessDenied() {
        PortalContext.set(1L, 1001L, "客户A"); // 登录客户 A (customerId = 1001)

        CustomerStatementDO statementOfCustomerB = CustomerStatementDO.builder()
                .id(88L)
                .statementNo("CS20260088")
                .customerId(1002L) // 属于客户 B (customerId = 1002)
                .status("SENT")
                .build();
        when(customerStatementMapper.selectById(88L)).thenReturn(statementOfCustomerB);

        // 详情端点横向越权拦截
        ServiceException exDetail = assertThrows(ServiceException.class, () -> portalStatementService.getStatementDetail(88L, null, null));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), exDetail.getCode());

        // 确认端点横向越权拦截
        PortalStatementConfirmReqVO confirmReq = new PortalStatementConfirmReqVO();
        confirmReq.setId(88L);
        confirmReq.setConfirmByName("张三");
        ServiceException exConfirm = assertThrows(ServiceException.class, () -> portalStatementService.confirmStatement(confirmReq));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), exConfirm.getCode());

        // 驳回端点横向越权拦截
        PortalStatementRejectReqVO rejectReq = new PortalStatementRejectReqVO();
        rejectReq.setId(88L);
        rejectReq.setRejectReason("价格有误");
        ServiceException exReject = assertThrows(ServiceException.class, () -> portalStatementService.rejectStatement(rejectReq));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), exReject.getCode());
    }

    @Test
    @DisplayName("越权防御：账龄纯函数计算跨客户严格隔离，不产生跨客户互抵")
    void testAging_CustomerIsolation() {
        LocalDate queryDate = LocalDate.now();

        CustomerDO customerA = new CustomerDO();
        customerA.setId(101L);
        customerA.setCreditDays(30);

        CustomerDO customerB = new CustomerDO();
        customerB.setId(102L);
        customerB.setCreditDays(30);

        // 客户 A: 借方 5000 (发货 10 天前), 贷方 1000
        LedgerEntryDO aDebit = LedgerEntryDO.builder()
                .id(1L).partyType("CUSTOMER").partyId(101L).direction("DEBIT").category("AR_INC")
                .originalAmount(new BigDecimal("5000.00")).currency("CNY").exchangeRate(BigDecimal.ONE)
                .bizDate(queryDate.minusDays(10)).build();
        LedgerEntryDO aCredit = LedgerEntryDO.builder()
                .id(2L).partyType("CUSTOMER").partyId(101L).direction("CREDIT").category("RECEIPT_WRITE_OFF")
                .originalAmount(new BigDecimal("1000.00")).currency("CNY").exchangeRate(BigDecimal.ONE)
                .bizDate(queryDate.minusDays(5)).build();

        // 客户 B: 借方 2000 (发货 40 天前), 贷方 0
        LedgerEntryDO bDebit = LedgerEntryDO.builder()
                .id(3L).partyType("CUSTOMER").partyId(102L).direction("DEBIT").category("AR_INC")
                .originalAmount(new BigDecimal("2000.00")).currency("CNY").exchangeRate(BigDecimal.ONE)
                .bizDate(queryDate.minusDays(40)).build();

        // 客户 A 独立计算：余额 4000，账龄 10 天 -> bucket0To30 (0-30天)
        CustomerAgingCalculator.AgingCalculationResult resultA = CustomerAgingCalculator.calculate(
                customerA, "CNY", queryDate, List.of(aDebit, aCredit), BigDecimal.ZERO, null);
        assertEquals(0, new BigDecimal("4000.00").compareTo(resultA.getSummary().getTotalBalance()));
        assertEquals(0, new BigDecimal("4000.00").compareTo(resultA.getSummary().getBucket0To30()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resultA.getSummary().getBucket31To60()));

        // 客户 B 独立计算：余额 2000，账龄 40 天 -> bucket31To60 (31-60天)
        CustomerAgingCalculator.AgingCalculationResult resultB = CustomerAgingCalculator.calculate(
                customerB, "CNY", queryDate, List.of(bDebit), BigDecimal.ZERO, null);
        assertEquals(0, new BigDecimal("2000.00").compareTo(resultB.getSummary().getTotalBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resultB.getSummary().getBucket0To30()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(resultB.getSummary().getBucket31To60()));
    }

    private void assertPreAuthorize(Class<?> controllerClass, String methodName, String expectedPermission) {
        boolean found = false;
        for (Method m : controllerClass.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                found = true;
                PreAuthorize pre = m.getAnnotation(PreAuthorize.class);
                assertNotNull(pre, "Method " + methodName + " must have @PreAuthorize");
                assertTrue(pre.value().contains(expectedPermission),
                        "Method " + methodName + " @PreAuthorize [" + pre.value() + "] must contain [" + expectedPermission + "]");
                break;
            }
        }
        assertTrue(found, "Method " + methodName + " not found on " + controllerClass.getSimpleName());
    }
}
