package org.edu_sharing.alfresco.lightbend;

import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

import java.io.Serializable;

public class LightbendConfigCache {

    /**
     * @DEPRECATED
     * use the regular lightbend, it is cached
     */
    public static boolean getBoolean(String config){
        return LightbendConfigLoader.get().getBoolean(config);
    }
}
