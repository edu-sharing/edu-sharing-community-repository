package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.restservices.shared.NodeSearch.Facette;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class SearchResult {

	private List<Node> nodes = new ArrayList<Node>();
	private Pagination pagination = null;
	private List<Facette> facettes = null;

	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("nodes")
	public List<Node> getNodes() {
		return nodes;
	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}
	
	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("pagination")
	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("facettes")
	public List<Facette> getFacettes() {
		return facettes;
	}

	public void setFacettes(List<Facette> facettes) {
		this.facettes = facettes;
	}

}
