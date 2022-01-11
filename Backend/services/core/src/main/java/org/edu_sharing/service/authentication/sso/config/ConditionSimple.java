package org.edu_sharing.service.authentication.sso.config;

public abstract class ConditionSimple implements Condition {

	String attribute;
	
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	public String getAttribute() {
		return attribute;
	}
}
