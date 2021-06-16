package org.edu_sharing.webservices.alfresco.extension;

public class GroupDetails implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	private String nodeId;
	private String groupName;
	private String displayName;
	private String homeFolderId;
	
	public GroupDetails() {
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getHomeFolderId() {
		return homeFolderId;
	}

	public void setHomeFolderId(String homeFolderId) {
		this.homeFolderId = homeFolderId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

}
