package org.edu_sharing.restservices.archive.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class RestoreResult {
	String archiveNodeId;
	String nodeId;
	
	String parent;
	String path;
	String name;
	
	String restoreStatus;


	@ApiModelProperty(required = true, value = "")
	@JsonProperty("archiveNodeId")
	public String getArchiveNodeId() {
		return archiveNodeId;
	}

	public void setArchiveNodeId(String archiveNodeId) {
		this.archiveNodeId = archiveNodeId;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("nodeId")
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String restoredNodeId) {
		this.nodeId = restoredNodeId;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("parent")
	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("restoreStatus")
	public String getRestoreStatus() {
		return restoreStatus;
	}

	public void setRestoreStatus(String restoreStatus) {
		this.restoreStatus = restoreStatus;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("path")
	public String getPath() {
		return path;
	}

	public void setPath(String resultPath) {
		this.path = resultPath;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	


}
