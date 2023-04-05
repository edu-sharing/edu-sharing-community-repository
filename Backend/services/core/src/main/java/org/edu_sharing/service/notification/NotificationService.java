package org.edu_sharing.service.notification;

import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;
import org.edu_sharing.service.rating.RatingDetails;

import java.util.HashMap;
import java.util.List;

public interface NotificationService {
	void notifyNodeIssue(String nodeId,String reason,String userEmail,String userComment) throws Throwable;

	void notifyWorkflowChanged(String nodeId, HashMap<String, Object> nodeProperties, String receiver, String comment, String status);

	void notifyPersonStatusChanged(String receiver, String firstname, String lastName, String oldStatus, String newStatus);

    void notifyPermissionChanged(String senderAuthority, String receiverAuthority, String nodeId, String[] permissions, String mailText) throws Throwable;

	void notifyMetadataSetSuggestion(MdsValue mdsValue, MetadataWidget widgetDefinition, List<String> nodes) throws Throwable;

	void notifyComment(String node, String comment, String commentReference, HashMap<String, Object> nodeProperties, Status status);

	void notifyCollection(String collectionId, String refNodeId, HashMap<String, Object> collectionProperties, HashMap<String, Object> nodeProperties, Status status);

	void notifyRatingChanged(String nodeId, HashMap<String, Object> nodeProps, Double rating, RatingDetails accumulatedRatings, Status removed);
}
