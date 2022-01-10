package org.edu_sharing.restservices.login.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

public class LoginCredentials {

	String userName;
	
	String password;
	
	String scope;

	@Schema(required = true, description = "")
	@JsonProperty("userName")
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Schema(required = true, description = "")
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
	
	@Schema(required = true, description = "")
	@JsonProperty("scope")
	public String getScope() {
		return scope;
	}
}
