package org.edu_sharing.service.nodeservice;

import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.Map;

public class PropertiesInterceptorImpl implements PropertiesInterceptor {

    @Override
    public Map<String, Object> beforeCacheProperties(PropertiesContext context) {
        return context.getProperties();
    }

    @Override
    public Map<String, Object> beforeDeliverProperties(PropertiesContext context) {
        return context.getProperties();
    }
}
