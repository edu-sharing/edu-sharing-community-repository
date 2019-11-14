package org.edu_sharing.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;

public class LightbendConfigLoader {
    public static Config get(){
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Config base = ConfigFactory.load(loader,"config/edu-sharing.base.conf");
        //ConfigBeanFactory.create()
        return ConfigFactory.load(loader,"config/edu-sharing.conf").withFallback(base);
    }
}
