package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.UserSimple;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkflowHistory {
	@JsonProperty
	private long time;
	@JsonProperty
	private UserSimple editor;
	@JsonProperty
	private Authority[] receiver;
	@JsonProperty
	private String status;
	@JsonProperty
	private String comment;
	
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public UserSimple getEditor() {
		return editor;
	}
	public void setEditor(UserSimple editor) {
		this.editor = editor;
	}
	public Authority[] getReceiver() {
		return receiver;
	}
	public void setReceiver(Authority[] receiver) {
		this.receiver = receiver;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	
}
