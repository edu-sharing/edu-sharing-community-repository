package org.edu_sharing.service.tracking;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.poi.ss.formula.functions.Even;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class TrackingServiceDefault implements TrackingService{
    private final NodeService nodeService;
    public static Map<EventType,String> EVENT_PROPERTY_MAPPING=new HashMap<>();
    static{
        EVENT_PROPERTY_MAPPING.put(EventType.DOWNLOAD_MATERIAL,CCConstants.CCM_PROP_TRACKING_DOWNLOADS);
        EVENT_PROPERTY_MAPPING.put(EventType.VIEW_MATERIAL,CCConstants.CCM_PROP_TRACKING_VIEWS);
    }

    public TrackingServiceDefault() {

        ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

        ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        nodeService=serviceRegistry.getNodeService();
    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef, EventType type) {
        Integer value= (Integer) nodeService.getProperty(nodeRef,QName.createQName(EVENT_PROPERTY_MAPPING.get(type)));
        if(value==null)
            value=0;

        value++;
        nodeService.setProperty(nodeRef,QName.createQName(EVENT_PROPERTY_MAPPING.get(type)),value);
        return true;
    }
}
