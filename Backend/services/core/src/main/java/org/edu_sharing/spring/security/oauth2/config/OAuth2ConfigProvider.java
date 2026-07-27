package org.edu_sharing.spring.security.oauth2.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigObject;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authentication.sso.mapping.MappingBeanFactory;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2ConfigProvider {

    public static final String REPOSITORY_OAUTH2_CONFIG_PATH = "security.sso.oauth2";
    public static final String REPOSITORY_CONTEXT_CONFIG_PATH = "repository.context";

    private final LightbendConfigLoader configLoader;

    @EventListener(RefreshScopeRefreshedEvent.class)
    @CacheEvict(cacheNames = {"oauth2DefaultConfig", "oauth2Configs", "oauth2AllConfigs"}, allEntries = true , cacheManager = "localCacheManager")
    public void onConfigurationChanged() {}

    @Cacheable(cacheNames = "oauth2DefaultConfig", cacheManager = "localCacheManager")
    public OAuth2ClientProperties getDefaultConfig() {
        Config rootConfig = configLoader.getConfig();
        Config config = rootConfig.getConfig(REPOSITORY_OAUTH2_CONFIG_PATH);
        return createConfig(CCConstants.EDUCONTEXT_DEFAULT, config);
    }

    @Cacheable(key = "#context", cacheNames = "oauth2Configs", cacheManager = "localCacheManager")
    public OAuth2ClientProperties getConfig(String context) {
        Config rootConfig = configLoader.getConfig();
        if (StringUtils.isBlank(context) || context.equals(CCConstants.EDUCONTEXT_DEFAULT)) {
            return getDefaultConfig();
        }

        String contextConfigPath = getContextConfigPath(context);
        if (!rootConfig.hasPath(contextConfigPath)) {
            return getDefaultConfig();
        }

        Config defaultConfig = rootConfig.getConfig(REPOSITORY_OAUTH2_CONFIG_PATH);
        Config config = rootConfig.getConfig(contextConfigPath).withFallback(defaultConfig);

        return createConfig(context, config);
    }

    @Cacheable(cacheNames = "oauth2AllConfigs" , cacheManager = "localCacheManager")
    public List<OAuth2ClientProperties> getAllConfigs(){
        List<OAuth2ClientProperties> configs = new ArrayList<>();
        configs.add(getDefaultConfig());

        Config rootConfig = configLoader.getConfig();
        Config defaultConfig = rootConfig.getConfig(REPOSITORY_OAUTH2_CONFIG_PATH);

        if (rootConfig.hasPath(REPOSITORY_CONTEXT_CONFIG_PATH)) {
            ConfigObject contextObject = rootConfig.getObject(REPOSITORY_CONTEXT_CONFIG_PATH);
            Config contextConfig = contextObject.toConfig();

            for(String contextId: contextObject.keySet()) {
                String configPath = String.join(".", contextId.contains(".") ? String.format("\"%s\"", contextId) : contextId, REPOSITORY_OAUTH2_CONFIG_PATH);
                Config openIdConfig = contextConfig.hasPath(configPath)
                        ? contextConfig.getConfig(configPath).withFallback(defaultConfig)
                        : defaultConfig;
                configs.add(createConfig(contextId, openIdConfig));
            }
        }
        return configs;
    }

    private OAuth2ClientProperties createConfig(String contextId, Config config) {
        OAuth2ClientProperties oAuth2ClientProperties = new OAuth2ClientProperties();
        oAuth2ClientProperties.setContextId(contextId);

        if(config.hasPath("provider")) {
            config.getObject("provider").forEach((key, value) -> {
                OAuth2ClientProperties.Provider provider = ConfigBeanFactory.create(((ConfigObject) value).toConfig(), OAuth2ClientProperties.Provider.class);
                oAuth2ClientProperties.getProvider().put(key, provider);
            });
        }

        if(config.hasPath("registration")) {
            config.getObject("registration").forEach((key, value) -> {
                OAuth2ClientProperties.Registration registration = ConfigBeanFactory.create(((ConfigObject) value).toConfig(), OAuth2ClientProperties.Registration.class);
                oAuth2ClientProperties.getRegistration().put(key, registration);
            });
        }

        if(config.hasPath("mapping")) {
            config.getObject("mapping").forEach((key, value) -> {
                oAuth2ClientProperties.getMapping().put(key, MappingBeanFactory.getMapping(((ConfigObject) value).toConfig()));
            });
        }

        if(config.hasPath("defaultRegistration")){
            String defaultRegistrationId = config.getString("defaultRegistration");
            oAuth2ClientProperties.setDefaultRegistration(defaultRegistrationId);
        }

        return oAuth2ClientProperties;
    }

    private static String getContextConfigPath(String context) {
        return String.join(".", REPOSITORY_CONTEXT_CONFIG_PATH, context, OAuth2ConfigProvider.REPOSITORY_OAUTH2_CONFIG_PATH);
    }
}
