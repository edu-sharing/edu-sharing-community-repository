package org.edu_sharing.alfresco.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class LightbendConfigLoader {
    public static String PATH_PREFIX = "config/";
    public static String BASE_FILE = "edu-sharing.base.conf";
    public static String DEPLOYMENT_FILE = "edu-sharing.deployment.conf";
    public static String SERVER_FILE = "edu-sharing.server.conf";
    public static String CUSTOM_FILE = "edu-sharing.conf";

    public static Config get() {
        String base = PATH_PREFIX + BASE_FILE;
        String deployment = PATH_PREFIX + DEPLOYMENT_FILE;
        String server = PATH_PREFIX + SERVER_FILE;
        String custom = PATH_PREFIX + CUSTOM_FILE;
        return ConfigFactory.parseResourcesAnySyntax(server)
                .withFallback(ConfigFactory.parseResourcesAnySyntax(deployment))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(custom))
                .withFallback(ConfigFactory.parseResourcesAnySyntax(base))
                .resolve();
    }
}
