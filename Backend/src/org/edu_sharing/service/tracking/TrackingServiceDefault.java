package org.edu_sharing.service.tracking;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TrackingServiceDefault implements TrackingService{
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
    public boolean trackActivityOnUser(String authorityName, EventType type) {
        return false;
    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef, EventType type) {
        Integer value= (Integer) nodeService.getProperty(nodeRef,QName.createQName(EVENT_PROPERTY_MAPPING.get(type)));
        if(value==null)
            value=0;

        value++;

        Integer finalValue = value;
        AuthenticationUtil.runAsSystem(()->{
            nodeService.setProperty(nodeRef, QName.createQName(EVENT_PROPERTY_MAPPING.get(type)), finalValue);
            return null;
        });
        return true;
    }

    /**
     * remove / annonymize / print the username for tracking
     * @return
     */
    protected String getTrackedUsername(String username) {
        if(username==null)
            username=AuthenticationUtil.getFullyAuthenticatedUser();

        String mode=RepoFactory.getEdusharingProperty(CCConstants.EDU_SHARING_PROPERTIES_PROPERTY_TRACKING_USER);
        if(mode==null)
            return null;
        if(mode.equalsIgnoreCase("obfuscate"))
            return DigestUtils.sha1Hex(username);
        if(mode.equalsIgnoreCase("full"))
            return username;
        return null;
    }
}
