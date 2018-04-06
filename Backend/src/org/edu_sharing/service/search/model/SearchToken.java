package org.edu_sharing.service.search.model;

import java.util.List;

import org.edu_sharing.repository.client.rpc.SearchCriterias;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.search.QueryBuilder;
import org.edu_sharing.repository.server.tools.search.QueryValidationFailedException;
import org.edu_sharing.service.search.SearchService.ContentType;

public class SearchToken {
	
	SortDefinition sortDefinition;
	
	String luceneString;
	
	
	List<String> facettes=null;

	

	String storeProtocol = "workspace";
	
	String storeName = "SpacesStore";
	
	int from;
	
	int maxResult;

	private ContentType contentType;

	public ContentType getContentType(){
		if(contentType==null)
			return ContentType.FILES;
		return contentType;
	}
	public void setContentType(ContentType contentType){
		this.contentType=contentType;
		updateSearchCriterias();
	}
	public int getFacettesMinCount() {
		return facettesMinCount;
	}

	public void setFacettesMinCount(int facettesMinCount) {
		this.facettesMinCount = facettesMinCount;
	}


	
	public void setFacettesLimit(int facettesLimit) {
		this.facettesLimit = facettesLimit;
	}

	private int facettesLimit=50;
	private int facettesMinCount=4;

	private SearchCriterias searchCriterias;

	private String queryString;
	
	public SortDefinition getSortDefinition() {
		return sortDefinition;
	}

	public void setSortDefinition(SortDefinition sortDefinition) {
		this.sortDefinition = sortDefinition;
	}

	public String getLuceneString() throws QueryValidationFailedException {
		if(searchCriterias!=null){
			QueryBuilder queryBuilder = new QueryBuilder();
			queryBuilder.setSearchCriterias(searchCriterias);
			if(luceneString==null || luceneString.trim().isEmpty())
				return queryBuilder.getSearchString();
			return "("+queryBuilder.getSearchString()+") AND ("+luceneString+")";
		}
		return luceneString;
	}
	/**
	 * Nulls any existing search criterias
	 */
	public void disableSearchCriterias() {
		this.searchCriterias=null;
	}
	public void setLuceneString(String luceneString) {
		this.luceneString = luceneString;
	}

	public String getStoreProtocol() {
		return storeProtocol;
	}

	public void setStoreProtocol(String storeProtocol) {
		this.storeProtocol = storeProtocol;
	}

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public int getMaxResult() {
		return maxResult;
	}

	public void setMaxResult(int maxResult) {
		this.maxResult = maxResult;
	}
	public List<String> getFacettes() {
		return facettes;
	}

	public void setFacettes(List<String> facettes) {
		this.facettes = facettes;
	}

	public int getFacettesLimit() {
		return facettesLimit;
	}

	public void setSearchCriterias(SearchCriterias searchCriterias) {
		this.searchCriterias=searchCriterias;
		updateSearchCriterias();
	}
	private void updateSearchCriterias() {
		if(searchCriterias==null)
			searchCriterias=new SearchCriterias();
		if(getContentType()==null || getContentType().equals(ContentType.ALL)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_MAP});
			return;
		}
		if(getContentType().equals(ContentType.FILES)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_IO});
		}
		if(getContentType().equals(ContentType.FOLDERS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_MAP});
		}
		if(getContentType().equals(ContentType.FILES_AND_FOLDERS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_MAP});
		}
		if(getContentType().equals(ContentType.COLLECTIONS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_MAP});
			searchCriterias.setAspects(new String[]{CCConstants.CCM_ASPECT_COLLECTION});
		}
		if(getContentType().equals(ContentType.TOOLPERMISSIONS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_TOOLPERMISSION});
		}
	}
	/**
	 * Get and set query string (for ui purposes/info)
	 * @param queryString
	 */
	public void setQueryString(String queryString) {
		this.queryString=queryString;
	}
	public String getQueryString() throws QueryValidationFailedException {
		if(queryString!=null)
			return queryString;
		return getLuceneString();
	}
	
	
}
