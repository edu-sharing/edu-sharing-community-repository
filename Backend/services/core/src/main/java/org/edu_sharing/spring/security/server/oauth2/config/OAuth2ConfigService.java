package org.edu_sharing.spring.security.server.oauth2.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ConfigService {
    public static final String CONFIG_PATH = "security.authentication.oauth2";

    Config rootConfig = LightbendConfigLoader.get();

    public OAuth2Config getDefaultConfig() {
        Config config = rootConfig.getConfig(CONFIG_PATH);
        return ConfigBeanFactory.create(config, OAuth2Config.class);
    }
}
