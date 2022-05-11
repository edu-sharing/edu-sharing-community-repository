package org.edu_sharing.service.statistic;

public class FilterEntry {

	String property;
	
	String[] values;
	
	public void setProperty(String property) {
		this.property = property;
	}
	
	public String getProperty() {
		return property;
	}
	
	public void setValues(String[] values) {
		this.values = values;
	}
	
	public String[] getValues() {
		return values;
	}
}
