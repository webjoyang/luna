package com.luna.lunasrv.module.infra.framework.file.config;

import com.luna.lunasrv.module.infra.framework.file.core.client.FileClientFactory;
import com.luna.lunasrv.module.infra.framework.file.core.client.FileClientFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件配置类
 *
 * @author luna
 */
@Configuration(proxyBeanMethods = false)
public class LunasrvFileAutoConfiguration {

    @Bean
    public FileClientFactory fileClientFactory() {
        return new FileClientFactoryImpl();
    }

}
