package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;

public class Group implements Serializable {

	String name;
	
	String displayName;
	
	String repositoryId;
	
	String nodeId;
	
	String authorityName;
	String authorityType;
	
	String groupType;
	
	boolean editable = true;
	
	public Group() {
		authorityType = "GROUP";
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

	
	public String getAuthorityDisplayName() {
		return getDisplayName();
	}
	
	
	public void setAuthorityName(String authorityName) {
		this.authorityName = authorityName;
	}

	public String getAuthorityName() {
		return this.authorityName;
	}
	
	
	public void setAuthorityType(String authorityType) {
		this.authorityType = authorityType;
	}

	public String getAuthorityType() {
		return this.authorityType;
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
