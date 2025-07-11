package org.edu_sharing.repository.server.tracking;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.server.tools.XApiTool;
import org.edu_sharing.service.tracking.*;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;

public class TrackingTool {
    /**
     * Wrapper to use nodeId instead of NodeRef
     * @param nodeId
     * @param details
     * @param type
     */
    public static void trackActivityOnNode(String nodeId,NodeTrackingDetails details, ActivityOnNodeEventType type){
        trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),details,type);
    }
    public static void trackActivityOnNode(NodeRef nodeRef, NodeTrackingDetails details, ActivityOnNodeEventType type){
        ApplicationContext applicationContext = ApplicationContextFactory.getApplicationContext();
        ActivityEventService activityEventService = applicationContext.getBean(ActivityEventService.class);
        activityEventService.trackActivityOnNode(nodeRef,details,type, AuthenticationUtil.getFullyAuthenticatedUser());
        XApiTool.trackActivity(XApiTool.mapActivityVerb(type),nodeRef.getId(),details!=null ? details.getNodeVersion() : null);
    }
}
