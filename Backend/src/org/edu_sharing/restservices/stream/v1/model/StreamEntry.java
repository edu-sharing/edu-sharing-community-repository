package org.edu_sharing.restservices.stream.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Person;
import org.edu_sharing.restservices.shared.UserSimple;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StreamEntry {
	@JsonProperty private String id;
	@JsonProperty private String description;
	@JsonProperty private List<Node> nodes;
	@JsonProperty private List<String> categories;
	@JsonProperty private int priority;
	@JsonProperty private UserSimple author;
	@JsonProperty private long created;
	@JsonProperty private long modified;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<Node> getNodes() {
		return nodes;
	}
	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}
	public List<String> getCategories() {
		return categories;
	}
	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public UserSimple getAuthor() {
		return author;
	}
	public void setAuthor(UserSimple author) {
		this.author = author;
	}
	public long getCreated() {
		return created;
	}
	public void setCreated(long created) {
		this.created = created;
	}
	public long getModified() {
		return modified;
	}
	public void setModified(long modified) {
		this.modified = modified;
	}
	
}
