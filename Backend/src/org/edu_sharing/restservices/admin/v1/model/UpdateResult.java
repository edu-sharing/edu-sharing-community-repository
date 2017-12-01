package org.edu_sharing.restservices.admin.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateResult {
	@JsonProperty
	private String result;
	public UpdateResult(String result) {
		this.result=result;
	}
	public UpdateResult(){}
}
