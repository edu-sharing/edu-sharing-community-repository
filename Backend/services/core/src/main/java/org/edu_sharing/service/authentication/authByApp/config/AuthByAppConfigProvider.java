package org.edu_sharing.service.authentication.authByApp.config;

import com.drew.lang.annotations.NotNull;
import com.typesafe.config.Config;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.service.authentication.sso.config.ExternalProperties;
import org.edu_sharing.service.authentication.sso.mapping.MappingBeanFactory;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthByAppConfigProvider {
    public static final String SECURITY_SSO_AUTH_BY_APP_MAPPING = "security.sso.authByApp.mapping";

    private final LightbendConfigLoader configLoader;

    @NotNull
    @Cacheable(cacheNames = "authByAppProperties", cacheManager = "localCacheManager")
    public AuthByAppProperties getConfig() {
        Config rootConfig = configLoader.getConfig();
        AuthByAppProperties authByAppProperties = new AuthByAppProperties();
        if(rootConfig.hasPath(SECURITY_SSO_AUTH_BY_APP_MAPPING)){
            authByAppProperties.setMapping(MappingBeanFactory.getMapping(rootConfig.getConfig(SECURITY_SSO_AUTH_BY_APP_MAPPING)));
        }

        return authByAppProperties;

    }


    @EventListener(RefreshScopeRefreshedEvent.class)
    @CacheEvict(cacheNames = {"authByAppProperties"}, allEntries = true, cacheManager = "localCacheManager")
    public void onConfigurationChanged() {}

}
