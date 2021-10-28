package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
;

public class SearchResultElastic<T> extends SearchResult<T> {
	private String elasticResponse;

	@JsonProperty
	public void setElasticResponse(String elasticResponse) {
		this.elasticResponse = elasticResponse;
	}

	public String getElasticResponse() {
		return elasticResponse;
	}
}
