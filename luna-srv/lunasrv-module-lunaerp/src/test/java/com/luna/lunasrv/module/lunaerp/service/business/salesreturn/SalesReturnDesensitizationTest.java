package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnPrintRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnRespVO;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 销售退货脱敏、打印 VO 豁免与切面日志契约测试（Issue 07 / W0 Spec）
 *
 * @author luna
 */
@DisplayName("业务管理 - 销售退货脱敏、打印豁免与操作日志契约测试")
public class SalesReturnDesensitizationTest {

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
    @DisplayName("销售退货单 VO：无金额权限脱敏为 ****，有权限输出数值")
    void testSalesReturnRespVO_desensitization() throws Exception {
        SalesReturnRespVO vo = new SalesReturnRespVO();
        vo.setId(10L);
        vo.setReturnNo("RT-20261018-001");
        vo.setTotalAmount(new BigDecimal("1250.00"));

        // 1. 无权限 -> ****
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        String jsonWithoutPerm = objectMapper.writeValueAsString(vo);
        assertTrue(jsonWithoutPerm.contains("\"totalAmount\":\"****\""), "无权限时 totalAmount 必须脱敏为 ****");

        // 2. 有权限 -> 原值输出
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(true);
        String jsonWithPerm = objectMapper.writeValueAsString(vo);
        assertTrue(jsonWithPerm.contains("\"totalAmount\":1250.00"), "有权限时 totalAmount 必须输出明文数值");
    }

    @Test
    @DisplayName("销售退货明细行 VO：无金额权限 unitPrice/amount 脱敏为 ****，有权限输出数值")
    void testSalesReturnItemRespVO_desensitization() throws Exception {
        SalesReturnItemRespVO item = new SalesReturnItemRespVO();
        item.setId(101L);
        item.setUnitPrice(new BigDecimal("25.0000"));
        item.setAmount(new BigDecimal("1250.00"));

        // 1. 无权限 -> ****
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        String jsonWithoutPerm = objectMapper.writeValueAsString(item);
        assertTrue(jsonWithoutPerm.contains("\"unitPrice\":\"****\""), "无权限时 unitPrice 必须脱敏为 ****");
        assertTrue(jsonWithoutPerm.contains("\"amount\":\"****\""), "无权限时 amount 必须脱敏为 ****");

        // 2. 有权限 -> 原值输出
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(true);
        String jsonWithPerm = objectMapper.writeValueAsString(item);
        assertTrue(jsonWithPerm.contains("\"unitPrice\":25.0000"), "有权限时 unitPrice 必须输出明文数值");
        assertTrue(jsonWithPerm.contains("\"amount\":1250.00"), "有权限时 amount 必须输出明文数值");
    }

    @Test
    @DisplayName("打印 VO 豁免脱敏规约：SalesReturnPrintRespVO 及其明细行严禁标注 @Sensitive，无权限仍输出明文")
    void testSalesReturnPrintRespVO_exemption() throws Exception {
        // 1. 反射注解审查：主表与明细行均无 @Sensitive
        for (Field field : SalesReturnPrintRespVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "SalesReturnPrintRespVO." + field.getName() + " 必须豁免脱敏，严禁标注 @Sensitive");
        }
        for (Field field : SalesReturnPrintRespVO.ItemVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "SalesReturnPrintRespVO.ItemVO." + field.getName() + " 必须豁免脱敏，严禁标注 @Sensitive");
        }
        for (Field field : SalesReturnPrintRespVO.RollVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "SalesReturnPrintRespVO.RollVO." + field.getName() + " 必须豁免脱敏，严禁标注 @Sensitive");
        }

        // 2. 序列化行为断言：无权限时依然全量明文输出，绝不出现 ****
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        SalesReturnPrintRespVO printVO = SalesReturnPrintRespVO.builder()
                .id(10L)
                .returnNo("RT-20261018-001")
                .totalAmount(new BigDecimal("1250.00"))
                .items(List.of(SalesReturnPrintRespVO.ItemVO.builder()
                        .id(101L)
                        .unitPrice(new BigDecimal("25.0000"))
                        .amount(new BigDecimal("1250.00"))
                        .build()))
                .build();

        String json = objectMapper.writeValueAsString(printVO);
        assertFalse(json.contains("****"), "打印 VO 序列化绝不应包含脱敏占位符 ****");
        assertTrue(json.contains("\"totalAmount\":1250.00"));
        assertTrue(json.contains("\"unitPrice\":25.0000"));
        assertTrue(json.contains("\"amount\":1250.00"));
    }

    @Test
    @DisplayName("切面日志注解规约：关键动作（submit/withdraw/receive/post/cancel/print）均正确标注 @LogRecord")
    void testLogRecordAnnotations() throws NoSuchMethodException {
        // 1. submit
        Method submitMethod = SalesReturnServiceImpl.class.getMethod("submitSalesReturn", Long.class);
        LogRecord submitLog = submitMethod.getAnnotation(LogRecord.class);
        assertNotNull(submitLog, "submitSalesReturn 必须标注 @LogRecord");
        assertEquals("SALES_RETURN", submitLog.type());
        assertEquals("SUBMIT", submitLog.subType());

        // 2. withdraw
        Method withdrawMethod = SalesReturnServiceImpl.class.getMethod("withdrawSalesReturn", Long.class);
        LogRecord withdrawLog = withdrawMethod.getAnnotation(LogRecord.class);
        assertNotNull(withdrawLog, "withdrawSalesReturn 必须标注 @LogRecord");
        assertEquals("SALES_RETURN", withdrawLog.type());
        assertEquals("WITHDRAW", withdrawLog.subType());

        // 3. receive
        Method receiveMethod = SalesReturnServiceImpl.class.getMethod("receiveSalesReturn",
                com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.SalesReturnReceiveReqVO.class);
        LogRecord receiveLog = receiveMethod.getAnnotation(LogRecord.class);
        assertNotNull(receiveLog, "receiveSalesReturn 必须标注 @LogRecord");
        assertEquals("SALES_RETURN", receiveLog.type());
        assertEquals("RECEIVE", receiveLog.subType());

        // 4. post
        Method postMethod = SalesReturnServiceImpl.class.getMethod("postSalesReturn", Long.class);
        LogRecord postLog = postMethod.getAnnotation(LogRecord.class);
        assertNotNull(postLog, "postSalesReturn 必须标注 @LogRecord");
        assertEquals("SALES_RETURN", postLog.type());
        assertEquals("POST", postLog.subType());

        // 5. cancel
        Method cancelMethod = SalesReturnServiceImpl.class.getMethod("cancelSalesReturn", Long.class, String.class);
        LogRecord cancelLog = cancelMethod.getAnnotation(LogRecord.class);
        assertNotNull(cancelLog, "cancelSalesReturn 必须标注 @LogRecord");
        assertEquals("SALES_RETURN", cancelLog.type());
        assertEquals("CANCEL", cancelLog.subType());

        // 6. print
        Method printMethod = SalesReturnServiceImpl.class.getMethod("getSalesReturnPrintData", Long.class);
        LogRecord printLog = printMethod.getAnnotation(LogRecord.class);
        assertNotNull(printLog, "getSalesReturnPrintData 必须标注 @LogRecord");
        assertEquals("打印", printLog.type());
        assertEquals("销售退货单", printLog.subType());
    }

}
