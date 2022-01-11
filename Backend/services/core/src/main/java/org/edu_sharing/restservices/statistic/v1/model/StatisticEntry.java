package org.edu_sharing.restservices.statistic.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class StatisticEntry {
	
	String property;
	
	List<StatisticEntity> entities = new ArrayList<StatisticEntity>();
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("property")
	public String getProperty() {
		return property;
	}
	
	public void setProperty(String property) {
		this.property = property;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("entities")
	public List<StatisticEntity> getEntities() {
		return entities;
	}
	
	public void setEntities(List<StatisticEntity> entities) {
		this.entities = entities;
	}
}
