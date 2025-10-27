package org.edu_sharing.service.lti13.sso.config;

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
public class LTIConfigProvider {
    public static final String SECURITY_SSO_LTI_MAPPING = "security.sso.lti.mapping";

    private final LightbendConfigLoader configLoader;

    @NotNull
    @Cacheable(cacheNames = "ltiProperties", cacheManager = "localCacheManager")
    public LTIProperties getConfig() {
        Config rootConfig = configLoader.getConfig();
        LTIProperties ltiProperties = new LTIProperties();
        if (!rootConfig.hasPath(SECURITY_SSO_LTI_MAPPING)) {
            ltiProperties.setMapping(MappingBeanFactory.getMapping(rootConfig.getConfig(SECURITY_SSO_LTI_MAPPING)));
        }

        return ltiProperties;

    }


    @EventListener(RefreshScopeRefreshedEvent.class)
    @CacheEvict(cacheNames = {"ltiProperties"}, allEntries = true, cacheManager = "localCacheManager")
    public void onConfigurationChanged() {
    }
}
