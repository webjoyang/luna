package com.luna.lunasrv.module.lunaerp.portal.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalRateLimiter 60s 固定窗口限流器测试")
class PortalRateLimiterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PortalRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiter = new PortalRateLimiter(stringRedisTemplate);
    }

    @Test
    @DisplayName("首次请求 (count=1)：设置 60s 过期时间并放行")
    void testFirstRequestSetsExpireAndAllows() {
        String token = "valid-token-123";
        String expectedKey = "luna_portal:rl:" + token;
        when(valueOperations.increment(expectedKey)).thenReturn(1L);

        boolean allowed = rateLimiter.incrementAndCheck(token);

        assertTrue(allowed);
        verify(valueOperations).increment(expectedKey);
        verify(stringRedisTemplate).expire(expectedKey, 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("窗口期内请求 2..60：放行且不重复设置过期时间")
    void testSubsequentRequestsWithinLimit() {
        String token = "valid-token-123";
        String expectedKey = "luna_portal:rl:" + token;
        when(valueOperations.increment(expectedKey)).thenReturn(60L);

        boolean allowed = rateLimiter.incrementAndCheck(token);

        assertTrue(allowed);
        verify(valueOperations).increment(expectedKey);
        verify(stringRedisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("超过 60 次 (count=61)：拒绝访问 (返回 false)")
    void testRequestExceedsLimit() {
        String token = "valid-token-123";
        String expectedKey = "luna_portal:rl:" + token;
        when(valueOperations.increment(expectedKey)).thenReturn(61L);

        boolean allowed = rateLimiter.incrementAndCheck(token);

        assertFalse(allowed);
        verify(valueOperations).increment(expectedKey);
    }

    @Test
    @DisplayName("窗口重置后 (新周期 count=1)：恢复放行并重新设置 60s 过期")
    void testWindowResetAllowsAgain() {
        String token = "valid-token-123";
        String expectedKey = "luna_portal:rl:" + token;
        when(valueOperations.increment(expectedKey)).thenReturn(1L);

        boolean allowed = rateLimiter.incrementAndCheck(token);

        assertTrue(allowed);
        verify(stringRedisTemplate).expire(expectedKey, 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("空 token 或空白 token：直接拒绝")
    void testBlankTokenRejected() {
        assertFalse(rateLimiter.incrementAndCheck(null));
        assertFalse(rateLimiter.incrementAndCheck(""));
        assertFalse(rateLimiter.incrementAndCheck("   "));
        verifyNoInteractions(valueOperations);
    }
}
