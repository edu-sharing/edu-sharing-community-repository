package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class NodeRemote {
	Node node;
	Node remote;
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("node")
	public Node getNode() {
		return node;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("remote")
	public Node getRemote() {
		return remote;
	}
	
	public void setNode(Node node) {
		this.node = node;
	}
	
	public void setRemote(Node remote) {
		this.remote = remote;
	}
}
