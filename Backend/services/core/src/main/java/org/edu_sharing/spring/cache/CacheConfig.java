package org.edu_sharing.spring.cache;

import org.edu_sharing.spring.conditions.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    // @ConditionalOnMissingBean actually doesn't work. The beanFactory doesn't know the beans defined by CacheConfig
    // or other Configuration Beans until they are registered. You can overwrite the actual bean
    // by defining another bean with the same name and type. But the Order of creation matters!
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
