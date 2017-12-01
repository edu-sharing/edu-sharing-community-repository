package org.edu_sharing.restservices.statistic.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class StatisticEntity {
	String value;
	Integer count;
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("count")
	public Integer getCount() {
		return count;
	}
	
	public void setCount(Integer count) {
		this.count = count;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("value")
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	
}
