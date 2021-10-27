package org.edu_sharing.restservices.archive.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

public class RestoreResult {
	String archiveNodeId;
	String nodeId;
	
	String parent;
	String path;
	String name;
	
	String restoreStatus;


	@Schema(required = true, description = "")
	@JsonProperty("archiveNodeId")
	public String getArchiveNodeId() {
		return archiveNodeId;
	}

	public void setArchiveNodeId(String archiveNodeId) {
		this.archiveNodeId = archiveNodeId;
	}

	@Schema(required = true, description = "")
	@JsonProperty("nodeId")
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String restoredNodeId) {
		this.nodeId = restoredNodeId;
	}

	@Schema(required = true, description = "")
	@JsonProperty("parent")
	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@Schema(required = true, description = "")
	@JsonProperty("restoreStatus")
	public String getRestoreStatus() {
		return restoreStatus;
	}

	public void setRestoreStatus(String restoreStatus) {
		this.restoreStatus = restoreStatus;
	}

	@Schema(required = true, description = "")
	@JsonProperty("path")
	public String getPath() {
		return path;
	}

	public void setPath(String resultPath) {
		this.path = resultPath;
	}

	@Schema(required = true, description = "")
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	


}
