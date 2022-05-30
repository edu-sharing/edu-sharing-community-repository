package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(description = "")
public class NodeRemote {
	Node node;
	Node remote;
	
	@Schema(required = true, description = "")
	@JsonProperty("node")
	public Node getNode() {
		return node;
	}
	
	@Schema(required = true, description = "")
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
