package org.edu_sharing.repository.server.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.server.tools.XApiTool;
import org.edu_sharing.service.tracking.NodeTrackingDetails;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;

public class TrackingTool {
    /**
     * Wrapper to use nodeId instead of NodeRef
     * @param nodeId
     * @param details
     * @param type
     */
    public static void trackActivityOnNode(String nodeId,NodeTrackingDetails details, TrackingService.EventType type){
        trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),details,type);
    }
    public static void trackActivityOnNode(NodeRef nodeRef, NodeTrackingDetails details, TrackingService.EventType type){
        TrackingServiceFactory.getTrackingService().trackActivityOnNode(nodeRef,details,type);
        XApiTool.trackActivity(XApiTool.mapActivityVerb(type),nodeRef.getId(),details!=null ? details.getNodeVersion() : null);
    }
}
