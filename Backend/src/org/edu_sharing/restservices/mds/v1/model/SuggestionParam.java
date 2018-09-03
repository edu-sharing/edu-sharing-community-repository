package org.edu_sharing.restservices.mds.v1.model;

import org.edu_sharing.restservices.search.v1.model.SearchParameters;

public class SuggestionParam {
	ValueParameters valueParameters;
	
	SearchParameters searchParameters;

	public ValueParameters getValueParameters() {
		return valueParameters;
	}

	public void setValueParameters(ValueParameters valueParameters) {
		this.valueParameters = valueParameters;
	}

	public SearchParameters getSearchParameters() {
		return searchParameters;
	}

	public void setSearchParameters(SearchParameters searchParameters) {
		this.searchParameters = searchParameters;
	}
	
	
}
