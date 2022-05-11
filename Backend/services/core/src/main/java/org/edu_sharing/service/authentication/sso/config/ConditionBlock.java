package org.edu_sharing.service.authentication.sso.config;

import java.util.List;
import java.util.Map;

public class ConditionBlock implements Condition{

	
	public static final String OPERATOR_AND = "AND";
	public static final String OPERATOR_OR = "OR";
	
	List<Condition> conditions;
	String operator;
	
	public ConditionBlock(){
	}
	
	@Override
	public boolean isTrue(Map<String,String> ssoAttributes) {
		
		Boolean result = null;
		
		for(Condition c : conditions){
			if(result != null){
				if(operator.equals(OPERATOR_AND)){
					result = (result && c.isTrue(ssoAttributes));
				}else{
					result = (result || c.isTrue(ssoAttributes));
				}
			}else{
				result = c.isTrue(ssoAttributes);
			}
		}
		
		return result;
	}
	
	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}
	
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	public List<Condition> getConditions() {
		return conditions;
	}
	
}
