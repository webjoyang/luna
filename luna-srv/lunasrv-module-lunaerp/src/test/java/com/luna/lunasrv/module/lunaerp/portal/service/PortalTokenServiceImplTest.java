package com.luna.lunasrv.module.lunaerp.portal.service;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPortalTokenRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.enums.PortalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalTokenPayload;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortalTokenServiceImplTest {

    @InjectMocks
    private PortalTokenServiceImpl portalTokenService;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ConfigApi configApi;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final Long CUSTOMER_ID = 1001L;
    private static final Long TENANT_ID = 1L;
    private static final String COMPANY_NAME = "杭州某纺织有限公司";
    private static final String REVERSE_KEY = "luna_portal:token_of:1:1001";

    private CustomerDO mockCustomer;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        mockCustomer = new CustomerDO();
        mockCustomer.setId(CUSTOMER_ID);
        mockCustomer.setCompanyName(COMPANY_NAME);
        mockCustomer.setTenantId(TENANT_ID);
        mockCustomer.setDeleted(false);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("首次生成令牌：旧主键不存在，正确生成 43 字符令牌并写入 Redis 双键，拼装完整 URL")
    void testGenerateToken_firstTime() {
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REVERSE_KEY)).thenReturn(null);
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.lunaerp.com/");

        CustomerPortalTokenRespVO respVO = portalTokenService.generateToken(CUSTOMER_ID);

        assertNotNull(respVO);
        assertTrue(respVO.getExists());
        assertNotNull(respVO.getToken());
        assertEquals(43, respVO.getToken().length(), "Base64URL 32 字节无填充应为 43 字符");
        assertEquals(respVO.getToken().substring(0, 6), respVO.getTokenFingerprint());
        assertEquals("https://portal.lunaerp.com/portal/shipments?t=" + respVO.getToken(), respVO.getUrl());
        assertEquals(30L, respVO.getRemainingDays());

        // 验证主键写入
        String mainKey = "luna_portal:token:" + respVO.getToken();
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(mainKey), payloadCaptor.capture(), eq(30L), eq(TimeUnit.DAYS));

        PortalTokenPayload payload = JsonUtils.parseObject(payloadCaptor.getValue(), PortalTokenPayload.class);
        assertNotNull(payload);
        assertEquals(TENANT_ID, payload.getTenantId());
        assertEquals(CUSTOMER_ID, payload.getCustomerId());
        assertEquals(COMPANY_NAME, payload.getCustomerName());

        // 验证反向索引写入
        verify(valueOperations).set(eq(REVERSE_KEY), eq(respVO.getToken()), eq(30L), eq(TimeUnit.DAYS));

        // 首次无旧 token，不触发 DEL
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("重新生成令牌：重发即吊销，先读取反向索引删除旧主键，再写入新双键")
    void testGenerateToken_regenerateRevokesOldToken() {
        String oldToken = "oldToken12345678901234567890123456789012345";
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REVERSE_KEY)).thenReturn(oldToken);
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.lunaerp.com");

        CustomerPortalTokenRespVO respVO = portalTokenService.generateToken(CUSTOMER_ID);

        assertNotNull(respVO);
        assertNotEquals(oldToken, respVO.getToken());

        // 验证旧主键被 DEL
        verify(stringRedisTemplate).delete("luna_portal:token:" + oldToken);

        // 验证新双键写入
        verify(valueOperations).set(eq("luna_portal:token:" + respVO.getToken()), anyString(), eq(30L), eq(TimeUnit.DAYS));
        verify(valueOperations).set(eq(REVERSE_KEY), eq(respVO.getToken()), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("生成令牌：客户档案不存在时抛出 CUSTOMER_NOT_EXISTS (1_030_004_000)")
    void testGenerateToken_customerNotExists() {
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> portalTokenService.generateToken(CUSTOMER_ID));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("生成令牌：基准 URL 未配置时不阻断，url 返回 null")
    void testGenerateToken_baseUrlNotConfigured() {
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REVERSE_KEY)).thenReturn(null);
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn(null);

        CustomerPortalTokenRespVO respVO = portalTokenService.generateToken(CUSTOMER_ID);

        assertNotNull(respVO);
        assertTrue(respVO.getExists());
        assertNotNull(respVO.getToken());
        assertNull(respVO.getUrl(), "基址未配置时 url 应为空");
    }

    @Test
    @DisplayName("吊销令牌：DEL 主键与反向索引双键，保证双删一致性")
    void testRevokeToken_success() {
        String activeToken = "activeToken1234567890123456789012345678901";
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REVERSE_KEY)).thenReturn(activeToken);

        portalTokenService.revokeToken(CUSTOMER_ID);

        // 验证双删
        verify(stringRedisTemplate).delete("luna_portal:token:" + activeToken);
        verify(stringRedisTemplate).delete(REVERSE_KEY);
    }

    @Test
    @DisplayName("吊销令牌：客户无有效令牌时安全幂等，仅删除反向索引键")
    void testRevokeToken_idempotent() {
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REVERSE_KEY)).thenReturn(null);

        portalTokenService.revokeToken(CUSTOMER_ID);

        verify(stringRedisTemplate, never()).delete(startsWith("luna_portal:token:"));
        verify(stringRedisTemplate).delete(REVERSE_KEY);
    }

    @Test
    @DisplayName("查询令牌状态：已开通且有效，返回生成时间、剩余天数与指纹")
    void testGetTokenState_active() {
        String activeToken = "tokenABC1234567890123456789012345678901234";
        LocalDateTime genTime = LocalDateTime.of(2026, 10, 20, 10, 0, 0);
        PortalTokenPayload payload = new PortalTokenPayload(TENANT_ID, CUSTOMER_ID, COMPANY_NAME, genTime);

        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REVERSE_KEY)).thenReturn(activeToken);
        when(valueOperations.get("luna_portal:token:" + activeToken)).thenReturn(JsonUtils.toJsonString(payload));
        when(stringRedisTemplate.getExpire("luna_portal:token:" + activeToken, TimeUnit.SECONDS)).thenReturn(2592000L); // 30天
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.lunaerp.com");

        CustomerPortalTokenRespVO respVO = portalTokenService.getTokenState(CUSTOMER_ID);

        assertNotNull(respVO);
        assertTrue(respVO.getExists());
        assertEquals(activeToken, respVO.getToken());
        assertEquals("tokenA", respVO.getTokenFingerprint());
        assertEquals(genTime, respVO.getGenerateTime());
        assertEquals(30L, respVO.getRemainingDays());
        assertEquals("https://portal.lunaerp.com/portal/shipments?t=" + activeToken, respVO.getUrl());
    }

    @Test
    @DisplayName("查询令牌状态：未开通（反向索引不存在），返回 exists=false")
    void testGetTokenState_notExists() {
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REVERSE_KEY)).thenReturn(null);

        CustomerPortalTokenRespVO respVO = portalTokenService.getTokenState(CUSTOMER_ID);

        assertNotNull(respVO);
        assertFalse(respVO.getExists());
        assertNull(respVO.getToken());
    }

    @Test
    @DisplayName("查询令牌状态：反向索引残留但主键已过期，清理反向索引并返回 exists=false")
    void testGetTokenState_staleReverseKeyCleaned() {
        String staleToken = "staleToken123456789012345678901234567890123";
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REVERSE_KEY)).thenReturn(staleToken);
        when(valueOperations.get("luna_portal:token:" + staleToken)).thenReturn(null);

        CustomerPortalTokenRespVO respVO = portalTokenService.getTokenState(CUSTOMER_ID);

        assertNotNull(respVO);
        assertFalse(respVO.getExists());
        // 自动清理游离反向索引
        verify(stringRedisTemplate).delete(REVERSE_KEY);
    }

    @Test
    @DisplayName("滑动续期：有效请求到来，主键与反向索引双键均续期 30 天")
    void testVerifyAndRenewToken_slidingRenewal() {
        String token = "validToken12345678901234567890123456789012";
        PortalTokenPayload mockPayload = new PortalTokenPayload(TENANT_ID, CUSTOMER_ID, COMPANY_NAME, LocalDateTime.now());

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("luna_portal:token:" + token)).thenReturn(JsonUtils.toJsonString(mockPayload));

        PortalTokenPayload result = portalTokenService.verifyAndRenewToken(token);

        assertNotNull(result);
        assertEquals(CUSTOMER_ID, result.getCustomerId());

        // 验证双键滑动续期
        verify(stringRedisTemplate).expire("luna_portal:token:" + token, 30L, TimeUnit.DAYS);
        verify(stringRedisTemplate).expire("luna_portal:token_of:" + TENANT_ID + ":" + CUSTOMER_ID, 30L, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("滑动续期：无效或已过期 token 抛出 1-030-038-000 PORTAL_TOKEN_INVALID")
    void testVerifyAndRenewToken_invalidToken() {
        String invalidToken = "nonExistentToken";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("luna_portal:token:" + invalidToken)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> portalTokenService.verifyAndRenewToken(invalidToken));
        assertEquals(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("切面日志规约：generateToken 与 revokeToken 必须标注 @LogRecord 且使用指纹")
    void testAspectAnnotations() throws NoSuchMethodException {
        Method generateMethod = PortalTokenServiceImpl.class.getMethod("generateToken", Long.class);
        LogRecord generateLog = generateMethod.getAnnotation(LogRecord.class);
        assertNotNull(generateLog, "generateToken 必须标注 @LogRecord");
        assertEquals("CUSTOMER_PORTAL", generateLog.type());
        assertEquals("GENERATE_TOKEN", generateLog.subType());

        Method revokeMethod = PortalTokenServiceImpl.class.getMethod("revokeToken", Long.class);
        LogRecord revokeLog = revokeMethod.getAnnotation(LogRecord.class);
        assertNotNull(revokeLog, "revokeToken 必须标注 @LogRecord");
        assertEquals("CUSTOMER_PORTAL", revokeLog.type());
        assertEquals("REVOKE_TOKEN", revokeLog.subType());
    }

}
