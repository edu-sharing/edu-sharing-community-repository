package org.edu_sharing.service.authentication.sso.config;

import java.util.Map;

public class ConditionRegEx extends ConditionSimple {

	String regex;
	
	public ConditionRegEx() {
	}
	
	@Override
	public boolean isTrue(Map<String,String> ssoAttributes) {

		String value = ssoAttributes.get(this.attribute);
		if(value == null){
			return false;
		}
		return value.matches(regex);
	}
	
	public void setRegex(String regex) {
		this.regex = regex;
	}
	
}
