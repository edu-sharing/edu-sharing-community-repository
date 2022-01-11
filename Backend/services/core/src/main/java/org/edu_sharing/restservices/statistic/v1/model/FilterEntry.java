package org.edu_sharing.restservices.statistic.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class FilterEntry {

	String property;
	
	String[] values;
	
	public void setProperty(String property) {
		this.property = property;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("property")
	public String getProperty() {
		return property;
	}
	
	public void setValues(String[] values) {
		this.values = values;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("values")
	public String[] getValues() {
		return values;
	}
}
