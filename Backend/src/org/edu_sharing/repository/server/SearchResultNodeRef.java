package org.edu_sharing.repository.server;

import java.util.List;

import org.edu_sharing.metadataset.v2.SearchCriterias;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.model.NodeRef;

public class SearchResultNodeRef extends Result<List<NodeRef>> {

	List<NodeSearch.Facet> facets = null;
	List<NodeSearch.Suggest> suggests = null;

	
	private SearchCriterias searchCriterias = null;
	
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
	public List<NodeRef> getData() {
		return super.getData();
	}
	
	@Override
	public void setData(List<NodeRef> data) {
		super.setData(data);
	}
}
