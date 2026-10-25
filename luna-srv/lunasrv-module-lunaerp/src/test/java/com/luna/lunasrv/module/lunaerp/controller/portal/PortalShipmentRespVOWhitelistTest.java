package com.luna.lunasrv.module.lunaerp.controller.portal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageRespVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户门户发货单 VO 白名单与金额透明度断言测试 (Spec §7 & Issue 03)
 */
public class PortalShipmentRespVOWhitelistTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("规约审查：PortalShipmentPageRespVO 遵循【详情不入列表】原则，严禁包含 items、明细与签收历史")
    public void testPortalShipmentPageRespVO_minimalContract() {
        // 确保列表 VO 不挂载明细列表 items 与签收历史等详情字段
        Field[] fields = PortalShipmentPageRespVO.class.getDeclaredFields();
        for (Field f : fields) {
            assertNotEquals("items", f.getName(), "分页列表 VO 严禁挂载 items 明细行，违反【详情不入列表】原则");
            assertNotEquals("signedByName", f.getName(), "分页列表 VO 严禁包含 signedByName，应归属详情页");
            assertNotEquals("signedAt", f.getName(), "分页列表 VO 严禁包含 signedAt，应归属详情页");
            assertNotEquals("signRemark", f.getName(), "分页列表 VO 严禁包含 signRemark，应归属详情页");
            assertNotEquals("receiveAddress", f.getName(), "分页列表 VO 严禁包含 receiveAddress，应归属详情页");
        }
    }

    @Test
    @DisplayName("规约审查：PortalShipmentDetailRespVO 序列化输出不含客户手机号、内部备注、成本相关与管理端用户字段")
    public void testPortalShipmentDetailRespVO_whitelistFields() throws Exception {
        PortalShipmentDetailRespVO vo = new PortalShipmentDetailRespVO();
        vo.setId(1001L);
        vo.setShipmentNo("SH-20260927-001");
        vo.setCustomerName("常州盛达纺织有限公司");
        vo.setShippingDate(LocalDate.of(2026, 9, 27));
        vo.setReceiveAddress("江苏省常州市高新产业园8号");
        vo.setCarrier("顺丰速运");
        vo.setTrackingNumber("SF123456789");
        vo.setFreightFee(new BigDecimal("150.00"));
        vo.setTotalAmount(new BigDecimal("5000.00"));
        vo.setTotalPieces(10);
        vo.setCurrency("RMB");
        vo.setSignStatus("PENDING");
        vo.setSignedByName("李收货");
        vo.setSignedAt(LocalDateTime.of(2026, 9, 28, 10, 30));
        vo.setSignRemark("货物完好");

        PortalShipmentItemRespVO item = new PortalShipmentItemRespVO();
        item.setId(101L);
        item.setShipmentId(1001L);
        item.setProductId(201L);
        item.setProductCode("P-2026-001");
        item.setProductName("纯棉斜纹布");
        item.setCustomerColorNo("C-88");
        item.setColorName("藏青");
        item.setVatNo("GH-20260901");
        item.setQuantity(new BigDecimal("100.00"));
        item.setPieces(5);
        item.setUnit("M");
        item.setUnitPrice(new BigDecimal("25.50"));
        item.setAmount(new BigDecimal("2550.00"));
        vo.setItems(Collections.singletonList(item));

        String json = objectMapper.writeValueAsString(vo);
        JsonNode root = objectMapper.readTree(json);

        // 1. 白名单断言：绝对不能包含的字段
        String[] forbiddenFields = {
                "phone", "mobile", "contactPhone", "creatorPhone",
                "remark", // 内部备注（注：signRemark 为签收备注，允许透出）
                "cost", "costPrice", "purchaseCost", "costAmount", "grossProfit", "margin",
                "salespersonUserId", "salespersonName", "deptId", "warehouseId",
                "creator", "updater", "tenantId"
        };
        for (String field : forbiddenFields) {
            assertFalse(root.has(field), "PortalShipmentDetailRespVO 严禁暴露字段: " + field);
            assertFalse(root.get("items").get(0).has(field), "PortalShipmentItemRespVO 严禁暴露字段: " + field);
        }

        // 2. 金额透明断言：totalAmount, freightFee, unitPrice, amount 存在且数值完全正确
        assertTrue(root.has("totalAmount"), "必须包含货物金额 totalAmount");
        assertEquals(5000.00, root.get("totalAmount").asDouble(), 0.001);

        assertTrue(root.has("freightFee"), "必须包含运费 freightFee");
        assertEquals(150.00, root.get("freightFee").asDouble(), 0.001);

        JsonNode itemNode = root.get("items").get(0);
        assertTrue(itemNode.has("unitPrice"), "明细必须包含单价 unitPrice");
        assertEquals(25.50, itemNode.get("unitPrice").asDouble(), 0.001);

        assertTrue(itemNode.has("amount"), "明细必须包含金额 amount");
        assertEquals(2550.00, itemNode.get("amount").asDouble(), 0.001);
    }

    @Test
    @DisplayName("规约审查：Portal VO 中 BigDecimal 金额字段严禁标注 @Sensitive 注解（门户金额透明）")
    public void testPortalVO_noSensitiveAnnotation() {
        for (Field field : PortalShipmentPageRespVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "PortalShipmentPageRespVO." + field.getName() + " 不应标注 @Sensitive，门户金额透明");
        }
        for (Field field : PortalShipmentDetailRespVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "PortalShipmentDetailRespVO." + field.getName() + " 不应标注 @Sensitive，门户金额透明");
        }
        for (Field field : PortalShipmentItemRespVO.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(Sensitive.class),
                    "PortalShipmentItemRespVO." + field.getName() + " 不应标注 @Sensitive，门户金额透明");
        }
    }
}
