package org.edu_sharing.spring;

import jakarta.annotation.PostConstruct;
import org.apache.ibatis.annotations.Mapper;
import org.edu_sharing.spring.security.basic.SecurityConfigurationBasic;
import org.edu_sharing.spring.security.context.DelegatingSecurityContextHolderStrategy;
import org.edu_sharing.spring.security.oauth2.SecurityConfigurationOAuth2;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;

@EnableAsync
@EnableMethodSecurity
@EnableScheduling
@Configuration
@MapperScan(value = "org.edu_sharing", annotationClass = Mapper.class)
@Import({SecurityConfigurationSaml.class, SecurityConfigurationOAuth2.class, SecurityConfigurationBasic.class})
//component scan to enable edu-sharing custom condition annotations
@ComponentScan(basePackages = {"org.edu_sharing"})
public class SpringConfigRoot {

    @PostConstruct
    void initSecurityStrategy() {
        SecurityContextHolder.setContextHolderStrategy(new DelegatingSecurityContextHolderStrategy());
    }

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
