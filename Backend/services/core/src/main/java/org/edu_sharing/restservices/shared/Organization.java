package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.edu_sharing.service.organization.GroupSignupMethod;

@ApiModel(description = "")
public class Organization extends ManagableGroup {

	private NodeRef folderId = null;

	/**
	 **/
	@ApiModelProperty(value = "")
	@JsonProperty("sharedFolder")
	public NodeRef getSharedFolder() {
		return folderId;
	}

	public void setSharedFolder(NodeRef folderId) {
		this.folderId = folderId;
	}
}
