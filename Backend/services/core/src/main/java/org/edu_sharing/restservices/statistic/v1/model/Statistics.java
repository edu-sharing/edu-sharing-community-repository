package org.edu_sharing.restservices.statistic.v1.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Statistics {
	
	List<StatisticEntry> entries = new ArrayList<StatisticEntry>();
	
	public void setEntries(List<StatisticEntry> entries) {
		this.entries = entries;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("entries")
	public List<StatisticEntry> getEntries() {
		return entries;
	}
}
