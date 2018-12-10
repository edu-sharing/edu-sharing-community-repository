package org.edu_sharing.service.config;

import org.edu_sharing.service.config.model.Config;
import org.json.JSONObject;

public interface ConfigService {

	Config getConfig() throws Exception;

	Config getConfigByDomain(String domain) throws Exception;


    DynamicConfig setDynamicValue(String key, boolean readPublic, JSONObject object) throws Throwable;

    DynamicConfig getDynamicValue(String key) throws Throwable;
}
