package org.edu_sharing.alfresco.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import org.alfresco.repo.cache.SimpleCache;
import org.apache.log4j.lf5.util.StreamUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.edu_sharing.restservices.CollectionDao.logger;

public class LightbendConfigLoader {
    private static SimpleCache<String, String> configCache = (SimpleCache) AlfAppContextGate.getApplicationContext().getBean("eduSharingLightBendConfigCache");

    public static String PATH_PREFIX = "config/";
    public static String BASE_FILE = "edu-sharing.base.conf";
    public static String DEPLOYMENT_FILE = "edu-sharing.deployment.conf";
    public static String SERVER_FILE = "edu-sharing.server-{{hostname}}.conf";
    public static String CUSTOM_FILE = "edu-sharing.conf";

    /**
     * parsing of lightbend config is expensive
     * use @org.edu_sharing.alfresco.lightbend.LightbendConfigCache
     * @return Config
     */
    public static Config get() {
        if(configCache.get("config") != null) {
            return ConfigFactory.parseString(configCache.get("config"), ConfigParseOptions.defaults());
        }
        String base = PATH_PREFIX + BASE_FILE;
        String deployment = PATH_PREFIX + DEPLOYMENT_FILE;
        String server = PATH_PREFIX + getServerConfigName();
        String custom = PATH_PREFIX + CUSTOM_FILE;
        Config config = ConfigFactory.parseResourcesAnySyntax(server)
                .withFallback(ConfigFactory.parseResourcesAnySyntax(deployment))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(custom))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(base))
                .resolve();
        configCache.put("config", config.root().render(ConfigRenderOptions.concise()));
        return config;
    }

    public static String getServerConfigName() {
        String hostname;
        try {
            Process process = Runtime.getRuntime().exec("hostname");
            process.waitFor();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(process.getInputStream(), bos);
            hostname = new String(bos.toByteArray(), StandardCharsets.UTF_8).trim();
        } catch(Throwable t) {
            logger.warn("Could not determine hostname, using \"default\"");
            hostname = "default";
        }
        return SERVER_FILE.replace(
                "{{hostname}}", hostname
        );
    }

    public static void refresh(){
        configCache.clear();
    }
}
