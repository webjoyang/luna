package com.luna.lunasrv.module.infra.framework.web.config;

import com.luna.lunasrv.framework.swagger.config.LunasrvSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * infra 模块的 web 组件的 Configuration
 *
 * @author luna
 */
@Configuration(proxyBeanMethods = false)
public class InfraWebConfiguration {

    /**
     * infra 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi infraGroupedOpenApi() {
        return LunasrvSwaggerAutoConfiguration.buildGroupedOpenApi("infra");
    }

}
