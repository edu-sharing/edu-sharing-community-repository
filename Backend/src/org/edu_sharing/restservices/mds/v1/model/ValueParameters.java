package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class ValueParameters {
	
	private String query;
	
	private String property;
	
	private String pattern;
	
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("property")
	public String getProperty() {
		return property;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("query")
	public String getQuery() {
		return query;
	}
	
	
	
	@ApiModelProperty(required = true, value = "prefix of the value (or \"-all-\" for all values)")
	@JsonProperty("pattern")
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	public void setProperty(String property) {
		this.property = property;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	public String getPattern() {
		return pattern;
	}
	
}
