package org.edu_sharing.restservices.iam.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ShowHideEmail {
	@ApiModelProperty(required = true, value = "false")
	@JsonProperty("showHideEmail")
	private boolean showHideEmail; // show or hide email in profile

	public boolean getShowHideEmail() {
		return showHideEmail;
	}

	public void setShowHideEmail(boolean showHideEmail) {
		this.showHideEmail = showHideEmail;
	}
}
