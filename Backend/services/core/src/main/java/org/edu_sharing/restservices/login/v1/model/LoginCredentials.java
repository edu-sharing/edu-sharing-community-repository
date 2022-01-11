package org.edu_sharing.restservices.login.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class LoginCredentials {

	String userName;
	
	String password;
	
	String scope;

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("userName")
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("password")
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setScope(String scope) {
		this.scope = scope;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("scope")
	public String getScope() {
		return scope;
	}
}
