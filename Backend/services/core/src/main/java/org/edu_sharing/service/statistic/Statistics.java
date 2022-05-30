package org.edu_sharing.service.statistic;

import java.util.ArrayList;
import java.util.List;


public class Statistics {
	
	List<StatisticEntry> entries = new ArrayList<StatisticEntry>();
	
	public void setEntries(List<StatisticEntry> entries) {
		this.entries = entries;
	}
	
	public List<StatisticEntry> getEntries() {
		return entries;
	}
	
}
