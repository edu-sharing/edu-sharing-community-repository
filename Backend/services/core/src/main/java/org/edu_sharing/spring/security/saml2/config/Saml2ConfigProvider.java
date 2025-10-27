package org.edu_sharing.spring.security.saml2.config;

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
public class Saml2ConfigProvider {

    public static final String SECURITY_SSO_SAML_MAPPING = "security.sso.saml.mapping";

    private final LightbendConfigLoader configLoader;

    @NotNull
    @Cacheable(cacheNames = "saml2Properties", cacheManager = "localCacheManager")
    public Saml2Properties getConfig() {
        Config rootConfig = configLoader.getConfig();
        Saml2Properties saml2Properties = new Saml2Properties();
        if(!rootConfig.hasPath(SECURITY_SSO_SAML_MAPPING)){
            saml2Properties.setMapping(MappingBeanFactory.getMapping(rootConfig.getConfig(SECURITY_SSO_SAML_MAPPING)));
        }

        return saml2Properties;

    }


    @EventListener(RefreshScopeRefreshedEvent.class)
    @CacheEvict(cacheNames = {"saml2Properties"}, allEntries = true, cacheManager = "localCacheManager")
    public void onConfigurationChanged() {}
}
