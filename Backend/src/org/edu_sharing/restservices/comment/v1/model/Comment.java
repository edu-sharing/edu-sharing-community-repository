package org.edu_sharing.restservices.comment.v1.model;

import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.Person;
import org.edu_sharing.restservices.shared.UserProfile;
import org.edu_sharing.service.config.model.Language;
import org.edu_sharing.service.config.model.Values;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Comment {
	@JsonProperty private NodeRef ref;
	@JsonProperty private UserProfile creator;
	@JsonProperty private long created;
	@JsonProperty private String comment;
	
	public NodeRef getRef() {
		return ref;
	}
	public void setRef(NodeRef ref) {
		this.ref = ref;
	}
	public UserProfile getCreator() {
		return creator;
	}
	public void setCreator(UserProfile creator) {
		this.creator = creator;
	}
	public long getCreated() {
		return created;
	}
	public void setCreated(long created) {
		this.created = created;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	
	
}
