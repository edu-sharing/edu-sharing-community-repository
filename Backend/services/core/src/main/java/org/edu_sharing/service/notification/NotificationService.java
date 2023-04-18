package org.edu_sharing.service.notification;

import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;
import org.edu_sharing.service.rating.RatingDetails;

import java.util.List;
import java.util.Map;

public interface NotificationService {
	void notifyNodeIssue(String nodeId, String reason, Map<String, Object> properties, String userEmail, String userComment) throws Throwable;

	void notifyWorkflowChanged(String nodeId, Map<String, Object> nodeProperties, String receiver, String comment, String status);

	void notifyPersonStatusChanged(String receiver, String firstname, String lastName, String oldStatus, String newStatus);

    void notifyPermissionChanged(String senderAuthority, String receiverAuthority, String nodeId, Map<String, Object> props, String[] permissions, String[] strings, String mailText) throws Throwable;

	void notifyMetadataSetSuggestion(MdsValue mdsValue, MetadataWidget widgetDefinition, List<String> nodeIds, List<Map<String, Object>> nodeProperties) throws Throwable;

	void notifyComment(String node, String comment, String commentReference, Map<String, Object> nodeProperties, Status status);

	void notifyCollection(String collectionId, String refNodeId, Map<String, Object> collectionProperties, Map<String, Object> nodeProperties, Status status);

	void notifyRatingChanged(String nodeId, Map<String, Object> nodeProps, Double rating, RatingDetails accumulatedRatings, Status removed);
}
