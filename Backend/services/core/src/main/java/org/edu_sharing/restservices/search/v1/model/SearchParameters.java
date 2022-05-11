package org.edu_sharing.restservices.search.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class SearchParameters extends SearchParametersFacets{

	private List<String> permissions;
	private boolean resolveCollections = false;
	private boolean returnSuggestions = false;

	@JsonProperty
	public List<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
	}

	public boolean isResolveCollections() {
		return resolveCollections;
	}

	public void setResolveCollections(boolean resolveCollections) {
		this.resolveCollections = resolveCollections;
	}

	@Schema(required = false, description = "")
	@JsonProperty("facets")
	public List<String> getFacets() { return super.getFacets();}

	public void setReturnSuggestions(boolean returnSuggestions) {
		this.returnSuggestions = returnSuggestions;
	}

	public boolean isReturnSuggestions() {
		return returnSuggestions;
	}
}
