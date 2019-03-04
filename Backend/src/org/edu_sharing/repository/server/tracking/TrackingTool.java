package org.edu_sharing.repository.server.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.server.tools.XApiTool;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;

public class TrackingTool {
    /**
     * Wrapper to use nodeId instead of NodeRef
     * @param nodeId
     * @param nodeVersion
     * @param type
     */
    public static void trackActivityOnNode(String nodeId,String nodeVersion, TrackingService.EventType type){
        trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),nodeVersion,type);
    }
    public static void trackActivityOnNode(NodeRef nodeRef,String nodeVersion, TrackingService.EventType type){
        TrackingServiceFactory.getTrackingService().trackActivityOnNode(nodeRef,nodeVersion,type);
        XApiTool.trackActivity(XApiTool.mapActivityVerb(type),nodeRef.getId(),nodeVersion);
    }
}
