package com.luna.lunasrv.module.lunaerp.portal.core.config;

import com.luna.lunasrv.framework.security.config.AuthorizeRequestsCustomizer;
import com.luna.lunasrv.module.lunaerp.portal.core.filter.PortalTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * 客户门户安全与过滤器配置
 */
@Configuration("portalSecurityConfiguration")
public class PortalSecurityConfiguration {

    /**
     * 将 PortalTokenFilter 注册到 Servlet 容器中
     * 顺序 -101：在 TENANT_CONTEXT_FILTER (-104) 之后、Spring Security (-100) 与 TENANT_SECURITY_FILTER (-99) 之前执行
     */
    @Bean
    public FilterRegistrationBean<PortalTokenFilter> portalTokenFilterRegistration(PortalTokenFilter portalTokenFilter) {
        FilterRegistrationBean<PortalTokenFilter> registrationBean = new FilterRegistrationBean<>(portalTokenFilter);
        registrationBean.setOrder(-101);
        return registrationBean;
    }

    /**
     * 配置 Spring Security 对门户 API 放行，由 PortalTokenFilter 统一鉴权
     */
    @Bean("portalAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer portalAuthorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {
            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                registry.requestMatchers(buildAppApi("/lunaerp/portal/**")).permitAll();
            }
        };
    }
}
