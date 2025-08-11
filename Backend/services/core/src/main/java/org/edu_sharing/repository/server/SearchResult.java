package org.edu_sharing.repository.server;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.metadataset.v2.SearchCriterias;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.model.NodeRef;

import java.util.List;
import java.util.Map;

public class SearchResult<T> extends Result<List<T>> {

	List<NodeSearch.Facet> facets = null;
	List<NodeSearch.Suggest> suggests = null;

	
	private SearchCriterias searchCriterias = null;

	@JsonIgnore
	@Getter
	@Setter
	private List<Hit<Map>> elasticHits;

	public void setFacets(List<NodeSearch.Facet> facets){
		this.facets = facets;
	}

	public List<NodeSearch.Facet> getFacets() {
		return facets;
	}

	public void setSuggests(List<NodeSearch.Suggest> suggests) {
		this.suggests = suggests;
	}

	public List<NodeSearch.Suggest> getSuggests() {
		return suggests;
	}

	public SearchCriterias getSearchCriterias() {
		return searchCriterias;
	}
	
	/**
	 * @param searchCriterias
	 */
	public void setSearchCriterias(SearchCriterias searchCriterias) {
		this.searchCriterias = searchCriterias;
	}
	
	@Override
	public List<T> getData() {
		return super.getData();
	}
	
	@Override
	public void setData(List<T> data) {
		super.setData(data);
	}
}
