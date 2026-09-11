package com.luna.lunasrv.framework.idempotent.config;

import com.luna.lunasrv.framework.idempotent.core.aop.IdempotentAspect;
import com.luna.lunasrv.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import com.luna.lunasrv.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import com.luna.lunasrv.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import com.luna.lunasrv.framework.idempotent.core.keyresolver.impl.UserIdempotentKeyResolver;
import com.luna.lunasrv.framework.idempotent.core.redis.IdempotentRedisDAO;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import com.luna.lunasrv.framework.redis.config.LunasrvRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@AutoConfiguration(after = LunasrvRedisAutoConfiguration.class)
public class LunasrvIdempotentConfiguration {

    @Bean
    public IdempotentAspect idempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO) {
        return new IdempotentAspect(keyResolvers, idempotentRedisDAO);
    }

    @Bean
    public IdempotentRedisDAO idempotentRedisDAO(StringRedisTemplate stringRedisTemplate) {
        return new IdempotentRedisDAO(stringRedisTemplate);
    }

    // ========== 各种 IdempotentKeyResolver Bean ==========

    @Bean
    public DefaultIdempotentKeyResolver defaultIdempotentKeyResolver() {
        return new DefaultIdempotentKeyResolver();
    }

    @Bean
    public UserIdempotentKeyResolver userIdempotentKeyResolver() {
        return new UserIdempotentKeyResolver();
    }

    @Bean
    public ExpressionIdempotentKeyResolver expressionIdempotentKeyResolver() {
        return new ExpressionIdempotentKeyResolver();
    }

}
