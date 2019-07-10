package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.edu_sharing.service.tracking.model.StatisticEntryNode;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TrackingService {
    enum GroupingType {
        None,
        Daily,
        Monthly,
        Yearly
    }
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
    List<StatisticEntryNode> getNodeStatisics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable;
    List<StatisticEntry> getUserStatistics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable;
}
