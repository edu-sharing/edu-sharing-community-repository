package org.edu_sharing.service.archive.model;

public class RestoreResult {
	String archiveNodeId;
	String nodeId;
	
	String parent;
	String path;
	
	String restoreStatus;
	
	String name;


	public String getArchiveNodeId() {
		return archiveNodeId;
	}


	public void setArchiveNodeId(String archiveNodeId) {
		this.archiveNodeId = archiveNodeId;
	}


	public String getNodeId() {
		return nodeId;
	}


	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}


	public String getParent() {
		return parent;
	}


	public void setParent(String parent) {
		this.parent = parent;
	}


	public String getRestoreStatus() {
		return restoreStatus;
	}


	public void setRestoreStatus(String fallBackTargetCause) {
		this.restoreStatus = fallBackTargetCause;
	}


	public String getPath() {
		return path;
	}


	public void setPath(String path) {
		this.path = path;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}
	
	
}
