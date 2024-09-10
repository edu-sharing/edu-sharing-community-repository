package org.edu_sharing.spring.security.openid.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigObject;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.guest.GuestConfig;
import org.edu_sharing.spring.security.openid.SecurityConfigurationOpenIdConnect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile(SecurityConfigurationOpenIdConnect.PROFILE_ID)
public class OpenIdConfigService {

    public static final String REPOSITORY_OPENID_CONFIG_PATH = "security.sso.openIdConnect";
    public static final String REPOSITORY_CONTEXT_CONFIG_PATH = "repository.context";


    Config rootConfig = LightbendConfigLoader.get();

    public OpenIdConfig getDefaultConfig() {
        Config config = rootConfig.getConfig("security.sso.openIdConnect");
        return ConfigBeanFactory.create(config, OpenIdConfig.class);
    }

    public OpenIdConfig getConfig(String context) {
        Config rootConfig = LightbendConfigLoader.get();
        if (StringUtils.isBlank(context)) {
            return getDefaultConfig();
        }

        String contextConfigPath = getContextConfigPath(context, REPOSITORY_OPENID_CONFIG_PATH);
        if (!rootConfig.hasPath(contextConfigPath)) {
            return getDefaultConfig();
        }

        Config defaultConfig = rootConfig.getConfig(REPOSITORY_OPENID_CONFIG_PATH);
        Config config = rootConfig.getConfig(contextConfigPath).withFallback(defaultConfig);

        return ConfigBeanFactory.create(config, OpenIdConfig.class);
    }

    public List<OpenIdConfig> getAllConfigs(){
        List<OpenIdConfig> configs = new ArrayList<>();
        configs.add(getDefaultConfig());

        Config defaultConfig = rootConfig.getConfig(REPOSITORY_OPENID_CONFIG_PATH);

        if (rootConfig.hasPath(REPOSITORY_CONTEXT_CONFIG_PATH)) {
            ConfigObject contextObject = rootConfig.getObject(REPOSITORY_CONTEXT_CONFIG_PATH);
            Config contextConfig = contextObject.toConfig();
            contextObject.keySet().stream()
                    .map(x->String.join(".", x.contains(".") ? String.format("\"%s\"", x) : x, REPOSITORY_OPENID_CONFIG_PATH))
                    .filter(contextConfig::hasPath)
                    .map(contextConfig::getConfig)
                    .map(x->x.withFallback(defaultConfig))
                    .map(config -> ConfigBeanFactory.create(config, OpenIdConfig.class))
                    .forEach(configs::add);
        }
        return configs;
    }

    private static String getContextConfigPath(String context, String subPath) {
        return String.join(".", REPOSITORY_CONTEXT_CONFIG_PATH, context, subPath);
    }
}
