package org.edu_sharing.restservices.iam.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Preferences {
	@JsonProperty
	private String preferences;

	public String getPreferences() {
		return preferences;
	}

	public void setPreferences(String preferences) {
		this.preferences = preferences;
	}
	
	
}
