package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class UserCredential {

	private String oldPassword = null;
	private String newPassword = null;

	/**
	 **/
	@ApiModelProperty(value = "")
	@JsonProperty("oldPassword")
	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	/**
	 **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("newPassword")
	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
}
