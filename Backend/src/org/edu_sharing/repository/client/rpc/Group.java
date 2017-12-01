package org.edu_sharing.repository.client.rpc;

public class Group extends Authority {

	String name;
	
	String displayName;
	
	String repositoryId;
	
	String nodeId;
	
	String scope;
	
	String groupType;
	
	boolean editable = true;
	
	public Group() {
		super("GROUP");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		setAuthorityName(this.name = name);
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	
	public String getRepositoryId() {
		return repositoryId;
	}
	
	public String getNodeId() {
		return nodeId;
	}
	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public String getAuthorityDisplayName() {
		return getDisplayName();
	}
	
	public void setScope(String scope) {
		this.scope = scope;
	}
	
	public String getScope() {
		return scope;
	}
	
	public String getGroupType() {
		return groupType;
	}
	
	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public boolean isEditable() {
		return editable;
	}
	
}
