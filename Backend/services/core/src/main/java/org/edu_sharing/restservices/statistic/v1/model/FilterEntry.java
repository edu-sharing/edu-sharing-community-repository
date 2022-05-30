package org.edu_sharing.restservices.statistic.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

public class FilterEntry {

	String property;
	
	String[] values;
	
	public void setProperty(String property) {
		this.property = property;
	}
	
	@Schema(required = true, description = "")
	@JsonProperty("property")
	public String getProperty() {
		return property;
	}
	
	public void setValues(String[] values) {
		this.values = values;
	}
	
	@Schema(required = true, description = "")
	@JsonProperty("values")
	public String[] getValues() {
		return values;
	}
}
