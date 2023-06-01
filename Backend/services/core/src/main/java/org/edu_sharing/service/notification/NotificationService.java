package org.edu_sharing.service.notification;

import com.google.gson.Gson;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;
import org.edu_sharing.service.nodeservice.annotation.NodeOriginal;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.rating.RatingDetails;

import java.io.Serializable;
import java.util.HashMap;
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

	@Permission(requiresUser = true)
	default NotificationConfig getConfig() throws Exception {
		HashMap<String, String> info = new MCAlfrescoAPIClient().getUserInfo(AuthenticationUtil.getFullyAuthenticatedUser());
		if(info.get(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES) != null){
			return new Gson().fromJson(info.get(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES), NotificationConfig.class);
		}
		return new NotificationConfig();
	}
	@Permission(requiresUser = true)
    default void setConfig(NotificationConfig config) throws Exception {
		HashMap<String, Serializable> userInfo = new HashMap<>();
		userInfo.put(CCConstants.PROP_USERNAME, AuthenticationUtil.getFullyAuthenticatedUser());
		userInfo.put(CCConstants.CCM_PROP_PERSON_NOTIFICATION_PREFERENCES, new Gson().toJson(config));
		new MCAlfrescoAPIClient().updateUser(userInfo);
	}
}
