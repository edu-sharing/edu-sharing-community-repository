package org.edu_sharing.restservices.archive.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class RestoreResults {
	List<RestoreResult> results;
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("results")
	public List<RestoreResult> getResults() {
		return results;
	}
	
	public void setResults(List<RestoreResult> results) {
		this.results = results;
	}
}
