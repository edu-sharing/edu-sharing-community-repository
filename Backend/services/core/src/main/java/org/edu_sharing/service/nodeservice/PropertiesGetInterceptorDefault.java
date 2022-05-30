package org.edu_sharing.service.nodeservice;


import org.alfresco.service.ServiceRegistry;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public class PropertiesGetInterceptorDefault implements PropertiesGetInterceptor {

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    @Override
    public Map<String, Object> beforeCacheProperties(PropertiesContext context) {
        return context.getProperties();
    }

    @Override
    public Map<String, Object> beforeDeliverProperties(PropertiesContext context) {
        return context.getProperties();
    }
}
