package com.luna.lunasrv.module.lunaerp.portal.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortalRateLimiterTest {

    @InjectMocks
    private PortalRateLimiter portalRateLimiter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String TOKEN = "token_abc_123";
    private static final String KEY = "luna_portal:rl:token_abc_123";

    @Test
    @DisplayName("首次请求计数为 1，应设置 60s 过期时间并返回 true")
    void testTryAcquire_firstRequest() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(KEY)).thenReturn(1L);

        boolean allowed = portalRateLimiter.tryAcquire(TOKEN);

        assertTrue(allowed);
        verify(stringRedisTemplate).expire(KEY, 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("第 60 次请求在限流阈值内，返回 true")
    void testTryAcquire_60thRequest() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(KEY)).thenReturn(60L);
        when(stringRedisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(45L);

        boolean allowed = portalRateLimiter.tryAcquire(TOKEN);

        assertTrue(allowed);
        verify(stringRedisTemplate, never()).expire(KEY, 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("第 61 次请求超过 60 次/分钟限流阈值，返回 false")
    void testTryAcquire_61stRequest_exceedsLimit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(KEY)).thenReturn(61L);
        when(stringRedisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(30L);

        boolean allowed = portalRateLimiter.tryAcquire(TOKEN);

        assertFalse(allowed);
    }

    @Test
    @DisplayName("防御性：计数 > 1 但 key 无 TTL 时补设 60s")
    void testTryAcquire_missingTtlRepair() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(KEY)).thenReturn(5L);
        when(stringRedisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(-1L);

        boolean allowed = portalRateLimiter.tryAcquire(TOKEN);

        assertTrue(allowed);
        verify(stringRedisTemplate).expire(KEY, 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("空 token 直接返回 false")
    void testTryAcquire_emptyToken() {
        assertFalse(portalRateLimiter.tryAcquire(null));
        assertFalse(portalRateLimiter.tryAcquire(""));
        assertFalse(portalRateLimiter.tryAcquire("   "));
    }

    @Test
    @DisplayName("重置限流删除 Redis 键")
    void testReset() {
        portalRateLimiter.reset(TOKEN);
        verify(stringRedisTemplate).delete(KEY);
    }
}
