package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;

public interface TrackingServiceCustomInterface {
    default JSONObject buildJson(ActivityOnNodeEvent event) {
        return null;
    }
    default JSONObject buildJson(UserActivityEvent event) {
        return null;
    }
}
