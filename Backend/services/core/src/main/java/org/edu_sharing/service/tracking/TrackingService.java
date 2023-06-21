package org.edu_sharing.service.tracking;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.tracking.ibatis.NodeData;
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
        Yearly,
        Node,
    }
    enum EventType {
        DOWNLOAD_MATERIAL,
        VIEW_MATERIAL,
        VIEW_MATERIAL_EMBEDDED,
        VIEW_MATERIAL_PLAY_MEDIA, // When a video or audio file is actually started playing
        LOGIN_USER_SESSION,
        LOGIN_USER_OAUTH_PASSWORD,
        LOGIN_USER_OAUTH_REFRESH_TOKEN,
        LOGOUT_USER_TIMEOUT,
        LOGOUT_USER_REGULAR
    }
    List<String> getAlteredNodes(java.util.Date from);
    List<NodeData> getNodeData(String nodeId, java.util.Date from);
    boolean trackActivityOnUser(String authorityName,EventType type);

    /**
     *
     * @param nodeRef
     * @param details
     * @param type
     * @param authorityName might be null (then the current authenticated user is used)
     * @return
     */
    boolean trackActivityOnNode(NodeRef nodeRef,NodeTrackingDetails details,EventType type, String authorityName);
    default boolean trackActivityOnNode(NodeRef nodeRef, NodeTrackingDetails details, EventType type) {
        return trackActivityOnNode(nodeRef, details, type, null);
    }
    List<StatisticEntryNode> getNodeStatisics(GroupingType type, Date dateFrom, Date dateTo, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable;
    List<StatisticEntry> getUserStatistics(GroupingType type, java.util.Date dateFrom, java.util.Date dateTo, String mediacenter, List<String> additionalFields, List<String> groupFields, Map<String, String> filters) throws Throwable;
    StatisticEntry getSingleNodeData(NodeRef nodeRef,java.util.Date dateFrom,java.util.Date dateTo) throws Throwable;
    Map<NodeRef, StatisticEntry> getListNodeData(List<NodeRef> nodes, java.util.Date dateFrom, java.util.Date dateTo, List<String> additionalFields, String mediacenter) throws Throwable;

    /**
     * delete all tracked data for a given user
     * This method can do nothing, depending on the configured @UserTrackingMode
     * It will only do something if the mode is NOT set to "none"
     */
    void deleteUserData(String username) throws Throwable;
    /**
     * reassign all tracked data for a given user to a new user (usually a dummy)
     * This method can do nothing, depending on the configured @UserTrackingMode
     * It will only do something if the mode is NOT set to "none"
     */
    void reassignUserData(String oldUsername, String newUsername);

    enum UserTrackingMode{
        none,
        obfuscate,
        session,
        full
    }
}
