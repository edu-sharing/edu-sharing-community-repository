package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;

public interface TrackingServiceCustomInterface {
    JSONObject buildJson(ActivityOnNodeEvent event);
    JSONObject buildJson(UserActivityEvent event);
}
