package org.edu_sharing.restservices.admin.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadResult {
	@JsonProperty
	private String file;
	public UploadResult(String file) {
		this.file=file;
	}
	public UploadResult(){}
}
