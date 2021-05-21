package org.edu_sharing.service.config;

import org.edu_sharing.alfresco.service.config.model.Config;
import org.json.JSONObject;

public interface ConfigService {

	Config getConfig() throws Exception;

    /**
     * Gets the id for the context that matches the domain or null if there is no context for that domain
     * @param domain
     * @return
     * @throws Exception
     */
    String getContextId(String domain) throws Exception;

    Config getConfigByDomain(String domain) throws Exception;


    DynamicConfig setDynamicValue(String key, boolean readPublic, JSONObject object) throws Throwable;

    DynamicConfig getDynamicValue(String key) throws Throwable;
    void refresh();
}
