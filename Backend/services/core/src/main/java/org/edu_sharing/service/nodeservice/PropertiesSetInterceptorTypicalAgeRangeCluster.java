package org.edu_sharing.service.nodeservice;

import java.util.Map;

/**
 * Interceptor that handles copying data from ccm:educationaltypicalagerangecluster into ccm:educationaltypicalagerange
 */
public class PropertiesSetInterceptorTypicalAgeRangeCluster implements PropertiesSetInterceptor{
    @Override
    public Map<String, Object> beforeSetProperties(PropertiesGetInterceptor.PropertiesContext context) {
        // @TODO: map CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGECLUSTER to CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE
        return context.getProperties();
    }

    @Override
    public SetInterceptorTiming getInterceptorTiming() {
        return SetInterceptorTiming.AfterAlfrescoInterceptors;
    }
}
