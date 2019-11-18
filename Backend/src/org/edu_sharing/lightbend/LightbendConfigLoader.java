package org.edu_sharing.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.connector.ConnectorServiceFactory;

public class LightbendConfigLoader {
    public static String PATH_PREFIX="config/";
    public static String BASE_FILE="edu-sharing.base.conf";
    public static String CUSTOM_FILE="edu-sharing.conf";
    public static Config get(){
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Config base = ConfigFactory.load(loader,PATH_PREFIX+BASE_FILE);
        return ConfigFactory.load(loader,PATH_PREFIX+CUSTOM_FILE).withFallback(base);
        //ConfigBeanFactory.create()
    }

    public static void refresh() {
        ConnectorServiceFactory.invalidate(); // reinit connectors data
        HttpQueryTool.initFinished=false; // reinit proxy settings
    }
}
