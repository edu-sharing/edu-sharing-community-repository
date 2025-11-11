package org.edu_sharing.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.service.config.model.ConfigRating;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.event.NotificationEventDTO;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;
import org.edu_sharing.service.notification.events.*;
import org.edu_sharing.service.rating.RatingDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service("notificationService")
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Optional<NotificationProxyService> notificationProxyService;

    private static IllegalStateException NotificationProxyServiceNotAvailableException() {
        return new IllegalStateException("Notification proxy service is not available");
    }

    @Override
    public void notifyNodeIssue(String nodeId, NotifyMode mode, String reason, String nodeType, List<String> aspects, Map<String, Object> properties, String userEmail, String userComment) {
        applicationEventPublisher.publishEvent(new NodeIssueEvent(nodeId, mode, reason, nodeType, aspects, properties, userEmail, userComment));
    }

    @Override
    public void notifyWorkflowChanged(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, String receiver, String comment, String status) {
        applicationEventPublisher.publishEvent(new WorkflowChangedEvent(nodeId, nodeType, aspects, nodeProperties, receiver, comment, status));
    }

    @Override
    public void notifyPersonStatusChanged(String receiver, String firstname, String lastName, String oldStatus, String newStatus) {
        applicationEventPublisher.publishEvent(new PersonStatusChangedEvent(receiver, firstname, lastName, oldStatus, newStatus));
    }

    @Override
    public void notifyPermissionChanged(String senderAuthority, String receiverAuthority, String nodeId, String nodeType, List<String> aspects, Map<String, Object> props, String[] permissions, String mailText) {
        applicationEventPublisher.publishEvent(new PermissionChangedEvent(senderAuthority, receiverAuthority, nodeId, nodeType, aspects, props, permissions, mailText));
    }

    public void notifyMetadataSetSuggestion(MdsValue mdsValue, MetadataWidget widgetDefinition, List<String> nodeId, List<String> nodeType, List<List<String>> aspects, List<Map<String, Object>> nodePropertiesList) {
        applicationEventPublisher.publishEvent(new MetadataSetSuggestionEvent(mdsValue, widgetDefinition, nodeId, nodeType, aspects, nodePropertiesList));
    }

    @Override
    public void notifyComment(String node, String comment, String commentReference, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, Status status) {
        applicationEventPublisher.publishEvent(new CommentEvent(node, comment, commentReference, nodeType, aspects, nodeProperties, status));
    }

    @Override
    public void notifyAddCollection(String collectionId, String refNodeId, String collectionType, List<String> collectionAspects, Map<String, Object> collectionProperties, String nodeType, List<String> nodeAspects, Map<String, Object> nodeProperties, Status status) {
        applicationEventPublisher.publishEvent(new AddToCollectionEvent(collectionId, refNodeId, collectionType, collectionAspects, collectionProperties, nodeType, nodeAspects, nodeProperties, status));
    }

    @Override
    public void notifyProposeForCollection(String collectionId, String refNodeId, String collectionType, List<String> collectionAspects, Map<String, Object> collectionProperties, String nodeType, List<String> nodeAspects, Map<String, Object> nodeProperties, Status status) {
        applicationEventPublisher.publishEvent(new ProposeForCollectionEvent(collectionId, refNodeId, collectionType, collectionAspects, collectionProperties, nodeType, nodeAspects, nodeProperties, status));
    }

    @Override
    public void notifyRatingChanged(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProps, ConfigRating.RatingMode ratingMode, Double rating, RatingDetails accumulatedRatings, Status removed) {
        applicationEventPublisher.publishEvent(new RatingChangedEvent(nodeId, nodeType, aspects, nodeProps, ratingMode, rating, accumulatedRatings, removed));
    }

    @Override
    public void notifyMaterialAddedToInbox(String nodeId, String nodeType, List<String> aspects, Map<String, Object> nodeProperties, String comment, String senderAuthority, String receiverAuthority) {
        applicationEventPublisher.publishEvent(new AddedToInboxEvent(senderAuthority, receiverAuthority, nodeId, nodeType, aspects, nodeProperties, comment));
    }

    @Override
    public Page<NotificationEventDTO> getNotifications(String receiverId, List<StatusDTO> status, Pageable pageable) {
        return  notificationProxyService.orElseThrow(NotificationServiceImpl::NotificationProxyServiceNotAvailableException)
                .getNotifications(receiverId, status, pageable);
    }

    @Override
    public NotificationEventDTO setNotificationStatusByNotificationId(String id, StatusDTO status) {
        return notificationProxyService.orElseThrow(NotificationServiceImpl::NotificationProxyServiceNotAvailableException)
                .setNotificationStatusByNotificationId(id, status);
    }

    @Override
    public void setNotificationStatusByReceiverId(String receiverId, List<StatusDTO> oldStatusList, StatusDTO newStatus) {
        notificationProxyService.orElseThrow(NotificationServiceImpl::NotificationProxyServiceNotAvailableException)
                .setNotificationStatusByReceiverId(receiverId, oldStatusList, newStatus);
    }

    @Override
    public void deleteNotification(String id) {
        notificationProxyService.orElseThrow(NotificationServiceImpl::NotificationProxyServiceNotAvailableException)
                .deleteNotification(id);
    }

    @Override
    public NotificationConfig getConfig() throws Exception {
        Map<String, String> info = new MCAlfrescoAPIClient().getUserInfo(AuthenticationUtil.getFullyAuthenticatedUser());
        if (!StringUtils.isEmpty(info.get(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES))) {
            return new ObjectMapper().readValue(info.get(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES), NotificationConfig.class);
        }
        return new NotificationConfig();
    }

    @Override
    public void setConfig(NotificationConfig config) throws Exception {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put(CCConstants.PROP_USERNAME, AuthenticationUtil.getFullyAuthenticatedUser());
        userInfo.put(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES, new ObjectMapper().writeValueAsString(config));
        new MCAlfrescoAPIClient().updateUser(userInfo);
    }

}
