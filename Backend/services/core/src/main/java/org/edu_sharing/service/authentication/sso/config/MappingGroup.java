package org.edu_sharing.service.authentication.sso.config;

public class MappingGroup {
	
	Condition condition;
	
	String mapTo;
	
	String mapToDisplayName;
	
	String parentGroup;
	
	public MappingGroup() {
	}
	
	public void setCondition(Condition condition) {
		this.condition = condition;
	}
	
	public void setMapTo(String mapTo) {
		this.mapTo = mapTo;
	}
	
	public String getMapTo() {
		return mapTo;
	}
	
	public Condition getCondition() {
		return condition;
	}
	
	public void setParentGroup(String parentGroup) {
		this.parentGroup = parentGroup;
	}
	
	public String getParentGroup() {
		return parentGroup;
	}
	
	public void setMapToDisplayName(String mapToDisplayName) {
		this.mapToDisplayName = mapToDisplayName;
	}
	
	public String getMapToDisplayName() {
		return mapToDisplayName;
	}

}
