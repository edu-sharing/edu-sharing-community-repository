package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class NodeLocked {

	boolean locked = false;

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("isLocked")
	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
}
