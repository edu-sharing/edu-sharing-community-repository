package org.edu_sharing.restservices.stream.v1.model;

import java.util.List;
import java.util.Map;

import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StreamEntryInput {
	@JsonProperty private String id;
	@JsonProperty private String title;
	@JsonProperty private String description;
	@JsonProperty private List<String> nodes;
	@JsonProperty private Map<String,Object> properties;
	@JsonProperty private int priority;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<String> getNodes() {
		return nodes;
	}
	public void setNodes(List<String> nodes) {
		this.nodes = nodes;
	}
	public Map<String, Object> getProperties() {
		return properties;
	}
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}	
	
	
}
