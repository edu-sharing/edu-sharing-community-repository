package org.edu_sharing.repository.server;

import java.util.List;
import java.util.Map;

import org.edu_sharing.metadataset.v2.SearchCriterias;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.service.model.NodeRef;

public class SearchResultNodeRef extends Result<List<NodeRef>> {
	
	Map<String,Map<String,Integer>> countedProps = null;
	
	private SearchCriterias searchCriterias = null;
	
	public Map<String, Map<String, Integer>> getCountedProps() {
		return countedProps;
	}
	
	public void setCountedProps(Map<String, Map<String, Integer>> countedProps) {
		this.countedProps = countedProps;
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
