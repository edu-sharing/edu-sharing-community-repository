package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class Person {

	private String firstName = null;
	private String lastName = null;
	private String mailbox = null;
	private UserProfile profile = null;

	@ApiModelProperty(required = false, value = "")
	@JsonProperty("firstName")
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@ApiModelProperty(required = false, value = "")
	@JsonProperty("lastName")
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@ApiModelProperty(required = false, value = "")
	@JsonProperty("mailbox")
	public String getMailbox() {
		return mailbox;
	}

	public void setMailbox(String mailbox) {
		this.mailbox = mailbox;
	}
	
	@JsonProperty
	public UserProfile getProfile() {
		return profile;
	}

	public void setProfile(UserProfile profile) {
		this.profile = profile;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class PersonRef {\n");

		sb.append("  firstName: ").append(firstName).append("\n");
		sb.append("  lastName: ").append(lastName).append("\n");
		sb.append("  mailbox: ").append(mailbox).append("\n");
		sb.append("}\n");
		return sb.toString();
	}
}
