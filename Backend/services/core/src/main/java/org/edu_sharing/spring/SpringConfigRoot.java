package org.edu_sharing.spring;

import org.apache.ibatis.annotations.Mapper;
import org.edu_sharing.spring.security.basic.SecurityConfigurationBasic;
import org.edu_sharing.spring.security.openid.SecurityConfigurationOpenIdConnect;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@EnableScheduling
@Configuration
@MapperScan(value = "org.edu_sharing", annotationClass = Mapper.class)
@Import({SecurityConfigurationSaml.class, SecurityConfigurationOpenIdConnect.class, SecurityConfigurationBasic.class})
//component scan to enable edu-sharing custom condition annotations
@ComponentScan(basePackages = {"org.edu_sharing"})
public class SpringConfigRoot {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors()));
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("TaskExecutor-");
        executor.initialize();
        return executor;
    }
}
