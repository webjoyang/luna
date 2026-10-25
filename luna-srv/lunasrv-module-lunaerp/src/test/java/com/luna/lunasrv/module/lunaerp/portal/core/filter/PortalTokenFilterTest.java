package com.luna.lunasrv.module.lunaerp.portal.core.filter;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.enums.PortalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalRateLimiter;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalTokenPayload;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortalTokenFilterTest {

    @InjectMocks
    private PortalTokenFilter portalTokenFilter;

    @Mock
    private PortalTokenService portalTokenService;

    @Mock
    private PortalRateLimiter portalRateLimiter;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private FilterChain filterChain;

    private static final String PORTAL_URI = "/app-api/lunaerp/portal/shipment/page";
    private static final String VALID_TOKEN = "valid_token_123456789012345678901234567890";
    private static final Long TENANT_ID = 1L;
    private static final Long CUSTOMER_ID = 1001L;

    @BeforeEach
    void setUp() {
        PortalContext.clear();
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        PortalContext.clear();
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("非门户 URL：直接放行，不校验 token")
    void testFilter_nonPortalUrl_directPass() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/lunaerp/baseinfo/customer");
        MockHttpServletResponse response = new MockHttpServletResponse();

        portalTokenFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(portalTokenService, portalRateLimiter, customerMapper);
    }

    @Test
    @DisplayName("OPTIONS 预检请求：直接放行")
    void testFilter_optionsRequest_directPass() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", PORTAL_URI);
        MockHttpServletResponse response = new MockHttpServletResponse();

        portalTokenFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(portalTokenService, portalRateLimiter, customerMapper);
    }

    @Test
    @DisplayName("缺失 X-Portal-Token 头：返回 401 + PORTAL_TOKEN_INVALID (1-030-038-000)")
    void testFilter_missingTokenHeader_returns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PORTAL_URI);
        MockHttpServletResponse response = new MockHttpServletResponse();

        portalTokenFilter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains(String.valueOf(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode())));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("无效/过期/伪造/吊销 token：返回 401 + PORTAL_TOKEN_INVALID (1-030-038-000)")
    void testFilter_invalidToken_returns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PORTAL_URI);
        request.addHeader(PortalTokenFilter.PORTAL_TOKEN_HEADER, "invalid_token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(portalTokenService.verifyAndRenewToken("invalid_token"))
                .thenThrow(new ServiceException(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID));

        portalTokenFilter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains(String.valueOf(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode())));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("超过 60 次/分钟限流：返回 429 + PORTAL_REQUEST_TOO_FREQUENT (1-030-038-002)")
    void testFilter_rateLimited_returns429() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PORTAL_URI);
        request.addHeader(PortalTokenFilter.PORTAL_TOKEN_HEADER, VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        PortalTokenPayload payload = PortalTokenPayload.builder()
                .tenantId(TENANT_ID)
                .customerId(CUSTOMER_ID)
                .customerName("纺织客户")
                .generateTime(LocalDateTime.now())
                .build();
        when(portalTokenService.verifyAndRenewToken(VALID_TOKEN)).thenReturn(payload);
        when(portalRateLimiter.tryAcquire(VALID_TOKEN)).thenReturn(false);

        portalTokenFilter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains(String.valueOf(PortalErrorCodeConstants.PORTAL_REQUEST_TOO_FREQUENT.getCode())));
        verifyNoInteractions(filterChain);
        verifyNoInteractions(customerMapper);
    }

    @Test
    @DisplayName("失效兜底：客户不存在返回 403 + PORTAL_DATA_ACCESS_DENIED (1-030-038-001)")
    void testFilter_customerNotExists_returns403() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PORTAL_URI);
        request.addHeader(PortalTokenFilter.PORTAL_TOKEN_HEADER, VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        PortalTokenPayload payload = PortalTokenPayload.builder()
                .tenantId(TENANT_ID)
                .customerId(CUSTOMER_ID)
                .customerName("纺织客户")
                .build();
        when(portalTokenService.verifyAndRenewToken(VALID_TOKEN)).thenReturn(payload);
        when(portalRateLimiter.tryAcquire(VALID_TOKEN)).thenReturn(true);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(null);

        portalTokenFilter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains(String.valueOf(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode())));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("失效兜底：客户已删除 (deleted=true) 返回 403 + PORTAL_DATA_ACCESS_DENIED (1-030-038-001)")
    void testFilter_customerDeleted_returns403() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PORTAL_URI);
        request.addHeader(PortalTokenFilter.PORTAL_TOKEN_HEADER, VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        PortalTokenPayload payload = PortalTokenPayload.builder()
                .tenantId(TENANT_ID)
                .customerId(CUSTOMER_ID)
                .customerName("纺织客户")
                .build();
        when(portalTokenService.verifyAndRenewToken(VALID_TOKEN)).thenReturn(payload);
        when(portalRateLimiter.tryAcquire(VALID_TOKEN)).thenReturn(true);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setDeleted(true);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(deletedCustomer);

        portalTokenFilter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains(String.valueOf(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode())));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("有效请求：成功注入 TenantContextHolder 与 PortalContext 并执行后续过滤器，执行后清理上下文")
    void testFilter_validRequest_successAndContextCleaned() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PORTAL_URI);
        request.addHeader(PortalTokenFilter.PORTAL_TOKEN_HEADER, VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        PortalTokenPayload payload = PortalTokenPayload.builder()
                .tenantId(TENANT_ID)
                .customerId(CUSTOMER_ID)
                .customerName("纺织客户")
                .build();
        when(portalTokenService.verifyAndRenewToken(VALID_TOKEN)).thenReturn(payload);
        when(portalRateLimiter.tryAcquire(VALID_TOKEN)).thenReturn(true);

        CustomerDO normalCustomer = new CustomerDO();
        normalCustomer.setId(CUSTOMER_ID);
        normalCustomer.setDeleted(false);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(normalCustomer);

        doAnswer(invocation -> {
            assertEquals(TENANT_ID, TenantContextHolder.getTenantId());
            assertEquals(CUSTOMER_ID, PortalContext.getCustomerId());
            assertEquals("纺织客户", PortalContext.getCustomerName());
            return null;
        }).when(filterChain).doFilter(request, response);

        portalTokenFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // 验证请求结束后上下文已被清理
        assertNull(PortalContext.get());
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    @DisplayName("并发请求：多线程上下文互不干扰且结束后清理干净")
    void testFilter_concurrentRequests_noContextLeak() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean allPassed = new AtomicBoolean(true);

        for (int i = 0; i < threadCount; i++) {
            final long tenantId = i + 1;
            final long customerId = (i + 1) * 1000L;
            final String token = "token_" + i;

            executor.submit(() -> {
                try {
                    MockHttpServletRequest req = new MockHttpServletRequest("GET", PORTAL_URI);
                    req.addHeader(PortalTokenFilter.PORTAL_TOKEN_HEADER, token);
                    MockHttpServletResponse resp = new MockHttpServletResponse();

                    PortalTokenPayload payload = PortalTokenPayload.builder()
                            .tenantId(tenantId)
                            .customerId(customerId)
                            .customerName("客户" + customerId)
                            .build();

                    PortalTokenService localService = mock(PortalTokenService.class);
                    PortalRateLimiter localLimiter = mock(PortalRateLimiter.class);
                    CustomerMapper localMapper = mock(CustomerMapper.class);
                    FilterChain localChain = mock(FilterChain.class);

                    when(localService.verifyAndRenewToken(token)).thenReturn(payload);
                    when(localLimiter.tryAcquire(token)).thenReturn(true);

                    CustomerDO customer = new CustomerDO();
                    customer.setId(customerId);
                    customer.setDeleted(false);
                    when(localMapper.selectById(customerId)).thenReturn(customer);

                    doAnswer(inv -> {
                        if (!java.util.Objects.equals(tenantId, TenantContextHolder.getTenantId())
                                || !java.util.Objects.equals(customerId, PortalContext.getCustomerId())) {
                            allPassed.set(false);
                        }
                        return null;
                    }).when(localChain).doFilter(req, resp);

                    PortalTokenFilter filter = new PortalTokenFilter();
                    org.springframework.test.util.ReflectionTestUtils.setField(filter, "portalTokenService", localService);
                    org.springframework.test.util.ReflectionTestUtils.setField(filter, "portalRateLimiter", localLimiter);
                    org.springframework.test.util.ReflectionTestUtils.setField(filter, "customerMapper", localMapper);

                    filter.doFilter(req, resp, localChain);

                    if (PortalContext.get() != null || TenantContextHolder.getTenantId() != null) {
                        allPassed.set(false);
                    }
                } catch (Exception e) {
                    allPassed.set(false);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        assertTrue(allPassed.get(), "并发请求不应串数据或泄漏上下文");
    }
}
