package org.edu_sharing.service.authentication.sso.config;

import com.drew.lang.annotations.NotNull;
import com.typesafe.config.Config;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.service.authentication.sso.mapping.MappingBeanFactory;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalConfigProvider {
    public static final String SECURITY_SSO_EXTERNAL_MAPPING = "security.sso.external.mapping";

    private final LightbendConfigLoader configLoader;

    @NotNull
    @Cacheable(cacheNames = "externalProperties", cacheManager = "localCacheManager")
    public ExternalProperties getConfig() {
        Config rootConfig = configLoader.getConfig();
        ExternalProperties externalProperties = new ExternalProperties();
        if(!rootConfig.hasPath(SECURITY_SSO_EXTERNAL_MAPPING)){
            externalProperties.setMapping(MappingBeanFactory.getMapping(rootConfig.getConfig(SECURITY_SSO_EXTERNAL_MAPPING)));
        }

        return externalProperties;

    }


    @EventListener(RefreshScopeRefreshedEvent.class)
    @CacheEvict(cacheNames = {"externalProperties"}, allEntries = true, cacheManager = "localCacheManager")
    public void onConfigurationChanged() {}

}
