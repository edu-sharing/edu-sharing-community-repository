package org.edu_sharing.service.notification;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;

import java.util.HashMap;
import java.util.List;

public interface NotificationService {
	void notifyNodeIssue(String nodeId,String reason,String userEmail,String userComment) throws Throwable;

	void notifyWorkflowChanged(String nodeId, HashMap<String, Object> nodeProperties, String receiver, String comment, String status);

	void notifyPersonStatusChanged(String receiver, String firstname, String lastName, String oldStatus, String newStatus);

    void notifyPermissionChanged(String senderAuthority, String receiverAuthority, String nodeId, String[] permissions, String mailText) throws Throwable;

	void notifyGroupSignupList(String groupEmail, String groupName, NodeRef userRef) throws Exception;

	void notifyGroupSignupUser(String userEmail, String groupName, NodeRef userRef) throws Exception;

	void notifyGroupSignupAdmin(String groupEmail, String groupName, NodeRef userRef) throws Exception;

	void notifyGroupSignupHandeld(NodeRef userRef, String groupName, boolean add) throws Exception;

	void notifyMetadataSetSuggestion(MdsValue mdsValue, MetadataWidget widgetDefinition, List<String> nodes) throws Throwable;
}
