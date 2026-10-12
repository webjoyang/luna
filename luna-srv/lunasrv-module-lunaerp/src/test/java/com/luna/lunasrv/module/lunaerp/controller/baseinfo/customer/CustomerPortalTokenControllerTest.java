package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPortalTokenReqVO;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenRespVO;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerPortalTokenController} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class CustomerPortalTokenControllerTest {

    @InjectMocks
    private CustomerPortalTokenController controller;

    @Mock
    private PortalTokenService portalTokenService;

    @Test
    @DisplayName("端点安全与权限注解校验：所有端点均要求 lunaerp:baseinfo:customer:portal-token 权限")
    void testSecurityPermissions() throws Exception {
        Method getMethod = CustomerPortalTokenController.class.getMethod("getPortalToken", Long.class);
        PreAuthorize preAuthGet = getMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthGet);
        assertEquals("@ss.hasPermission('lunaerp:baseinfo:customer:portal-token')", preAuthGet.value());

        Method genMethod = CustomerPortalTokenController.class.getMethod("generatePortalToken", Long.class, CustomerPortalTokenReqVO.class);
        PreAuthorize preAuthGen = genMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthGen);
        assertEquals("@ss.hasPermission('lunaerp:baseinfo:customer:portal-token')", preAuthGen.value());

        Method revokeMethod = CustomerPortalTokenController.class.getMethod("revokePortalToken", Long.class);
        PreAuthorize preAuthRevoke = revokeMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthRevoke);
        assertEquals("@ss.hasPermission('lunaerp:baseinfo:customer:portal-token')", preAuthRevoke.value());
    }

    @Test
    @DisplayName("留痕注解校验：generate 与 revoke 方法具有 @LogRecord 注解，且日志不包含完整 token 明文")
    void testLogRecordAnnotations() throws Exception {
        Method genMethod = CustomerPortalTokenController.class.getMethod("generatePortalToken", Long.class, CustomerPortalTokenReqVO.class);
        LogRecord logGen = genMethod.getAnnotation(LogRecord.class);
        assertNotNull(logGen);
        assertEquals("CUSTOMER_PORTAL", logGen.type());
        assertEquals("GENERATE_TOKEN", logGen.subType());
        // 确保不会直接在 success 表达式中输出完整 raw token
        assertFalse(logGen.success().contains("#_ret.data.token"), "日志严禁直接记录完整 token 明文");
        assertTrue(logGen.success().contains("tokenFingerprint") || logGen.success().contains("fingerprint"));

        Method revokeMethod = CustomerPortalTokenController.class.getMethod("revokePortalToken", Long.class);
        LogRecord logRevoke = revokeMethod.getAnnotation(LogRecord.class);
        assertNotNull(logRevoke);
        assertEquals("CUSTOMER_PORTAL", logRevoke.type());
        assertEquals("REVOKE_TOKEN", logRevoke.subType());
    }

    @Test
    @DisplayName("生成令牌：LogRecord 上下文记录前 6 位指纹，绝不透出 43 位完整明文")
    void testGeneratePortalToken_LogRecordFingerprint() {
        Long customerId = 1001L;
        String rawToken = "abc123XYZ789_abcdefghijklmnopqrstuvwxyz43";
        PortalTokenRespVO mockResp = PortalTokenRespVO.builder()
                .token(rawToken)
                .url("https://portal.luna.com/portal/shipments?t=" + rawToken)
                .expireDays(30)
                .generateTime(LocalDateTime.now())
                .build();
        when(portalTokenService.generateToken(customerId)).thenReturn(mockResp);

        CommonResult<PortalTokenRespVO> result = controller.generatePortalToken(customerId, null);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(mockResp, result.getData());

        // 验证 LogRecordContext 中记录的指纹
        Object recordedFingerprint = LogRecordContext.getVariable("tokenFingerprint");
        assertNotNull(recordedFingerprint);
        String fingerprint = recordedFingerprint.toString();
        assertEquals("abc123...", fingerprint);
        assertEquals(9, fingerprint.length(), "指纹格式应为前6位加上'...'");
        assertFalse(fingerprint.contains(rawToken), "严禁包含完整明文");
    }

    @Test
    @DisplayName("吊销令牌：LogRecord 上下文记录被吊销令牌的指纹")
    void testRevokePortalToken_LogRecordFingerprint() {
        Long customerId = 1001L;
        String revokedToken = "xyz789ABC123_abcdefghijklmnopqrstuvwxyz43";
        when(portalTokenService.revokeToken(customerId)).thenReturn(revokedToken);

        CommonResult<Boolean> result = controller.revokePortalToken(customerId);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());

        Object recordedFingerprint = LogRecordContext.getVariable("tokenFingerprint");
        assertNotNull(recordedFingerprint);
        assertEquals("xyz789...", recordedFingerprint.toString());
    }

    @Test
    @DisplayName("状态查询：委托 PortalTokenService.getTokenStatus 并返回结果")
    void testGetPortalToken() {
        Long customerId = 1001L;
        PortalTokenRespVO mockResp = PortalTokenRespVO.builder()
                .token("tok123")
                .expireDays(20)
                .build();
        when(portalTokenService.getTokenStatus(customerId)).thenReturn(mockResp);

        CommonResult<PortalTokenRespVO> result = controller.getPortalToken(customerId);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(mockResp, result.getData());
        verify(portalTokenService).getTokenStatus(customerId);
    }

}
