package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;
import java.util.Map;

public interface TrackingService {
    enum EventType{
        DOWNLOAD_MATERIAL,
        VIEW_MATERIAL
    }

    boolean trackActivityOnNode(NodeRef nodeRef,EventType type);

}
