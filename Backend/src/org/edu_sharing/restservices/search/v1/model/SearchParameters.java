package org.edu_sharing.restservices.search.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
;

import java.util.List;

import org.edu_sharing.restservices.shared.MdsQueryCriteria;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class SearchParameters {

	private List<String> permissions;
	private List<MdsQueryCriteria> criteria;
	private List<String> facets;
	private boolean resolveCollections = false;

	@Schema(required = true, description = "")
	@JsonProperty("criterias")
	public List<MdsQueryCriteria> getCriteria() {
		return criteria;
	}

	public void setCriteria(List<MdsQueryCriteria> criteria) {
		this.criteria = criteria;
	}

	@Schema(required = true, description = "")
	@JsonProperty("facettes")
	public List<String> getFacets() {
		return facets;
	}

	public void setFacets(List<String> facets) {
		this.facets = facets;
	}

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
}
