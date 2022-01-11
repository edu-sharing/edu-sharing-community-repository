package org.edu_sharing.restservices.comment.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Comments {
	@JsonProperty private List<Comment> comments;

	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
	
	
}
