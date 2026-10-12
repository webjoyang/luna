package com.luna.lunasrv.module.lunaerp.common.desensitize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecCascadeOptionVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecRespVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 生产主数据与基础信息 RespVO 脱敏断言测试
 */
public class DomainVOSensitiveTest {

    private ObjectMapper objectMapper;
    private SecurityFrameworkService securityFrameworkService;
    private ApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
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
    public void testYarnSpecRespVO_WithoutPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        YarnSpecRespVO vo = new YarnSpecRespVO();
        vo.setId(10L);
        vo.setSpecName("32S 棉");
        vo.setRefPrice(new BigDecimal("25.5000"));

        String json = objectMapper.writeValueAsString(vo);
        assertTrue(json.contains("\"refPrice\":\"****\""));
        assertTrue(json.contains("\"specName\":\"32S 棉\""));
    }

    @Test
    public void testYarnSpecRespVO_WithPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(true);

        YarnSpecRespVO vo = new YarnSpecRespVO();
        vo.setId(10L);
        vo.setRefPrice(new BigDecimal("25.5000"));

        String json = objectMapper.writeValueAsString(vo);
        assertTrue(json.contains("\"refPrice\":25.5000"));
    }

    @Test
    public void testYarnSpecCascadeOptionVO_WithoutPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        YarnSpecCascadeOptionVO.SpecOption option = new YarnSpecCascadeOptionVO.SpecOption();
        option.setId(10L);
        option.setSpecName("32S 棉");
        option.setRefPrice(new BigDecimal("25.5000"));

        YarnSpecCascadeOptionVO vo = new YarnSpecCascadeOptionVO();
        vo.setYarnTypeId(1L);
        vo.setYarnTypeName("棉");
        vo.setSpecs(Collections.singletonList(option));

        String json = objectMapper.writeValueAsString(vo);
        assertTrue(json.contains("\"refPrice\":\"****\""));
    }

    @Test
    public void testProductRespVO_WithoutPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        ProductRespVO vo = new ProductRespVO();
        vo.setCode("P1001");
        vo.setName("莫代尔汗布");
        vo.setKnitProcessingFeePerKg(new BigDecimal("6.50"));
        vo.setWeavingFeePerMeter(new BigDecimal("1.20"));
        vo.setOtherProcessingFeePerMeter(new BigDecimal("0.80"));
        vo.setPackagingFee(new BigDecimal("0.30"));
        vo.setCostPrice(new BigDecimal("35.50"));
        vo.setCostItems("[{\"price\":5.00,\"process\":\"染色\",\"supplierId\":\"s1\"}]");

        String json = objectMapper.writeValueAsString(vo);
        assertTrue(json.contains("\"knitProcessingFeePerKg\":\"****\""));
        assertTrue(json.contains("\"weavingFeePerMeter\":\"****\""));
        assertTrue(json.contains("\"otherProcessingFeePerMeter\":\"****\""));
        assertTrue(json.contains("\"packagingFee\":\"****\""));
        assertTrue(json.contains("\"costPrice\":\"****\""));
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
        String costItemsStr = root.get("costItems").asText();
        assertTrue(costItemsStr.contains("\"price\":\"****\""));
        assertTrue(costItemsStr.contains("\"process\":\"染色\""));
    }

    @Test
    public void testCustomerAndContactRespVO_WithoutPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(false);

        CustomerRespVO customer = new CustomerRespVO();
        customer.setCompanyName("杭州织造有限公司");
        customer.setPhone("0571-12345678");

        String customerJson = objectMapper.writeValueAsString(customer);
        assertTrue(customerJson.contains("\"phone\":\"0571*******78\""));

        CustomerContactRespVO contact = new CustomerContactRespVO();
        contact.setName("张三");
        contact.setPhone("13800000000");
        contact.setEmail("zhangsan@example.com");

        String contactJson = objectMapper.writeValueAsString(contact);
        assertTrue(contactJson.contains("\"name\":\"张*\""));
        assertTrue(contactJson.contains("\"phone\":\"138****0000\""));
        assertTrue(contactJson.contains("\"email\":\"z****@example.com\""));
    }

    @Test
    public void testSupplierAndContactRespVO_WithoutPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(false);

        SupplierRespVO supplier = new SupplierRespVO();
        supplier.setCompanyName("绍兴纱线厂");
        supplier.setPhone("0575-12345678");

        String supplierJson = objectMapper.writeValueAsString(supplier);
        assertTrue(supplierJson.contains("\"phone\":\"0575*******78\""));

        SupplierContactRespVO contact = new SupplierContactRespVO();
        contact.setName("李四");
        contact.setPhone("13900001111");
        contact.setEmail("lisi@example.com");

        String contactJson = objectMapper.writeValueAsString(contact);
        assertTrue(contactJson.contains("\"name\":\"李*\""));
        assertTrue(contactJson.contains("\"phone\":\"139****1111\""));
        assertTrue(contactJson.contains("\"email\":\"l****@example.com\""));
    }

    @Test
    public void testCustomerStatementRespVO_WithoutPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementRespVO vo =
                new com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementRespVO();
        vo.setId(100L);
        vo.setStatementNo("CS20260001");
        vo.setOpeningBalance(new BigDecimal("1000.00"));
        vo.setPeriodDebit(new BigDecimal("5000.00"));
        vo.setPeriodCredit(new BigDecimal("3000.00"));
        vo.setClosingBalance(new BigDecimal("3000.00"));

        String json = objectMapper.writeValueAsString(vo);
        assertTrue(json.contains("\"openingBalance\":\"****\""));
        assertTrue(json.contains("\"periodDebit\":\"****\""));
        assertTrue(json.contains("\"periodCredit\":\"****\""));
        assertTrue(json.contains("\"closingBalance\":\"****\""));
        assertTrue(json.contains("\"statementNo\":\"CS20260001\""));
    }

    @Test
    public void testCustomerStatementRespVO_WithPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(true);

        com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementRespVO vo =
                new com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementRespVO();
        vo.setId(100L);
        vo.setOpeningBalance(new BigDecimal("1000.00"));
        vo.setPeriodDebit(new BigDecimal("5000.00"));
        vo.setPeriodCredit(new BigDecimal("3000.00"));
        vo.setClosingBalance(new BigDecimal("3000.00"));

        String json = objectMapper.writeValueAsString(vo);
        assertTrue(json.contains("\"openingBalance\":1000.00"));
        assertTrue(json.contains("\"periodDebit\":5000.00"));
        assertTrue(json.contains("\"periodCredit\":3000.00"));
        assertTrue(json.contains("\"closingBalance\":3000.00"));
    }

    @Test
    public void testCustomerStatementItemRespVO_WithoutPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementItemRespVO item =
                new com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementItemRespVO();
        item.setId(201L);
        item.setDebitAmount(new BigDecimal("1500.00"));
        item.setCreditAmount(new BigDecimal("0.00"));

        String json = objectMapper.writeValueAsString(item);
        assertTrue(json.contains("\"debitAmount\":\"****\""));
        assertTrue(json.contains("\"creditAmount\":\"****\""));
    }

    @Test
    public void testCustomerRespVO_BankAccountNo_WithoutPermission() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(false);

        CustomerRespVO customer = new CustomerRespVO();
        customer.setCompanyName("广州盛世纺织");
        customer.setBankAccountNo("1202021109900012345");

        String json = objectMapper.writeValueAsString(customer);
        assertTrue(json.contains("\"bankAccountNo\":\"120202***********45\""));
    }

    @Test
    public void testCustomerStatementPrintRespVO_ExemptFromDesensitization() throws Exception {
        // 即使当前用户没有任何敏感权限，打印 VO 也必须原样输出
        when(securityFrameworkService.hasPermission(anyString())).thenReturn(false);

        com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementPrintItemRespVO item =
                com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementPrintItemRespVO.builder()
                        .id(301L)
                        .refNo("SH20260915001")
                        .debitAmount(new BigDecimal("5000.00"))
                        .creditAmount(BigDecimal.ZERO)
                        .build();

        com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementPrintRespVO printVO =
                com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementPrintRespVO.builder()
                        .id(100L)
                        .statementNo("CS20260001")
                        .contactPhone("13800138000")
                        .bankAccountNo("44050143004100001234")
                        .openingBalance(new BigDecimal("1000.00"))
                        .periodDebit(new BigDecimal("5000.00"))
                        .periodCredit(new BigDecimal("3000.00"))
                        .closingBalance(new BigDecimal("3000.00"))
                        .items(Collections.singletonList(item))
                        .build();

        String json = objectMapper.writeValueAsString(printVO);
        // 验证打印 VO 豁免脱敏：金额与联系方式全出原文
        assertTrue(json.contains("\"openingBalance\":1000.00"));
        assertTrue(json.contains("\"periodDebit\":5000.00"));
        assertTrue(json.contains("\"periodCredit\":3000.00"));
        assertTrue(json.contains("\"closingBalance\":3000.00"));
        assertTrue(json.contains("\"contactPhone\":\"13800138000\""));
        assertTrue(json.contains("\"bankAccountNo\":\"44050143004100001234\""));
        assertTrue(json.contains("\"debitAmount\":5000.00"));
        assertFalse(json.contains("****"));
    }
}
