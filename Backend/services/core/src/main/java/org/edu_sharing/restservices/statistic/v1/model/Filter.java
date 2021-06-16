package org.edu_sharing.restservices.statistic.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Filter {
	
	List<FilterEntry> entries;
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("entries")
	public List<FilterEntry> getEntries() {
		return entries;
	}
	
	public void setEntries(List<FilterEntry> entries) {
		this.entries = entries;
	}
}
