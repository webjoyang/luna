package com.luna.lunasrv.module.lunaerp.portal.core;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 客户门户限流器
 *
 * <p>基于 Redis 固定窗口（60s）按有效 token 计数，超出 60 次/分钟返回 429。</p>
 *
 * @author luna
 */
@Slf4j
@Component
public class PortalRateLimiter {

    private static final String KEY_PREFIX = "luna_portal:rl:";
    private static final long WINDOW_SECONDS = 60L;
    private static final long MAX_REQUESTS = 60L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public PortalRateLimiter() {
    }

    public PortalRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 对指定令牌执行计数递增并检查是否在限流阈值内
     *
     * @param token 门户令牌
     * @return true 允许访问（<= 60次）；false 超出限流（> 60次）或 token 为空
     */
    public boolean incrementAndCheck(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        String key = KEY_PREFIX + token;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        } else if (count != null && count > 1L) {
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl == -1L) {
                stringRedisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }
        }

        return count != null && count <= MAX_REQUESTS;
    }

}
