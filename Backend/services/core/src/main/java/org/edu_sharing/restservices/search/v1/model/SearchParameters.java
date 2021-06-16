package org.edu_sharing.restservices.search.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import org.edu_sharing.restservices.shared.MdsQueryCriteria;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class SearchParameters {

	private List<String> permissions;
	private List<MdsQueryCriteria> criterias;
	private List<String> facettes;

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("criterias")
	public List<MdsQueryCriteria> getCriterias() {
		return criterias;
	}

	public void setCriterias(List<MdsQueryCriteria> criterias) {
		this.criterias = criterias;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("facettes")
	public List<String> getFacettes() {
		return facettes;
	}

	public void setFacettes(List<String> facettes) {
		this.facettes = facettes;
	}

	@JsonProperty
	public List<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
	}
}
