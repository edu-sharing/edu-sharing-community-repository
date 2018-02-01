package org.edu_sharing.restservices.stream.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StreamEntry {
	@JsonProperty private String id;
	@JsonProperty private List<Node> nodes;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public List<Node> getNodes() {
		return nodes;
	}
	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}
	
	
}
