package org.edu_sharing.service.search;

import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.json.JSONObject;

public class SearchResultNodeRefElastic extends SearchResultNodeRef {

	@SuppressWarnings("NonSerializableFieldInSerializableClass")
	private JSONObject elasticResponse;

	public JSONObject getElasticResponse() {
		return elasticResponse;
	}

	public void setElasticResponse(JSONObject elasticResponse) {
		this.elasticResponse = elasticResponse;
	}
}
