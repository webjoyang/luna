package com.luna.lunasrv.module.lunaerp.service.portal;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
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

import java.util.concurrent.TimeUnit;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link PortalTokenServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class PortalTokenServiceImplTest {

    @InjectMocks
    private PortalTokenServiceImpl portalTokenService;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ConfigApi configApi;

    private static final Long TEST_TENANT_ID = 1L;
    private static final Long TEST_CUSTOMER_ID = 1001L;
    private static final String TEST_CUSTOMER_NAME = "测试纺织客户";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TEST_TENANT_ID);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private CustomerDO createMockCustomer(Long id, Long tenantId, Boolean deleted) {
        CustomerDO customer = new CustomerDO();
        customer.setId(id);
        customer.setTenantId(tenantId);
        customer.setCompanyName(TEST_CUSTOMER_NAME);
        customer.setDeleted(deleted);
        return customer;
    }

    @Test
    @DisplayName("令牌签发：首次生成成功，写入主键与反向索引双键且TTL为30天，返回43位Base64URL令牌")
    void testGenerateToken_FirstTime_Success() {
        // 准备客户
        CustomerDO customer = createMockCustomer(TEST_CUSTOMER_ID, TEST_TENANT_ID, Boolean.FALSE);
        when(customerMapper.selectById(TEST_CUSTOMER_ID)).thenReturn(customer);

        // 模拟反向索引无旧令牌
        String reverseKey = "luna_portal:token_of:" + TEST_TENANT_ID + ":" + TEST_CUSTOMER_ID;
        when(valueOperations.get(reverseKey)).thenReturn(null);

        // 模拟 base-url 配置
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.luna.com");

        // 执行生成
        PortalTokenRespVO resp = portalTokenService.generateToken(TEST_CUSTOMER_ID);

        // 断言响应
        assertNotNull(resp);
        assertNotNull(resp.getToken());
        assertEquals(43, resp.getToken().length(), "SecureRandom 32 字节 Base64URL 无填充长度应为 43 字符");
        assertEquals("https://portal.luna.com/portal/shipments?t=" + resp.getToken(), resp.getUrl());
        assertEquals(30, resp.getExpireDays());
        assertNotNull(resp.getGenerateTime());
        assertNotNull(resp.getExpireTime());

        // 验证主键设置
        String expectedMainKey = "luna_portal:token:" + resp.getToken();
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(expectedMainKey), jsonCaptor.capture(), eq(30L), eq(TimeUnit.DAYS));

        // 校验主键存入的 JSON
        PortalTokenDTO dto = JsonUtils.parseObject(jsonCaptor.getValue(), PortalTokenDTO.class);
        assertNotNull(dto);
        assertEquals(TEST_TENANT_ID, dto.getTenantId());
        assertEquals(TEST_CUSTOMER_ID, dto.getCustomerId());
        assertEquals(TEST_CUSTOMER_NAME, dto.getCustomerName());
        assertNotNull(dto.getGenerateTime());

        // 验证反向索引设置
        verify(valueOperations).set(eq(reverseKey), eq(resp.getToken()), eq(30L), eq(TimeUnit.DAYS));

        // 首次生成不应删除旧主键
        verify(stringRedisTemplate, never()).delete(startsWith("luna_portal:token:"));
    }

    @Test
    @DisplayName("令牌签发：重新生成时立即删除旧令牌（重发即吊销），并更新双键")
    void testGenerateToken_ReGeneration_RevokesOldTokenImmediately() {
        CustomerDO customer = createMockCustomer(TEST_CUSTOMER_ID, TEST_TENANT_ID, Boolean.FALSE);
        when(customerMapper.selectById(TEST_CUSTOMER_ID)).thenReturn(customer);

        String oldToken = "old-token-abcdefghijklmnopqrstuvwxyz1234567";
        String reverseKey = "luna_portal:token_of:" + TEST_TENANT_ID + ":" + TEST_CUSTOMER_ID;
        when(valueOperations.get(reverseKey)).thenReturn(oldToken);
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.luna.com/");

        // 执行重新生成
        PortalTokenRespVO resp = portalTokenService.generateToken(TEST_CUSTOMER_ID);

        // 验证立即删除旧主键
        verify(stringRedisTemplate).delete("luna_portal:token:" + oldToken);

        // 验证写入新主键与更新反向索引
        assertNotEquals(oldToken, resp.getToken());
        verify(valueOperations).set(eq("luna_portal:token:" + resp.getToken()), anyString(), eq(30L), eq(TimeUnit.DAYS));
        verify(valueOperations).set(eq(reverseKey), eq(resp.getToken()), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("基准地址解析：尾部多斜杠、无斜杠与未配置降级")
    void testGenerateToken_BaseUrlResolutions() {
        CustomerDO customer = createMockCustomer(TEST_CUSTOMER_ID, TEST_TENANT_ID, Boolean.FALSE);
        when(customerMapper.selectById(TEST_CUSTOMER_ID)).thenReturn(customer);

        // 情况 1: 尾部多斜杠
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.luna.com///");
        PortalTokenRespVO resp1 = portalTokenService.generateToken(TEST_CUSTOMER_ID);
        assertEquals("https://portal.luna.com/portal/shipments?t=" + resp1.getToken(), resp1.getUrl());

        // 情况 2: 未配置（null）
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn(null);
        PortalTokenRespVO resp2 = portalTokenService.generateToken(TEST_CUSTOMER_ID);
        assertNull(resp2.getUrl(), "基址未配置时 url 应为 null");

        // 情况 3: 配置为空白字符串
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("   ");
        PortalTokenRespVO resp3 = portalTokenService.generateToken(TEST_CUSTOMER_ID);
        assertNull(resp3.getUrl(), "基址配置空白串时 url 应为 null");
    }

    @Test
    @DisplayName("客户校验：客户不存在或已逻辑删除抛 CUSTOMER_NOT_EXISTS")
    void testGenerateToken_CustomerNotExistsOrDeleted() {
        // 客户不存在
        when(customerMapper.selectById(9999L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> portalTokenService.generateToken(9999L));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), ex1.getCode());

        // 客户已删除
        CustomerDO deletedCustomer = createMockCustomer(8888L, TEST_TENANT_ID, Boolean.TRUE);
        when(customerMapper.selectById(8888L)).thenReturn(deletedCustomer);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> portalTokenService.generateToken(8888L));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("令牌吊销：删除主键与反向索引双键，返回被吊销的令牌明文")
    void testRevokeToken_Success() {
        CustomerDO customer = createMockCustomer(TEST_CUSTOMER_ID, TEST_TENANT_ID, Boolean.FALSE);
        when(customerMapper.selectById(TEST_CUSTOMER_ID)).thenReturn(customer);

        String activeToken = "active-token-abcdefghijklmnopqrstuvwxyz123";
        String reverseKey = "luna_portal:token_of:" + TEST_TENANT_ID + ":" + TEST_CUSTOMER_ID;
        when(valueOperations.get(reverseKey)).thenReturn(activeToken);

        String revoked = portalTokenService.revokeToken(TEST_CUSTOMER_ID);

        assertEquals(activeToken, revoked);
        verify(stringRedisTemplate).delete("luna_portal:token:" + activeToken);
        verify(stringRedisTemplate).delete(reverseKey);
    }

    @Test
    @DisplayName("令牌吊销：未开通时吊销不报错，仅清理可能残留的反向索引")
    void testRevokeToken_NoActiveToken() {
        CustomerDO customer = createMockCustomer(TEST_CUSTOMER_ID, TEST_TENANT_ID, Boolean.FALSE);
        when(customerMapper.selectById(TEST_CUSTOMER_ID)).thenReturn(customer);

        String reverseKey = "luna_portal:token_of:" + TEST_TENANT_ID + ":" + TEST_CUSTOMER_ID;
        when(valueOperations.get(reverseKey)).thenReturn(null);

        String revoked = portalTokenService.revokeToken(TEST_CUSTOMER_ID);

        assertNull(revoked);
        verify(stringRedisTemplate).delete(reverseKey);
        verify(stringRedisTemplate, never()).delete(startsWith("luna_portal:token:"));
    }

    @Test
    @DisplayName("状态查询：有效令牌返回剩余天数、过期时间与 URL")
    void testGetTokenStatus_ActiveToken() {
        CustomerDO customer = createMockCustomer(TEST_CUSTOMER_ID, TEST_TENANT_ID, Boolean.FALSE);
        when(customerMapper.selectById(TEST_CUSTOMER_ID)).thenReturn(customer);

        String token = "active-token-abcdefghijklmnopqrstuvwxyz123";
        String reverseKey = "luna_portal:token_of:" + TEST_TENANT_ID + ":" + TEST_CUSTOMER_ID;
        when(valueOperations.get(reverseKey)).thenReturn(token);

        String mainKey = "luna_portal:token:" + token;
        PortalTokenDTO dto = PortalTokenDTO.builder()
                .tenantId(TEST_TENANT_ID)
                .customerId(TEST_CUSTOMER_ID)
                .customerName(TEST_CUSTOMER_NAME)
                .build();
        when(valueOperations.get(mainKey)).thenReturn(JsonUtils.toJsonString(dto));
        // 剩余 25 天（25 * 86400 = 2160000 秒）
        when(stringRedisTemplate.getExpire(mainKey, TimeUnit.SECONDS)).thenReturn(2160000L);
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.luna.com");

        PortalTokenRespVO status = portalTokenService.getTokenStatus(TEST_CUSTOMER_ID);

        assertNotNull(status);
        assertEquals(token, status.getToken());
        assertEquals("https://portal.luna.com/portal/shipments?t=" + token, status.getUrl());
        assertEquals(25, status.getExpireDays());
        assertNotNull(status.getExpireTime());
    }

    @Test
    @DisplayName("状态查询：未开通或已过期时返回 null 并清理反向索引")
    void testGetTokenStatus_NotFoundOrExpired() {
        CustomerDO customer = createMockCustomer(TEST_CUSTOMER_ID, TEST_TENANT_ID, Boolean.FALSE);
        when(customerMapper.selectById(TEST_CUSTOMER_ID)).thenReturn(customer);

        String reverseKey = "luna_portal:token_of:" + TEST_TENANT_ID + ":" + TEST_CUSTOMER_ID;

        // 情况 1: 反向索引不存在
        when(valueOperations.get(reverseKey)).thenReturn(null);
        assertNull(portalTokenService.getTokenStatus(TEST_CUSTOMER_ID));

        // 情况 2: 反向索引存在但主键已过期（null）
        String token = "expired-token-xyz";
        when(valueOperations.get(reverseKey)).thenReturn(token);
        String mainKey = "luna_portal:token:" + token;
        when(valueOperations.get(mainKey)).thenReturn(null);

        assertNull(portalTokenService.getTokenStatus(TEST_CUSTOMER_ID));
        verify(stringRedisTemplate).delete(reverseKey);
    }

    @Test
    @DisplayName("滑动续期：有效令牌刷新主键与反向索引为 30 天 TTL")
    void testRenewToken_Success() {
        String token = "valid-token-123456";
        String mainKey = "luna_portal:token:" + token;
        PortalTokenDTO dto = PortalTokenDTO.builder()
                .tenantId(TEST_TENANT_ID)
                .customerId(TEST_CUSTOMER_ID)
                .customerName(TEST_CUSTOMER_NAME)
                .build();
        when(valueOperations.get(mainKey)).thenReturn(JsonUtils.toJsonString(dto));

        boolean renewed = portalTokenService.renewToken(token);

        assertTrue(renewed);
        verify(stringRedisTemplate).expire(mainKey, 30L, TimeUnit.DAYS);
        verify(stringRedisTemplate).expire("luna_portal:token_of:" + TEST_TENANT_ID + ":" + TEST_CUSTOMER_ID, 30L, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("滑动续期：令牌不存在或为空返回 false")
    void testRenewToken_NotFound() {
        assertFalse(portalTokenService.renewToken(null));
        assertFalse(portalTokenService.renewToken("   "));

        when(valueOperations.get("luna_portal:token:not-exists")).thenReturn(null);
        assertFalse(portalTokenService.renewToken("not-exists"));
    }

}
