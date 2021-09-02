package org.edu_sharing.service.nodeservice;


import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertiesInterceptorImpl implements PropertiesInterceptor {

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
