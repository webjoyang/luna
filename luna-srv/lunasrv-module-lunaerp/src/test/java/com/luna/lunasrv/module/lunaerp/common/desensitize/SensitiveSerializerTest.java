package com.luna.lunasrv.module.lunaerp.common.desensitize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 敏感字段脱敏序列化器单元测试
 */
public class SensitiveSerializerTest {

    private ObjectMapper objectMapper;
    private SecurityFrameworkService securityFrameworkService;
    private ApplicationContext applicationContext;

    @Data
    public static class TestUserVO {
        @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.CHINESE_NAME)
        private String chineseName;

        @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.MOBILE)
        private String mobile;

        @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.BANK_CARD)
        private String bankCard;

        @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.FIXED_PHONE)
        private String fixedPhone;

        @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.EMAIL)
        private String email;

        @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
        private BigDecimal amount;

        @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
        private String costItems;
    }

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
    public void testSerialize_WithAllPermissions() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(true);
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(true);

        TestUserVO vo = new TestUserVO();
        vo.setChineseName("张三");
        vo.setMobile("13812345678");
        vo.setBankCard("6222021234567890");
        vo.setFixedPhone("0571-88889999");
        vo.setEmail("test@example.com");
        vo.setAmount(new BigDecimal("99.99"));
        vo.setCostItems("[{\"price\":12.5,\"supplier\":\"棉纺厂\"}]");

        String json = objectMapper.writeValueAsString(vo);

        assertTrue(json.contains("\"chineseName\":\"张三\""));
        assertTrue(json.contains("\"mobile\":\"13812345678\""));
        assertTrue(json.contains("\"bankCard\":\"6222021234567890\""));
        assertTrue(json.contains("\"fixedPhone\":\"0571-88889999\""));
        assertTrue(json.contains("\"email\":\"test@example.com\""));
        assertTrue(json.contains("\"amount\":99.99"));
        com.fasterxml.jackson.databind.JsonNode rootAll = objectMapper.readTree(json);
        String costItemsStrAll = rootAll.get("costItems").asText();
        assertTrue(costItemsStrAll.contains("\"price\":12.5"));
        assertTrue(costItemsStrAll.contains("\"supplier\":\"棉纺厂\""));
    }

    @Test
    public void testSerialize_WithoutPermissions() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(false);
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        TestUserVO vo = new TestUserVO();
        vo.setChineseName("张三");
        vo.setMobile("13812345678");
        vo.setBankCard("6222021234567890");
        vo.setFixedPhone("0571-88889999");
        vo.setEmail("test@example.com");
        vo.setAmount(new BigDecimal("99.99"));
        vo.setCostItems("[{\"price\":12.5,\"supplier\":\"棉纺厂\"}]");

        String json = objectMapper.writeValueAsString(vo);

        // 验证脱敏
        assertTrue(json.contains("\"chineseName\":\"张*\""));
        assertTrue(json.contains("\"mobile\":\"138****5678\""));
        assertTrue(json.contains("\"bankCard\":\"622202********90\""));
        assertTrue(json.contains("\"fixedPhone\":\"0571*******99\""));
        assertTrue(json.contains("\"email\":\"t****@example.com\""));
        assertTrue(json.contains("\"amount\":\"****\""));
        com.fasterxml.jackson.databind.JsonNode rootNone = objectMapper.readTree(json);
        String costItemsStrNone = rootNone.get("costItems").asText();
        assertTrue(costItemsStrNone.contains("\"price\":\"****\""));
        assertTrue(costItemsStrNone.contains("\"supplier\":\"棉纺厂\""));
    }

    @Test
    public void testSerialize_WithoutLoginContext() throws Exception {
        // 模拟无登录上下文或无 Spring Bean
        new SpringUtils().setApplicationContext(null);

        TestUserVO vo = new TestUserVO();
        vo.setChineseName("诸葛孔明");
        vo.setMobile("13987654321");
        vo.setAmount(new BigDecimal("500.00"));

        String json = objectMapper.writeValueAsString(vo);

        assertTrue(json.contains("\"chineseName\":\"诸***\""));
        assertTrue(json.contains("\"mobile\":\"139****4321\""));
        assertTrue(json.contains("\"amount\":\"****\""));
    }

    @Test
    public void testSerialize_NullValues() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(false);
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        TestUserVO vo = new TestUserVO();

        String json = objectMapper.writeValueAsString(vo);

        assertTrue(json.contains("\"chineseName\":null"));
        assertTrue(json.contains("\"mobile\":null"));
        assertTrue(json.contains("\"amount\":null"));
        assertTrue(json.contains("\"costItems\":null"));
    }

    @Test
    public void testAmountDesensitizationHandler_PlainString() {
        assertEquals("****", AmountDesensitizationHandler.desensitize("123.45"));
        assertEquals("****", AmountDesensitizationHandler.desensitize(new BigDecimal("88.88")));
        assertNull(AmountDesensitizationHandler.desensitize(null));
        assertEquals("", AmountDesensitizationHandler.desensitize(""));
    }

    @Test
    public void testAmountDesensitizationHandler_JsonString() {
        String json = "[{\"price\":10.5,\"process\":\"纺纱\"},{\"unitPrice\":20,\"process\":\"染色\"}]";
        String masked = AmountDesensitizationHandler.desensitize(json);
        assertTrue(masked.contains("\"price\":\"****\""));
        assertTrue(masked.contains("\"unitPrice\":\"****\""));
        assertTrue(masked.contains("\"process\":\"纺纱\""));
        assertTrue(masked.contains("\"process\":\"染色\""));
    }
}
