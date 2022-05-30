package org.edu_sharing.service.search;

import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.elasticsearch.action.search.SearchResponse;

public class SearchResultNodeRefElastic extends SearchResultNodeRef {

	private SearchResponse elasticResponse;

	public SearchResponse getElasticResponse() {
		return elasticResponse;
	}

	public void setElasticResponse(SearchResponse elasticResponse) {
		this.elasticResponse = elasticResponse;
	}
}
