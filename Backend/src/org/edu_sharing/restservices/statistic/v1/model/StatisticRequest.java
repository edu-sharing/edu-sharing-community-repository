package org.edu_sharing.restservices.statistic.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "settings for statistic request")
public class StatisticRequest {
	
	Filter filter;
	
	List<String> properties = new ArrayList<String>();
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("filter")
	public Filter getFilter() {
		return filter;
	}
	public void setFilter(Filter filter) {
		this.filter = filter;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("properties")
	public List<String> getProperties() {
		return properties;
	}
	
	public void setProperties(List<String> properties) {
		this.properties = properties;
	}
	
}
