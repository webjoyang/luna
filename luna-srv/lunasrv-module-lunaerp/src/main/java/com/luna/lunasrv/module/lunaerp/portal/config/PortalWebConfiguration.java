package com.luna.lunasrv.module.lunaerp.portal.config;

import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalRateLimiter;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalTokenFilter;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * 客户门户 Web 配置
 *
 * <p>注册 PortalTokenFilter 到 /app-api/lunaerp/portal/* 路径，设置过滤器执行顺序为 -100。</p>
 *
 * @author luna
 */
@Configuration(proxyBeanMethods = false)
public class PortalWebConfiguration {

    @Bean
    public PortalTokenFilter portalTokenFilter(PortalTokenService portalTokenService,
                                               PortalRateLimiter portalRateLimiter,
                                               CustomerMapper customerMapper) {
        return new PortalTokenFilter(portalTokenService, portalRateLimiter, customerMapper);
    }

    @Bean
    public FilterRegistrationBean<PortalTokenFilter> portalTokenFilterRegistration(PortalTokenFilter filter) {
        FilterRegistrationBean<PortalTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setUrlPatterns(Collections.singletonList("/app-api/lunaerp/portal/*"));
        registration.setOrder(-100);
        return registration;
    }

}
