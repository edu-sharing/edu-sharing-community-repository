package org.edu_sharing.service.admin.model;

public class GlobalGroup {

	String name;
	
	String displayName;
	
	
	String nodeId;
	
	String scope;

	String authorityType;
	String groupType;
	
	public String getGroupType() {
		return groupType;
	}

	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}

	public GlobalGroup() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getNodeId() {
		return nodeId;
	}
	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}
	
	public String getScope() {
		return scope;
	}

	public void setAuthorityType(String authorityType) {
		this.authorityType=authorityType;
	}
	
}
