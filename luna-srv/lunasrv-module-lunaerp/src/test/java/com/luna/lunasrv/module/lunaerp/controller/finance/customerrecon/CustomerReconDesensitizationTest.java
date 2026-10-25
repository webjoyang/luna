package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementPrintRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceApplicationItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoiceApplicationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.ReceiptRespVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 客户对账、收款单、账龄、发票与联系人脱敏及打印 VO 豁免契约测试
 *
 * @author luna
 */
@DisplayName("财务管理 - 客户对账/收款/账龄/发票脱敏及打印豁免契约测试")
public class CustomerReconDesensitizationTest {

    private ObjectMapper objectMapper;
    private SecurityFrameworkService securityFrameworkService;
    private ApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        securityFrameworkService = mock(SecurityFrameworkService.class);
        applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(SecurityFrameworkService.class)).thenReturn(securityFrameworkService);
        when(applicationContext.getBean("ss")).thenReturn(securityFrameworkService);
        when(applicationContext.getBean("securityFrameworkService")).thenReturn(securityFrameworkService);
        new SpringUtils().setApplicationContext(applicationContext);
    }

    @AfterEach
    public void tearDown() {
        new SpringUtils().setApplicationContext(null);
    }

    @Test
    @DisplayName("对账单 VO：无权限脱敏为 ****，有权限输出数值")
    void testCustomerStatementRespVO_desensitization() throws Exception {
        CustomerStatementRespVO vo = new CustomerStatementRespVO();
        vo.setId(101L);
        vo.setStatementNo("CS20260001");
        vo.setOpeningBalance(new BigDecimal("1000.00"));
        vo.setPeriodDebit(new BigDecimal("5000.00"));
        vo.setPeriodCredit(new BigDecimal("3000.00"));
        vo.setClosingBalance(new BigDecimal("3000.00"));

        // 1. 无权限 -> ****
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        String jsonWithoutPerm = objectMapper.writeValueAsString(vo);
        assertTrue(jsonWithoutPerm.contains("\"openingBalance\":\"****\""));
        assertTrue(jsonWithoutPerm.contains("\"periodDebit\":\"****\""));
        assertTrue(jsonWithoutPerm.contains("\"periodCredit\":\"****\""));
        assertTrue(jsonWithoutPerm.contains("\"closingBalance\":\"****\""));

        // 2. 有权限 -> 原值输出
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(true);
        String jsonWithPerm = objectMapper.writeValueAsString(vo);
        assertTrue(jsonWithPerm.contains("\"openingBalance\":1000.00"));
        assertTrue(jsonWithPerm.contains("\"periodDebit\":5000.00"));
        assertTrue(jsonWithPerm.contains("\"periodCredit\":3000.00"));
        assertTrue(jsonWithPerm.contains("\"closingBalance\":3000.00"));
    }

    @Test
    @DisplayName("对账单明细行 VO：无权限借贷金额脱敏为 ****")
    void testCustomerStatementItemRespVO_desensitization() throws Exception {
        CustomerStatementItemRespVO item = new CustomerStatementItemRespVO();
        item.setId(201L);
        item.setDebitAmount(new BigDecimal("2500.00"));
        item.setCreditAmount(new BigDecimal("0.00"));

        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        String json = objectMapper.writeValueAsString(item);
        assertTrue(json.contains("\"debitAmount\":\"****\""));
        assertTrue(json.contains("\"creditAmount\":\"****\""));
    }

    @Test
    @DisplayName("收款单与明细行 VO：无权限金额脱敏为 ****")
    void testReceiptAndItemRespVO_desensitization() throws Exception {
        ReceiptRespVO receipt = new ReceiptRespVO();
        receipt.setId(301L);
        receipt.setPayAmount(new BigDecimal("8800.00"));
        receipt.setPrepayUsedAmount(new BigDecimal("1200.00"));
        receipt.setAllocatedTotalAmount(new BigDecimal("10000.00"));
        receipt.setUnallocatedAmount(new BigDecimal("0.00"));

        ReceiptItemRespVO item = new ReceiptItemRespVO();
        item.setId(401L);
        item.setAllocateAmount(new BigDecimal("6000.00"));
        item.setBaseAmount(new BigDecimal("6000.00"));

        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        String receiptJson = objectMapper.writeValueAsString(receipt);
        assertTrue(receiptJson.contains("\"payAmount\":\"****\""));
        assertTrue(receiptJson.contains("\"prepayUsedAmount\":\"****\""));
        assertTrue(receiptJson.contains("\"allocatedTotalAmount\":\"****\""));
        assertTrue(receiptJson.contains("\"unallocatedAmount\":\"****\""));

        String itemJson = objectMapper.writeValueAsString(item);
        assertTrue(itemJson.contains("\"allocateAmount\":\"****\""));
        assertTrue(itemJson.contains("\"baseAmount\":\"****\""));
    }

    @Test
    @DisplayName("发票申请与明细行 VO：无权限税额与价税合计金额脱敏为 ****")
    void testInvoiceApplicationAndItemRespVO_desensitization() throws Exception {
        InvoiceApplicationRespVO app = new InvoiceApplicationRespVO();
        app.setId(501L);
        app.setTotalAmountExclTax(new BigDecimal("10000.00"));
        app.setTotalTax(new BigDecimal("1300.00"));
        app.setTotalAmountInclTax(new BigDecimal("11300.00"));

        InvoiceApplicationItemRespVO item = new InvoiceApplicationItemRespVO();
        item.setId(601L);
        item.setShipmentAmount(new BigDecimal("11300.00"));
        item.setAmount(new BigDecimal("5650.00"));

        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        String appJson = objectMapper.writeValueAsString(app);
        assertTrue(appJson.contains("\"totalAmountExclTax\":\"****\""));
        assertTrue(appJson.contains("\"totalTax\":\"****\""));
        assertTrue(appJson.contains("\"totalAmountInclTax\":\"****\""));

        String itemJson = objectMapper.writeValueAsString(item);
        assertTrue(itemJson.contains("\"shipmentAmount\":\"****\""));
        assertTrue(itemJson.contains("\"amount\":\"****\""));
    }

    @Test
    @DisplayName("客户账龄汇总与明细 VO：无权限余额与五桶金额脱敏为 ****")
    void testCustomerAgingSummaryAndDetailRespVO_desensitization() throws Exception {
        CustomerAgingSummaryRespVO summary = CustomerAgingSummaryRespVO.builder()
                .customerId(101L)
                .totalArBalance(new BigDecimal("20000.00"))
                .overdueAmount(new BigDecimal("5000.00"))
                .advanceBalance(new BigDecimal("2000.00"))
                .bucket0To30(new BigDecimal("10000.00"))
                .bucket31To60(new BigDecimal("5000.00"))
                .bucket61To90(BigDecimal.ZERO)
                .bucket91To180(BigDecimal.ZERO)
                .bucket181Plus(BigDecimal.ZERO)
                .build();

        CustomerAgingDetailRespVO detail = CustomerAgingDetailRespVO.builder()
                .ledgerEntryId(1024L)
                .originalAmount(new BigDecimal("10000.00"))
                .writtenOffAmount(new BigDecimal("4000.00"))
                .remainingAmount(new BigDecimal("6000.00"))
                .build();

        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        String summaryJson = objectMapper.writeValueAsString(summary);
        assertTrue(summaryJson.contains("\"totalArBalance\":\"****\""));
        assertTrue(summaryJson.contains("\"overdueAmount\":\"****\""));
        assertTrue(summaryJson.contains("\"advanceBalance\":\"****\""));
        assertTrue(summaryJson.contains("\"bucket0To30\":\"****\""));

        String detailJson = objectMapper.writeValueAsString(detail);
        assertTrue(detailJson.contains("\"originalAmount\":\"****\""));
        assertTrue(detailJson.contains("\"writtenOffAmount\":\"****\""));
        assertTrue(detailJson.contains("\"remainingAmount\":\"****\""));
    }

    @Test
    @DisplayName("客户联系人 VO：无权限联系人姓名、电话、邮箱脱敏，有权限输出原值")
    void testCustomerContactRespVO_desensitization() throws Exception {
        CustomerContactRespVO contact = new CustomerContactRespVO();
        contact.setId(10L);
        contact.setCustomerId(101L);
        contact.setName("李四");
        contact.setPhone("13912345678");
        contact.setEmail("lisi@textile.com");

        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(false);
        String maskedJson = objectMapper.writeValueAsString(contact);
        assertTrue(maskedJson.contains("\"name\":\"李*\""));
        assertTrue(maskedJson.contains("\"phone\":\"139****5678\""));
        assertTrue(maskedJson.contains("\"email\":\"l****@textile.com\""));

        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(true);
        String plainJson = objectMapper.writeValueAsString(contact);
        assertTrue(plainJson.contains("\"name\":\"李四\""));
        assertTrue(plainJson.contains("\"phone\":\"13912345678\""));
        assertTrue(plainJson.contains("\"email\":\"lisi@textile.com\""));
    }

    @Test
    @DisplayName("打印 VO 豁免脱敏规约：CustomerStatementPrintRespVO 及其明细行严禁标注 @Sensitive，无权限仍输出明文")
    void testCustomerStatementPrintRespVO_exemptFromDesensitization() throws Exception {
        // 1. 反射注解审查：主表与明细行均无 @Sensitive
        for (Field field : CustomerStatementPrintRespVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "CustomerStatementPrintRespVO." + field.getName() + " 必须豁免脱敏，严禁标注 @Sensitive");
        }
        for (Field field : CustomerStatementPrintRespVO.ItemVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "CustomerStatementPrintRespVO.ItemVO." + field.getName() + " 必须豁免脱敏，严禁标注 @Sensitive");
        }

        // 2. 运行时无任何权限时，序列化仍输出明文数字与联系电话
        when(securityFrameworkService.hasPermission(anyString())).thenReturn(false);

        CustomerStatementPrintRespVO.ItemVO item = CustomerStatementPrintRespVO.ItemVO.builder()
                .lineNo(1)
                .bizDate(LocalDate.of(2026, 9, 15))
                .refNo("SH2026090001")
                .debitAmount(new BigDecimal("12345.67"))
                .creditAmount(BigDecimal.ZERO)
                .build();

        CustomerStatementPrintRespVO printVO = CustomerStatementPrintRespVO.builder()
                .id(100L)
                .statementNo("CS20260001")
                .contactPhone("0571-88888888")
                .openingBalance(new BigDecimal("1000.00"))
                .periodDebit(new BigDecimal("12345.67"))
                .periodCredit(BigDecimal.ZERO)
                .closingBalance(new BigDecimal("13345.67"))
                .items(List.of(item))
                .build();

        String printJson = objectMapper.writeValueAsString(printVO);
        assertFalse(printJson.contains("****"), "打印数据严禁出现 **** 遮罩");
        assertTrue(printJson.contains("\"contactPhone\":\"0571-88888888\""));
        assertTrue(printJson.contains("\"openingBalance\":1000.00"));
        assertTrue(printJson.contains("\"periodDebit\":12345.67"));
        assertTrue(printJson.contains("\"closingBalance\":13345.67"));
        assertTrue(printJson.contains("\"debitAmount\":12345.67"));
    }
}
