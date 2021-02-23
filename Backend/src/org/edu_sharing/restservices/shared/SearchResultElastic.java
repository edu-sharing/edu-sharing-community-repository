package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.edu_sharing.restservices.shared.NodeSearch.Facette;

import java.util.ArrayList;
import java.util.List;

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
