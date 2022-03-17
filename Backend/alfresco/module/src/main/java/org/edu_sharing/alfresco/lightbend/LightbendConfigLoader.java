package org.edu_sharing.alfresco.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import org.alfresco.repo.cache.SimpleCache;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.tools.PropertiesHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LightbendConfigLoader {
    private static SimpleCache<String, String> configCache = (SimpleCache) AlfAppContextGate.getApplicationContext().getBean("eduSharingLightBendConfigCache");
    private static Logger logger = Logger.getLogger(LightbendConfigLoader.class);
    public static String BASE_FILE = "edu-sharing.reference.conf";
    public static String DEPLOYMENT_FILE = "edu-sharing.deployment.conf";
    public static String OVERRIDE_FILE = "edu-sharing.override.conf";
    public static String CUSTOM_FILE = "edu-sharing.application.conf";

    /**
     * parsing of lightbend config is expensive
     * use @org.edu_sharing.alfresco.lightbend.LightbendConfigCache
     * @return Config
     */
    public static Config get() {
        if(configCache.get("config") != null) {
            return ConfigFactory.parseString(configCache.get("config"), ConfigParseOptions.defaults());
        }
        String base = getConfigFileLocation(BASE_FILE, PropertiesHelper.Config.PathPrefix.DEFAULTS);
        String custom = getConfigFileLocation(CUSTOM_FILE, PropertiesHelper.Config.PathPrefix.DEFAULTS);
        String deploymentCluster = getConfigFileLocation(DEPLOYMENT_FILE, PropertiesHelper.Config.PathPrefix.CLUSTER);
        String overrideCluster = getConfigFileLocation(OVERRIDE_FILE, PropertiesHelper.Config.PathPrefix.CLUSTER);
        String deploymentNode = getConfigFileLocation(DEPLOYMENT_FILE, PropertiesHelper.Config.PathPrefix.NODE);
        String overrideNode = getConfigFileLocation(OVERRIDE_FILE, PropertiesHelper.Config.PathPrefix.NODE);
        Config config = ConfigFactory.parseResourcesAnySyntax(overrideNode)
                .withFallback(ConfigFactory.parseResourcesAnySyntax(deploymentNode))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(overrideCluster))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(deploymentCluster))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(custom))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(base))
                .resolve();
        configCache.put("config", config.root().render(ConfigRenderOptions.concise()));
        return config;
    }

    public static String getConfigFileLocation(String configFileName, PropertiesHelper.Config.PathPrefix pathPrefix) {
        return PropertiesHelper.Config.PATH_CONFIG + pathPrefix + "/" + configFileName;
    }

    public static void refresh(){
        configCache.clear();
    }
}
