package com.luna.lunasrv.module.lunaerp.portal.core;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 客户门户令牌限流器（固定窗口计数）
 *
 * 规则：luna_portal:rl:{token} Redis 固定窗口 60s 计数，>60 返回 429 + 1-030-038-002
 */
@Component
public class PortalRateLimiter {

    public static final String RATE_LIMIT_KEY_PREFIX = "luna_portal:rl:";
    public static final long WINDOW_SECONDS = 60L;
    public static final long MAX_REQUESTS = 60L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 判断指定 token 是否允许通过（固定窗口 60 秒计数，超过 60 次拒绝）
     *
     * @param token 门户有效 token
     * @return true-允许通过；false-超出限流
     */
    public boolean tryAcquire(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String key = formatKey(token);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        } else {
            // 防御性：若 key 缺少过期时间，补设 TTL
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                stringRedisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }
        }
        return count != null && count <= MAX_REQUESTS;
    }

    /**
     * 重置指定 token 的限流计数（用于测试或管理员重置）
     */
    public void reset(String token) {
        if (StringUtils.hasText(token)) {
            stringRedisTemplate.delete(formatKey(token));
        }
    }

    public String formatKey(String token) {
        return RATE_LIMIT_KEY_PREFIX + token;
    }
}
