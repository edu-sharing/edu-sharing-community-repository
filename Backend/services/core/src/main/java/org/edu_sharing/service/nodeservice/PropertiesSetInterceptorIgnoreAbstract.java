package org.edu_sharing.service.nodeservice;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Interceptor that will remove/ignore all configured properties
 */
public class PropertiesSetInterceptorIgnoreAbstract implements PropertiesSetInterceptor{

    private final List<String> ignoredProperties;

    public PropertiesSetInterceptorIgnoreAbstract(List<String> ignoredProperties) {
        this.ignoredProperties = ignoredProperties;
    }

    @Override
    public Map<String, Object> beforeSetProperties(PropertiesGetInterceptor.PropertiesContext context) {
        ignoredProperties.forEach((p) -> context.getProperties().remove(p));
        return context.getProperties();
    }
}
