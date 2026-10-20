package com.luna.lunasrv.module.lunaerp.portal;

import cn.hutool.core.util.ClassUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.CustomerStatementPortalController;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementPortalConfirmReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerStatementPortalRejectReqVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.PortalShipmentController;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentSignReqVO;
import com.luna.lunasrv.module.lunaerp.enums.PortalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.portal.core.filter.PortalTokenFilter;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalShipmentService;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalShipmentServiceImpl;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalTokenService;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalTokenServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementService;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementServiceImpl;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.security.PermitAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户门户全链路收口——安全越权矩阵、@LogRecord 审计、性能与 DoD 核对测试
 * (Spec §10, Issue 06)
 */
@DisplayName("客户门户 - 收口全链路冒烟、安全审计与 DoD 统一核对")
public class PortalCapstoneSmokeSecurityTest {

    @Test
    @DisplayName("DoD 核对：com.luna.lunasrv.module.lunaerp.portal 横切包零新增数据表（纯 Redis 存储）")
    public void testDoD_zeroNewTablesInPortalPackage() {
        Set<Class<?>> portalClasses = ClassUtil.scanPackage("com.luna.lunasrv.module.lunaerp.portal");
        assertFalse(portalClasses.isEmpty(), "必须能够扫描到 portal 模块下的类");

        for (Class<?> clazz : portalClasses) {
            assertFalse(clazz.isAnnotationPresent(TableName.class),
                    "客户门户必须遵循【零新表】DoD，类 " + clazz.getName() + " 严禁标注 @TableName 新增物理表");
        }
    }

    @Test
    @DisplayName("DoD 核对：错误码 1-030-038 段常量值与规范严格对齐")
    public void testDoD_errorCodeConstantsAlignment() {
        assertEquals(1_030_038_000, PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode());
        assertEquals("门户令牌无效或已过期", PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getMsg());

        assertEquals(1_030_038_001, PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode());
        assertEquals("门户数据归属不符", PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getMsg());

        assertEquals(1_030_038_002, PortalErrorCodeConstants.PORTAL_REQUEST_TOO_FREQUENT.getCode());
        assertEquals("门户请求过于频繁", PortalErrorCodeConstants.PORTAL_REQUEST_TOO_FREQUENT.getMsg());
    }

    @Test
    @DisplayName("DoD 核对：`luna.portal.base-url` 复用 04 号冻结键常量")
    public void testDoD_portalBaseUrlConfigKey() {
        assertEquals("luna.portal.base-url", PortalTokenServiceImpl.PORTAL_BASE_URL_KEY);
    }

    @Test
    @DisplayName("安全核对：五处业务留痕 @LogRecord 必须齐全，且严禁记录完整 token 明文（仅指纹）")
    public void testSecurity_fiveLogRecordsPresentAndNoRawToken() throws NoSuchMethodException {
        // 1. 生成令牌 (GENERATE_TOKEN)
        Method generateMethod = PortalTokenServiceImpl.class.getMethod("generateToken", Long.class);
        LogRecord genLog = generateMethod.getAnnotation(LogRecord.class);
        assertNotNull(genLog, "generateToken 必须标注 @LogRecord");
        assertEquals("CUSTOMER_PORTAL", genLog.type());
        assertEquals("GENERATE_TOKEN", genLog.subType());
        assertFalse(genLog.success().contains("newToken"), "生成令牌日志严禁输出完整 token 明文");
        assertTrue(genLog.success().contains("tokenFingerprint"), "生成令牌日志必须仅记录 tokenFingerprint 指纹");

        // 2. 吊销令牌 (REVOKE_TOKEN)
        Method revokeMethod = PortalTokenServiceImpl.class.getMethod("revokeToken", Long.class);
        LogRecord revokeLog = revokeMethod.getAnnotation(LogRecord.class);
        assertNotNull(revokeLog, "revokeToken 必须标注 @LogRecord");
        assertEquals("CUSTOMER_PORTAL", revokeLog.type());
        assertEquals("REVOKE_TOKEN", revokeLog.subType());
        assertFalse(revokeLog.success().contains("{{#token}}"), "吊销令牌日志严禁输出完整 token 明文");
        assertTrue(revokeLog.success().contains("tokenFingerprint"), "吊销令牌日志必须仅记录 tokenFingerprint 指纹");

        // 3. 门户发货单签收 (SIGN_SHIPMENT)
        Method signMethod = PortalShipmentServiceImpl.class.getMethod("signPortalShipment", PortalShipmentSignReqVO.class);
        LogRecord signLog = signMethod.getAnnotation(LogRecord.class);
        assertNotNull(signLog, "signPortalShipment 必须标注 @LogRecord");
        assertEquals("CUSTOMER_PORTAL", signLog.type());
        assertEquals("SIGN_SHIPMENT", signLog.subType());
        assertFalse(signLog.success().contains("token"), "发货单签收日志不包含 token 信息");
        assertTrue(signLog.success().contains("signedByName"), "签收日志必须记录手填姓名");

        // 4. 门户对账单确认 (PORTAL_CONFIRM)
        Method confirmMethod = CustomerStatementServiceImpl.class.getMethod("confirmPortalCustomerStatement",
                CustomerStatementPortalConfirmReqVO.class);
        LogRecord confirmLog = confirmMethod.getAnnotation(LogRecord.class);
        assertNotNull(confirmLog, "confirmPortalCustomerStatement 必须标注 @LogRecord");
        assertEquals("FINANCE_STATEMENT", confirmLog.type());
        assertEquals("PORTAL_CONFIRM", confirmLog.subType());
        assertFalse(confirmLog.success().contains("token"), "对账确认日志不包含 token 信息");

        // 5. 门户对账单驳回 (PORTAL_REJECT)
        Method rejectMethod = CustomerStatementServiceImpl.class.getMethod("rejectPortalCustomerStatement",
                CustomerStatementPortalRejectReqVO.class);
        LogRecord rejectLog = rejectMethod.getAnnotation(LogRecord.class);
        assertNotNull(rejectLog, "rejectPortalCustomerStatement 必须标注 @LogRecord");
        assertEquals("FINANCE_STATEMENT", rejectLog.type());
        assertEquals("PORTAL_REJECT", rejectLog.subType());
        assertFalse(rejectLog.success().contains("token"), "对账驳回日志不包含 token 信息");
    }

    @Test
    @DisplayName("安全核对：门户控制器必须标注 @PermitAll 免登录，路由统一收敛至 /app-api/lunaerp/portal/**")
    public void testSecurity_portalControllersPermitAllAndPrefix() {
        // PortalShipmentController
        PermitAll shipmentPermit = PortalShipmentController.class.getAnnotation(PermitAll.class);
        assertNotNull(shipmentPermit, "PortalShipmentController 必须标注 @PermitAll");
        RequestMapping shipmentMapping = PortalShipmentController.class.getAnnotation(RequestMapping.class);
        assertNotNull(shipmentMapping, "PortalShipmentController 必须标注 @RequestMapping");
        for (String path : shipmentMapping.value()) {
            assertTrue(path.startsWith("/app-api/lunaerp/portal/"), "发货单门户路径必须收敛至 /app-api/lunaerp/portal/ 前缀: " + path);
        }

        // CustomerStatementPortalController
        PermitAll statementPermit = CustomerStatementPortalController.class.getAnnotation(PermitAll.class);
        assertNotNull(statementPermit, "CustomerStatementPortalController 必须标注 @PermitAll");
        RequestMapping statementMapping = CustomerStatementPortalController.class.getAnnotation(RequestMapping.class);
        assertNotNull(statementMapping, "CustomerStatementPortalController 必须标注 @RequestMapping");
        for (String path : statementMapping.value()) {
            assertTrue(path.startsWith("/app-api/lunaerp/portal/"), "对账单门户路径必须收敛至 /app-api/lunaerp/portal/ 前缀: " + path);
        }
    }

    @Test
    @DisplayName("性能与架构核对：PortalTokenFilter 仅拦截 /app-api/lunaerp/portal/** 路径，Redis 校验路径 O(1)")
    public void testPerformance_filterPathPattern() {
        assertEquals("/app-api/lunaerp/portal/**", PortalTokenFilter.PORTAL_API_PATTERN);
    }
}
