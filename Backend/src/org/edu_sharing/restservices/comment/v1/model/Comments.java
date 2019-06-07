package org.edu_sharing.restservices.comment.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.Person;
import org.edu_sharing.service.config.model.Language;
import org.edu_sharing.service.config.model.Values;

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
