package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementRespVO;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 客户门户对账单 VO 白名单与金额透明度测试 (Spec §7, §8 & Issue 06)
 */
public class CustomerStatementRespVOWhitelistTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private SecurityFrameworkService securityFrameworkService;
    private ApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
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
        PortalContext.clear();
    }

    @Test
    @DisplayName("规约审查：对账单 VO 严禁暴露客户手机号、内部备注、成本相关与员工私密字段")
    public void testCustomerStatementVO_whitelistFields() throws Exception {
        CustomerStatementDetailRespVO vo = new CustomerStatementDetailRespVO();
        vo.setId(1001L);
        vo.setStatementNo("CS20260927001");
        vo.setCustomerId(88L);
        vo.setCustomerName("常州盛达纺织有限公司");
        vo.setCurrency("RMB");
        vo.setPeriodStart(LocalDate.of(2026, 9, 1));
        vo.setPeriodEnd(LocalDate.of(2026, 9, 30));
        vo.setOpeningBalance(new BigDecimal("1000.00"));
        vo.setPeriodDebit(new BigDecimal("5000.00"));
        vo.setPeriodCredit(new BigDecimal("3000.00"));
        vo.setClosingBalance(new BigDecimal("3000.00"));
        vo.setStatus("SENT");
        vo.setConfirmType("ONLINE_PORTAL");
        vo.setConfirmByName("张采购");
        vo.setConfirmTime(LocalDateTime.of(2026, 9, 28, 14, 0));
        vo.setConfirmMethod("门户自主确认");
        vo.setRejectReason(null);
        vo.setReplyRemark(null);
        vo.setRemark("正常对账");
        vo.setCreateTime(LocalDateTime.of(2026, 9, 27, 10, 0));

        CustomerStatementItemRespVO item = new CustomerStatementItemRespVO();
        item.setId(201L);
        item.setStatementId(1001L);
        item.setLineNo(1);
        item.setLedgerEntryId(301L);
        item.setDirection("DEBIT");
        item.setBizDate(LocalDate.of(2026, 9, 15));
        item.setRefType("SHIPMENT_ITEM");
        item.setRefNo("SH20260915001");
        item.setSummary("纯棉斜纹面料 32S");
        item.setDebitAmount(new BigDecimal("5000.00"));
        item.setCreditAmount(BigDecimal.ZERO);
        item.setSignStatus("SIGNED");
        item.setRemark("发货明细");
        item.setHasReversal(false);
        vo.setItems(Collections.singletonList(item));

        // 模拟门户上下文已注入
        PortalContext.set(1L, 88L, "常州盛达纺织有限公司");

        String json = objectMapper.writeValueAsString(vo);
        JsonNode root = objectMapper.readTree(json);

        // 1. 白名单断言：绝对不能包含的敏感字段
        String[] forbiddenFields = {
                "phone", "mobile", "contactPhone", "creatorPhone",
                "cost", "costPrice", "purchaseCost", "costAmount", "grossProfit", "margin",
                "salespersonUserId", "salespersonName", "deptId", "warehouseId",
                "tenantId"
        };
        for (String field : forbiddenFields) {
            assertFalse(root.has(field), "CustomerStatementDetailRespVO 严禁暴露字段: " + field);
            assertFalse(root.get("items").get(0).has(field), "CustomerStatementItemRespVO 严禁暴露字段: " + field);
        }

        // 2. 金额透明断言：门户上下文下，金额字段透明展示（保持数字原值，不脱敏为 ****）
        assertTrue(root.has("openingBalance"), "必须包含期初余额 openingBalance");
        assertEquals(1000.00, root.get("openingBalance").asDouble(), 0.001);

        assertTrue(root.has("periodDebit"), "必须包含本期应收发生额 periodDebit");
        assertEquals(5000.00, root.get("periodDebit").asDouble(), 0.001);

        assertTrue(root.has("periodCredit"), "必须包含本期收款核销额 periodCredit");
        assertEquals(3000.00, root.get("periodCredit").asDouble(), 0.001);

        assertTrue(root.has("closingBalance"), "必须包含期末应收余额 closingBalance");
        assertEquals(3000.00, root.get("closingBalance").asDouble(), 0.001);

        JsonNode itemNode = root.get("items").get(0);
        assertTrue(itemNode.has("debitAmount"), "明细必须包含借方金额 debitAmount");
        assertEquals(5000.00, itemNode.get("debitAmount").asDouble(), 0.001);

        assertTrue(itemNode.has("creditAmount"), "明细必须包含贷方金额 creditAmount");
        assertEquals(0.00, itemNode.get("creditAmount").asDouble(), 0.001);
    }

    @Test
    @DisplayName("规约审查：非门户且无权限上下文下，仍保持管理端安全脱敏为 ****")
    public void testCustomerStatementVO_nonPortalDesensitizedWithoutPermission() throws Exception {
        CustomerStatementRespVO vo = new CustomerStatementRespVO();
        vo.setId(1001L);
        vo.setOpeningBalance(new BigDecimal("1000.00"));
        vo.setClosingBalance(new BigDecimal("3000.00"));

        // 确保无门户上下文
        PortalContext.clear();
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        String json = objectMapper.writeValueAsString(vo);
        assertTrue(json.contains("\"openingBalance\":\"****\""), "非门户且无权限时必须脱敏为 ****");
        assertTrue(json.contains("\"closingBalance\":\"****\""), "非门户且无权限时必须脱敏为 ****");
    }
}
