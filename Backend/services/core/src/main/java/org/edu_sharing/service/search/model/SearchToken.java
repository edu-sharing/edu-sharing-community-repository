package org.edu_sharing.service.search.model;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.edu_sharing.metadataset.v2.SearchCriterias;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;

import org.edu_sharing.service.search.SearchService.ContentType;

public class SearchToken implements Serializable {
	static Logger logger = Logger.getLogger(SearchToken.class);

	SortDefinition sortDefinition;
	
	String luceneString;
	
	List<String> facets =null;

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
	private boolean resolveCollections = false;
	private boolean returnSuggestion = false;

	List<String> excludes = new ArrayList<>();

	public ContentType getContentType(){
		if(contentType==null)
			return ContentType.FILES;
		return contentType;
	}
	public void setContentType(ContentType contentType){
		this.contentType=contentType;
		updateSearchCriterias(true);
	}
	public int getFacetsMinCount() {
		return facetsMinCount;
	}

	public void setFacetsMinCount(int facetsMinCount) {
		this.facetsMinCount = facetsMinCount;
	}


	
	public void setFacetLimit(int facetLimit) {
		this.facetLimit = facetLimit;
	}

	private int facetLimit =50;
	private int facetsMinCount =4;

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

	public MetadataQuery getQuery() {
		return query;
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
	public List<String> getFacets() {
		if(this.query != null && facets != null) {
			List<String> combined = new ArrayList<>();
			this.facets.forEach((facet) -> {
				List<String> sublist = this.query.findParameterByName(facet).getFacets();
				if(sublist != null) {
					combined.addAll(sublist);
				} else {
					combined.add(facet);
				}
			});
			return combined;
		}
		return facets;
	}

	/**
	 *
	 * re aggregates splitted facettes from the current mds query to a single list for the client
	 */
	public Map<String, Map<String, Integer>> aggregateFacetes(Map<String, Map<String, Integer>> propsMap) {
		Map<String, Map<String, Integer>> combined = new HashMap<>();
		if(this.query != null && facets != null) {
			List<MetadataQueryParameter> facetParams = this.query.getParameters().stream().filter((p) -> p.getFacets() != null).collect(Collectors.toList());
			for(Map.Entry<String, Map<String, Integer>> entry : propsMap.entrySet()) {
				Optional<MetadataQueryParameter> param = facetParams.stream().filter((p) -> p.getFacets().contains(entry.getKey())).findFirst();
				String key = entry.getKey();
				if(param.isPresent()) {
					key = param.get().getName();
				}
				if(combined.containsKey(key)) {
					Map<String, Integer> current = combined.get(key);
					current.putAll(entry.getValue());
					combined.put(key, current);
				} else {
					combined.put(key, entry.getValue());
				}
			}
			return combined;
		}
		return propsMap;
	}

	public void setFacets(List<String> facets) {
		this.facets = facets;
	}

	public int getFacetLimit() {
		return facetLimit;
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
		if(getContentType().equals(ContentType.COLLECTION_PROPOSALS)){
			searchCriterias.setContentkind(new String[]{CCConstants.CCM_TYPE_COLLECTION_PROPOSAL});
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
		return queryString;
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

	public boolean isResolveCollections() {
		return resolveCollections;
	}

	public void setResolveCollections(boolean resolveCollections) {
		this.resolveCollections = resolveCollections;
	}

	public boolean isReturnSuggestion() { return returnSuggestion; }

	public void setReturnSuggestion(boolean returnSuggestion) {	this.returnSuggestion = returnSuggestion; }

	public List<String> getExcludes() {
		return excludes;
	}

	public void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}
}
