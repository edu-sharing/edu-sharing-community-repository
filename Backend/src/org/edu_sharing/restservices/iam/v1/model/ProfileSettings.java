package org.edu_sharing.restservices.iam.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ProfileSettings {
	@ApiModelProperty(required = true, value = "false")
	@JsonProperty("showEmail")
	private boolean showEmail; // show or hide email in profile

	public boolean getShowEmail() {
		return showEmail;
	}

	public void setShowEmail(boolean showEmail) {
		this.showEmail = showEmail;
	}
}
