package com.luna.lunasrv.module.lunaerp.portal.core;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenDTO;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalTokenFilter 门户令牌校验过滤器测试")
class PortalTokenFilterTest {

    @Mock
    private PortalTokenService portalTokenService;

    @Mock
    private PortalRateLimiter portalRateLimiter;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private FilterChain filterChain;

    private PortalTokenFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new PortalTokenFilter(portalTokenService, portalRateLimiter, customerMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRequestURI("/app-api/lunaerp/portal/auth/info");
        TenantContextHolder.clear();
        PortalContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        PortalContext.clear();
    }

    @Test
    @DisplayName("缺失 X-Portal-Token 请求头 -> 401 + PORTAL_TOKEN_INVALID (1030038000)")
    void testMissingTokenHeader() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
        assertNotNull(result);
        assertEquals(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode(), result.getCode());
        verifyNoInteractions(filterChain);
        assertNull(TenantContextHolder.getTenantId());
        assertNull(PortalContext.get());
    }

    @Test
    @DisplayName("伪造或已过期/吊销的 Token -> 401 + PORTAL_TOKEN_INVALID (1030038000)")
    void testInvalidOrExpiredToken() throws ServletException, IOException {
        String fakeToken = "invalid-or-expired-token";
        request.addHeader("X-Portal-Token", fakeToken);
        when(portalTokenService.getPortalToken(fakeToken)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
        assertNotNull(result);
        assertEquals(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode(), result.getCode());
        verifyNoInteractions(filterChain);
        assertNull(TenantContextHolder.getTenantId());
        assertNull(PortalContext.get());
    }

    @Test
    @DisplayName("请求频率超限 (>60/min) -> 429 + PORTAL_RATE_LIMIT_EXCEEDED (1030038002)")
    void testRateLimitExceeded() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("X-Portal-Token", token);
        PortalTokenDTO dto = PortalTokenDTO.builder()
                .tenantId(1L)
                .customerId(10L)
                .customerName("测试客户")
                .generateTime(LocalDateTime.now())
                .build();
        when(portalTokenService.getPortalToken(token)).thenReturn(dto);
        when(portalRateLimiter.incrementAndCheck(token)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
        assertNotNull(result);
        assertEquals(PortalErrorCodeConstants.PORTAL_RATE_LIMIT_EXCEEDED.getCode(), result.getCode());
        verifyNoInteractions(customerMapper);
        verifyNoInteractions(filterChain);
        assertNull(TenantContextHolder.getTenantId());
        assertNull(PortalContext.get());
    }

    @Test
    @DisplayName("客户不存在或已软删除 (deleted=true) -> 403 + PORTAL_DATA_ACCESS_DENIED (1030038001)")
    void testDeletedOrNonExistentCustomer() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("X-Portal-Token", token);
        PortalTokenDTO dto = PortalTokenDTO.builder()
                .tenantId(1L)
                .customerId(10L)
                .customerName("测试客户")
                .generateTime(LocalDateTime.now())
                .build();
        when(portalTokenService.getPortalToken(token)).thenReturn(dto);
        when(portalRateLimiter.incrementAndCheck(token)).thenReturn(true);

        // 场景 A: 客户不存在
        when(customerMapper.selectById(10L)).thenReturn(null);
        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
        assertNotNull(result);
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), result.getCode());
        verifyNoInteractions(filterChain);
        assertNull(TenantContextHolder.getTenantId());
        assertNull(PortalContext.get());

        // 场景 B: 客户已软删除 (deleted = true)
        response = new MockHttpServletResponse();
        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(10L);
        deletedCustomer.setDeleted(true);
        when(customerMapper.selectById(10L)).thenReturn(deletedCustomer);

        filter.doFilter(request, response, filterChain);
        assertEquals(403, response.getStatus());
        result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
        assertNotNull(result);
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), result.getCode());
        verifyNoInteractions(filterChain);
        assertNull(TenantContextHolder.getTenantId());
        assertNull(PortalContext.get());
    }

    @Test
    @DisplayName("合法令牌 -> 200，滑动续期成功，TenantContextHolder与PortalContext正确注入，请求结束后清理")
    void testValidTokenSuccess() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("X-Portal-Token", token);
        PortalTokenDTO dto = PortalTokenDTO.builder()
                .tenantId(1L)
                .customerId(10L)
                .customerName("老名称")
                .generateTime(LocalDateTime.now())
                .build();
        when(portalTokenService.getPortalToken(token)).thenReturn(dto);
        when(portalRateLimiter.incrementAndCheck(token)).thenReturn(true);

        CustomerDO customer = new CustomerDO();
        customer.setId(10L);
        customer.setCompanyName("最新客户公司名");
        customer.setDeleted(false);
        customer.setTenantId(1L);
        when(customerMapper.selectById(10L)).thenReturn(customer);

        AtomicReference<Long> capturedTenantId = new AtomicReference<>();
        AtomicReference<Long> capturedCustomerId = new AtomicReference<>();
        AtomicReference<String> capturedCustomerName = new AtomicReference<>();
        AtomicBoolean filterChainExecuted = new AtomicBoolean(false);

        doAnswer(invocation -> {
            filterChainExecuted.set(true);
            capturedTenantId.set(TenantContextHolder.getTenantId());
            capturedCustomerId.set(PortalContext.getCustomerId());
            capturedCustomerName.set(PortalContext.getCustomerName());
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertTrue(filterChainExecuted.get());
        assertEquals(1L, capturedTenantId.get());
        assertEquals(10L, capturedCustomerId.get());
        assertEquals("最新客户公司名", capturedCustomerName.get());

        // 验证滑动续期被调用
        verify(portalTokenService).renewToken(token);

        // 验证请求结束后 ThreadLocal 被彻底清理
        assertNull(TenantContextHolder.getTenantId());
        assertNull(PortalContext.get());
    }

    @Test
    @DisplayName("下游业务抛出异常 -> finally 块中 ThreadLocal 依然彻底清理")
    void testExceptionStillCleansUpThreadLocal() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("X-Portal-Token", token);
        PortalTokenDTO dto = PortalTokenDTO.builder()
                .tenantId(1L)
                .customerId(10L)
                .customerName("测试客户")
                .generateTime(LocalDateTime.now())
                .build();
        when(portalTokenService.getPortalToken(token)).thenReturn(dto);
        when(portalRateLimiter.incrementAndCheck(token)).thenReturn(true);

        CustomerDO customer = new CustomerDO();
        customer.setId(10L);
        customer.setCompanyName("测试客户");
        customer.setDeleted(false);
        when(customerMapper.selectById(10L)).thenReturn(customer);

        doThrow(new RuntimeException("下游业务异常")).when(filterChain).doFilter(any(), any());

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, filterChain));

        assertNull(TenantContextHolder.getTenantId());
        assertNull(PortalContext.get());
    }

    @Test
    @DisplayName("非门户路径 -> shouldNotFilter 返回 true，直接放行不校验 Token")
    void testNonPortalUrlBypassesFilter() throws ServletException, IOException {
        request.setRequestURI("/admin-api/lunaerp/baseinfo/customer/page");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(portalTokenService);
        verifyNoInteractions(portalRateLimiter);
    }
}
