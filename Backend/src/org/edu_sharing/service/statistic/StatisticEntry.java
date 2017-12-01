package org.edu_sharing.service.statistic;

import java.util.Map;

public class StatisticEntry {

	String property;
	
	Map<String,Integer> statistic;
	
	public String getProperty() {
		return property;
	}
	
	public void setProperty(String property) {
		this.property = property;
	}
	
	
	public Map<String, Integer> getStatistic() {
		return statistic;
	}
	
	public void setStatistic(Map<String, Integer> statistic) {
		this.statistic = statistic;
	}

	
}
