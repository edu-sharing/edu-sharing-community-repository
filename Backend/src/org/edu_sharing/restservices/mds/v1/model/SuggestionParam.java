package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.MdsQueryCriteria;

public class SuggestionParam {
	ValueParameters valueParameters;
	
	List<MdsQueryCriteria> criteria;

	public ValueParameters getValueParameters() {
		return valueParameters;
	}

	public void setValueParameters(ValueParameters valueParameters) {
		this.valueParameters = valueParameters;
	}

	public List<MdsQueryCriteria> getCriteria() {
		return criteria;
	}
	
	public void setCriteria(List<MdsQueryCriteria> criteria) {
		this.criteria = criteria;
	}
	
	
}
