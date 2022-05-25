package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.restservices.shared.NodeSearch.Facet;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

public class SearchResult<T> {

	private List<T> nodes = new ArrayList<T>();
	private Pagination pagination = null;
	private List<Facet> facets = null;
	private List<NodeSearch.Suggest> suggests = null;
	private List<String> ignored;

	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty("nodes")
	public List<T> getNodes() {
		return nodes;
	}

	public void setNodes(List<T> nodes) {
		this.nodes = nodes;
	}
	
	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty("pagination")
	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}

	/**
   **/
	@Schema(required = true, description = "")
	@JsonProperty("facets")
	public List<Facet> getFacets() {
		return facets;
	}

	public void setFacets(List<Facet> facets) {
		this.facets = facets;
	}
	
	@JsonProperty("ignored")
	public List<String> getIgnored() {
		return ignored;
	}
	public void setIgnored(List<String> ignored) {
		this.ignored = ignored;
	}

	public void setSuggests(List<NodeSearch.Suggest> suggests) { this.suggests = suggests; }

	@Schema(required = false, description = "")
	public List<NodeSearch.Suggest> getSuggests() { return suggests; }
}
