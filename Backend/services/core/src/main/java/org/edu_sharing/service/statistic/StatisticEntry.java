package org.edu_sharing.service.statistic;

import java.util.Map;

public class StatisticEntry {

	String property;
	
	Map<String,Long> statistic;
	
	public String getProperty() {
		return property;
	}
	
	public void setProperty(String property) {
		this.property = property;
	}
	
	
	public Map<String, Long> getStatistic() {
		return statistic;
	}
	
	public void setStatistic(Map<String, Long> statistic) {
		this.statistic = statistic;
	}

	
}
