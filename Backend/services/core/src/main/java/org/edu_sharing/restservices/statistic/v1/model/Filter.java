package org.edu_sharing.restservices.statistic.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

public class Filter {
	
	List<FilterEntry> entries;
	
	@Schema(required = true, description = "")
	@JsonProperty("entries")
	public List<FilterEntry> getEntries() {
		return entries;
	}
	
	public void setEntries(List<FilterEntry> entries) {
		this.entries = entries;
	}
}
