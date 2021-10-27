package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;
import org.edu_sharing.service.organization.GroupSignupMethod;

@Schema(description = "")
public class Organization extends ManagableGroup {

	private NodeRef folderId = null;

	/**
	 **/
	@Schema(description = "")
	@JsonProperty("sharedFolder")
	public NodeRef getSharedFolder() {
		return folderId;
	}

	public void setSharedFolder(NodeRef folderId) {
		this.folderId = folderId;
	}
}
