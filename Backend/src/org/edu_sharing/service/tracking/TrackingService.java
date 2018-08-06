package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.stream.model.ContentEntry;
import org.edu_sharing.service.stream.model.ContentEntry.Audience.STATUS;
import org.edu_sharing.service.stream.model.ScoreResult;
import org.edu_sharing.service.stream.model.StreamSearchRequest;
import org.edu_sharing.service.stream.model.StreamSearchResult;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface TrackingService {
    enum EventType{
        DOWNLOAD_MATERIAL,
        VIEW_MATERIAL
    }

    boolean trackActivityOnNode(NodeRef nodeRef,EventType type);

}
