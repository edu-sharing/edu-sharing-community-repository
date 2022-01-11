/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.client.rpc;

import org.edu_sharing.metadataset.v2.SearchCriterias;

import java.util.HashMap;
import java.util.Map;

public class SearchResult extends Result<HashMap<String, HashMap<String, Object>>>{
	
	Map<String,Map<String,Integer>> countedProps = null;
	
	private SearchCriterias searchCriterias = null;
	
	public SearchResult(){
		
	}
	
	public HashMap<String, HashMap<String, Object>> getData() {
		return data;
	}
	public void setData(HashMap<String, HashMap<String, Object>> data) {
		this.data = data;
	}
	
	/**
	 * HashMap<property, HashMap<value, count>>
	 * @return
	 */
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
	 * 
	 * @param searchCriterias
	 */
	public void setSearchCriterias(SearchCriterias searchCriterias) {
		this.searchCriterias = searchCriterias;
	}
	
}
