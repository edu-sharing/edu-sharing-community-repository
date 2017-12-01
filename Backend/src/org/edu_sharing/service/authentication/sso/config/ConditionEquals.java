package org.edu_sharing.service.authentication.sso.config;

import java.util.Map;

public class ConditionEquals extends ConditionSimple{
	
	String matcher;
	
	public ConditionEquals() {
	}
	
	@Override
	public boolean isTrue(Map<String,String> ssoAttributes) {
		String value = ssoAttributes.get(this.attribute);
		return matcher.equals(value);
	}
	
	public void setMatcher(String matcher) {
		this.matcher = matcher;
	}
	
}
