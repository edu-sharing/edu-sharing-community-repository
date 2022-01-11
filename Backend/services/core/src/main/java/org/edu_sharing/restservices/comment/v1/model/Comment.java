package org.edu_sharing.restservices.comment.v1.model;

import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.UserSimple;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Comment {
	@JsonProperty private NodeRef ref;
	@JsonProperty private NodeRef replyTo;
	@JsonProperty private UserSimple creator;
	@JsonProperty private long created;
	@JsonProperty private String comment;
	
	public NodeRef getRef() {
		return ref;
	}
	public void setRef(NodeRef ref) {
		this.ref = ref;
	}
	public NodeRef getReplyTo() {
		return replyTo;
	}
	public void setReplyTo(NodeRef replyTo) {
		this.replyTo = replyTo;
	}
	public UserSimple getCreator() {
		return creator;
	}
	public void setCreator(UserSimple userSimple) {
		this.creator = userSimple;
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
