package org.edu_sharing.restservices.iam.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(description = "")
public class ProfileSettings {
	@Schema(required = true, description = "false")
	@JsonProperty("showEmail")
	private boolean showEmail; // show or hide email in profile

	public boolean getShowEmail() {
		return showEmail;
	}

	public void setShowEmail(boolean showEmail) {
		this.showEmail = showEmail;
	}
}
