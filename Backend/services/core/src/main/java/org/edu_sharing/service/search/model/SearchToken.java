package org.edu_sharing.service.search.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.SearchCriterias;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;

import com.sun.star.lang.IllegalArgumentException;

import org.edu_sharing.service.search.SearchService.ContentType;

public class SearchToken implements Serializable {
	static Logger logger = Logger.getLogger(SearchToken.class);

	SortDefinition sortDefinition;
	
	String luceneString;
	
	List<String> facettes=null;

	String storeProtocol = "workspace";
	
	String storeName = "SpacesStore";
	
	int from;
	
	int maxResult;
	
	
	/**
	 * search in scope of authorities
	 */
	List<String> authorityScope;

	private ContentType contentType;
	private MetadataQueries queries;
	private List<String> permissions;

	public ContentType getContentType(){
		if(contentType==null)
			return ContentType.FILES;
		return contentType;
	}
	public void setContentType(ContentType contentType){
		this.contentType=contentType;
		updateSearchCriterias(true);
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

	private MetadataQuery query;

	private Map<String, String[]> parameters;
	
	public SortDefinition getSortDefinition() {
		return sortDefinition;
	}

	public void setSortDefinition(SortDefinition sortDefinition) {
		this.sortDefinition = sortDefinition;
	}

	public String getLuceneString() throws IllegalArgumentException {
		if(query!=null){
			return MetadataSearchHelper.getLuceneString(queries,query,searchCriterias,parameters);
		}
		if(searchCriterias!=null){
			logger.debug("Using lucene string only search");
			/*QueryBuilder queryBuilder = new QueryBuilder();
			queryBuilder.setSearchCriterias(searchCriterias);
			if(luceneString==null || luceneString.trim().isEmpty())
				return queryBuilder.getSearchString();
			return "("+queryBuilder.getSearchString()+") AND ("+luceneString+")";
			*/
			return MetadataSearchHelper.convertSearchCriteriasToLucene(luceneString,searchCriterias);
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
	public void setMetadataQuery(MetadataQueries queries, String queryId, Map<String, String[]> parameters) {
		this.queries = queries;
		this.query = queries.findQuery(queryId);
		this.parameters = parameters;
	}

	public Map<String, String[]> getParameters() {
		return parameters;
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
		updateSearchCriterias(false);
	}

	public SearchCriterias getSearchCriterias() {
		return searchCriterias;
	}

	private void updateSearchCriterias(boolean rebuild) {
		if(searchCriterias==null || rebuild)
			searchCriterias=new SearchCriterias();
		if(getContentType()==null || getContentType().equals(ContentType.ALL)){
			//searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_MAP});
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
	public String getQueryString() throws IllegalArgumentException {
		if(queryString!=null)
			return queryString;
		return getLuceneString();
	}
	
	/**
	 * set the scope of authorities
	 * security problem (only when admin permissions)
	 * @param authorities
	 */
	public void setAuthorityScope(List<String> authorities) {
		this.authorityScope = authorities;
	}
	
	public List<String> getAuthorityScope() {
		return authorityScope;
	}

	/**
	 * Filter for permissions by the current user
	 * Only materials with ALL permissions provided here will be shown for the current user
	 * Note: Only supported via ElasticSearch!
	 */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
