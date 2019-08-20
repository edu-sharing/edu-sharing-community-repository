package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.util.Date;
import java.util.List;

public interface TrackingService {
    enum EventType {
        DOWNLOAD_MATERIAL,
        VIEW_MATERIAL,
        VIEW_MATERIAL_EMBEDDED,
        LOGIN_USER_SESSION,
        LOGIN_USER_OAUTH_PASSWORD,
        LOGIN_USER_OAUTH_REFRESH_TOKEN,
        LOGOUT_USER_TIMEOUT,
        LOGOUT_USER_REGULAR
    }
    boolean trackActivityOnUser(String authorityName,EventType type);
    boolean trackActivityOnNode(NodeRef nodeRef,NodeTrackingDetails details,EventType type);
    List<StatisticEntryNode> getNodeStatisics(Date dateFrom, Date dateTo);
}
