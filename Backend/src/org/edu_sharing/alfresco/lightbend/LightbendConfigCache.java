package org.edu_sharing.alfresco.lightbend;

import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

import java.io.Serializable;

public class LightbendConfigCache {

    private static SimpleCache<String, Serializable> configCache = (SimpleCache<String, Serializable>) AlfAppContextGate.getApplicationContext().getBean("eduSharingLightBendConfigCache");

    public static boolean getBoolean(String config){
        Boolean cachedConfig = (Boolean)configCache.get(config);
        if(cachedConfig == null){
            synchronized(configCache) {
                cachedConfig = LightbendConfigLoader.get().getBoolean(config);
                configCache.put(config, cachedConfig);
            }
        }
        return cachedConfig;
    }

    public static void refresh(){
        configCache.clear();
    }
}
