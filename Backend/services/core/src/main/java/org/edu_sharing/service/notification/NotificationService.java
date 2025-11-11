package org.edu_sharing.service.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.alfresco.service.config.model.ConfigRating;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.event.NotificationEventDTO;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.rating.RatingDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    enum NotifyMode {
        ReportProblem,
        Feedback,
    }
    @Getter
    @Setter
    @AllArgsConstructor
    class NodeContext {
        private String nodeId;
        private List<String> aspects;
        private Map<String, Object> properties;
    }
    interface NodeIssueMapping {
        String getTemplateId(NodeContext context);
        List<String> getReceivers(NodeContext context);
    }

    void notifyNodeIssue(String nodeId, NotifyMode mode, String reason, String nodeType, List<String> aspects, Map<String, Object> properties, String userEmail, String userComment) throws Throwable;

    void notifyWorkflowChanged(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, String receiver, String comment, String status);

    void notifyPersonStatusChanged(String receiver, String firstname, String lastName, String oldStatus, String newStatus);

    void notifyPermissionChanged(String senderAuthority, String receiverAuthority, String nodeId, String nodeType, List<String> aspects, Map<String, Object> props, String[] permissions, String mailText) throws Throwable;

    void notifyMetadataSetSuggestion(MdsValue mdsValue, MetadataWidget widgetDefinition, List<String> nodeIds, List<String> nodeType, List<List<String>> aspects, List<Map<String, Object>> nodeProperties) throws Throwable;

    void notifyComment(String node, String comment, String commentReference, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, Status status);

    void notifyAddCollection(String collectionId, String nodeId, String collectionNodeType, List<String> collectionAspects, Map<String, Object> collectionProperties, String nodeType, List<String> nodeAspects, Map<String, Object> nodeProperties, Status status);

    void notifyProposeForCollection(String collectionId, String nodeId, String collectionNodeType, List<String> collectionAspects, Map<String, Object> collectionProperties, String nodeType, List<String> nodeAspects, Map<String, Object> nodeProperties, Status status);

    void notifyRatingChanged(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProps, ConfigRating.RatingMode ratingMode, Double rating, RatingDetails accumulatedRatings, Status removed);

    void notifyMaterialAddedToInbox(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, String comment, String senderAuthority, String receiverAuthority);

    @Permission(requiresUser = true)
    NotificationConfig getConfig() throws Exception;

    @Permission(requiresUser = true)
    void setConfig(NotificationConfig config) throws Exception;


    @Permission(requiresUser = true)
    Page<NotificationEventDTO> getNotifications(String receiverId, List<StatusDTO> status, Pageable pageable);

    @Permission(requiresUser = true)
    NotificationEventDTO setNotificationStatusByNotificationId(String id, StatusDTO status);

    @Permission(requiresUser = true)
    void setNotificationStatusByReceiverId(String receiverId, List<StatusDTO> oldStatusList, StatusDTO newStatus);

    @Permission(requiresUser = true)
    void deleteNotification(String id);


}
