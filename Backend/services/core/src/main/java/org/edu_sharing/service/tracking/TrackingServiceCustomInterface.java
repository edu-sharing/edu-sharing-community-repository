package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;

public interface TrackingServiceCustomInterface {
    JSONObject buildJson(NodeRef nodeRef, NodeTrackingDetails details, TrackingService.EventType type);
    JSONObject buildJson(String authorityName, TrackingService.EventType type);
}
