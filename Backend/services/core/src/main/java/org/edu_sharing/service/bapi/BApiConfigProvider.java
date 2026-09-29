package org.edu_sharing.service.bapi;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provides the {@link BApiProxyConfig} for the current edu-sharing context.
 * A context may override the {@code repository.bapi} via {@code repository.context.<id>.repository.bapi};
 * api keys and features;
 * all other values (uri, timeout) are always taken from the global config.
 */
@Component
@RequiredArgsConstructor
public class BApiConfigProvider {

    public static final String REPOSITORY_BAPI_CONFIG_PATH = "repository.bapi";
    public static final String REPOSITORY_CONTEXT_CONFIG_PATH = "repository.context";
    private static final String FEATURES = "features";
    private static final String AUTH_USER_API_KEY = "authUserApiKey";
    private static final String GUEST_USER_API_KEY = "guestUserApiKey";

    private final LightbendConfigLoader configLoader;

    @EventListener(RefreshScopeRefreshedEvent.class)
    @CacheEvict(cacheNames = {"bapiDefaultConfig", "bapiConfigs"}, allEntries = true, cacheManager = "localCacheManager")
    public void onConfigurationChanged() {
    }

    public BApiProxyConfig getCurrentConfig() {
        return getConfig(NodeCustomizationPolicies.getEduSharingContext());
    }

    @Cacheable(cacheNames = "bapiDefaultConfig", cacheManager = "localCacheManager")
    public BApiProxyConfig getDefaultConfig() {
        Config rootConfig = configLoader.getConfig();
        if (!rootConfig.hasPath(REPOSITORY_BAPI_CONFIG_PATH)) {
            return new BApiProxyConfig();
        }
        return ConfigBeanFactory.create(rootConfig.getConfig(REPOSITORY_BAPI_CONFIG_PATH), BApiProxyConfig.class);
    }

    @Cacheable(key = "#context", cacheNames = "bapiConfigs", cacheManager = "localCacheManager")
    public BApiProxyConfig getConfig(String context) {
        BApiProxyConfig defaultConfig = getDefaultConfig();
        if (StringUtils.isBlank(context) || context.equals(CCConstants.EDUCONTEXT_DEFAULT)) {
            return defaultConfig;
        }

        Config rootConfig = configLoader.getConfig();
        String contextConfigPath = getContextConfigPath(context);
        Config contextConfig = rootConfig.hasPath(contextConfigPath) ? rootConfig.getConfig(contextConfigPath) : null;

        // the fallback switch is global only
        boolean fallback = defaultConfig.isFallback();

        // uri and timeout are instance wide, only api keys and features can be set per context
        BApiProxyConfig result = new BApiProxyConfig();
        result.setUri(defaultConfig.getUri());
        result.setCallTimeout(defaultConfig.getCallTimeout());
        result.setFallback(fallback);
        result.setFeatures(resolveFeatures(contextConfig, defaultConfig.getFeatures(), fallback));
        result.setAuthUserApiKey(resolveKey(contextConfig, AUTH_USER_API_KEY, defaultConfig.getAuthUserApiKey(), fallback));
        result.setGuestUserApiKey(resolveKey(contextConfig, GUEST_USER_API_KEY, defaultConfig.getGuestUserApiKey(), fallback));
        return result;
    }

    private static List<String> resolveFeatures(Config contextConfig, List<String> defaultValue, boolean fallback) {
        if (contextConfig != null && contextConfig.hasPath(FEATURES)) {
            return contextConfig.getStringList(FEATURES);
        }
        return fallback ? defaultValue : List.of();
    }

    private static String resolveKey(Config contextConfig, String key, String defaultValue, boolean fallback) {
        if (contextConfig != null && contextConfig.hasPath(key)) {
            return contextConfig.getString(key);
        }
        return fallback ? defaultValue : null;
    }

    private static String getContextConfigPath(String context) {
        return String.join(".",
                REPOSITORY_CONTEXT_CONFIG_PATH,
                context.contains(".") ? String.format("\"%s\"", context) : context,
                REPOSITORY_BAPI_CONFIG_PATH);
    }
}
