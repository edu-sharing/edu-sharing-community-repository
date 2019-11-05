package org.edu_sharing.metadataset.v2.tools;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.rpc.SQLKeyword;
import org.edu_sharing.repository.client.rpc.SearchCriterias;
import org.edu_sharing.repository.client.rpc.SuggestFacetDTO;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.springframework.context.ApplicationContext;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.sun.star.lang.IllegalArgumentException;

public class MetadataSearchHelper {
	
	static Logger logger = Logger.getLogger(MetadataSearchHelper.class);
	
	public static String getLuceneSearchQuery(MetadataQueries queries, String queryId, Map<String,String[]> parameters) throws IllegalArgumentException{
		return getLuceneSearchQuery(queries.findQuery(queryId), parameters);
	}
	public static String getLuceneSearchQuery(MetadataQuery query, Map<String,String[]> parameters) throws IllegalArgumentException{

				// We need to add the basequery, it's currently still getting the base query from the old mds -> added at other stage
				//String queryString="("+queries.getBasequery()+")";
				String queryString="";
				if(query.getBasequery()!=null && !query.getBasequery().trim().isEmpty()){
					queryString+="("+query.getBasequery()+")";
				}
				for(String name : parameters.keySet()){
					MetadataQueryParameter parameter = query.findParameterByName(name);
					if(parameter==null)
						throw new IllegalArgumentException("Could not find parameter "+name+" in the query "+query.getId());
					
					String[] values=parameters.get(parameter.getName());
					if((values==null || values.length==0)) {
						if(parameter.getIgnorable()==0)
							continue;
						if(!queryString.isEmpty())
							queryString+=" "+query.getJoin()+" ";
						// handle ignorable parameters
						queryString+="ISNULL:@"+QueryParser.escape(parameter.getName());
							continue;
					}
					if(!queryString.isEmpty())
						queryString+=" "+query.getJoin()+" ";
					queryString+="(";
					if(parameter.isMultiple()){
						int i=0;
						for(String value : values){
							if(i>0)
								queryString+=" "+parameter.getMultiplejoin()+" ";
							queryString+="("+getStatmentForValue(parameter,value)+")";
							i++;
						}
					}
					else if(values.length>1){
						throw new InvalidParameterException("Trying to search for multiple values of a non-multivalue field "+parameter.getName());
					}
					else{
						queryString+=getStatmentForValue(parameter, values[0]);
					}
					queryString+=")";
				}
				return queryString;
			
	}
	/**
	 * If string is enclosed in "", search for the whole string
	 * otherwise, search for every single word (concat with AND)
	 * @param parameter
	 * @param value
	 * @return
	 */
	private static String getStatmentForValue(MetadataQueryParameter parameter, String value) {
		if(value==null && parameter.isMandatory()) {
			throw new java.lang.IllegalArgumentException("null value for mandatory parameter "+parameter.getName()+" given, null values are not allowed if mandatory is set to true");
		}
		if(value==null)
		    return "";

		// invoke any preprocessors for this value
		try {
			value = MetadataQueryPreprocessor.run(parameter, value);
		}catch(Exception e){
			throw new RuntimeException(e);
		}

		if(value.startsWith("\"") && value.endsWith("\"") || parameter.isExactMatching())
			return parameter.getStatement(value).replace("${value}", QueryParser.escape(value));

		String[] words = value.split(" ");
		String query="";
		for(String word : words) {
			if(!query.isEmpty())
				query+=" AND ";
			query+=parameter.getStatement(value).replace("${value}", QueryParser.escape(word));
		}
		return query;
	}
	public static MetadataQueryParameter getParameter(MetadataQueries queries,String queryId,String parameterId){
		for(MetadataQuery query : queries.getQueries()){
			if(query.getId().equals(queryId)){
				for(MetadataQueryParameter parameter : query.getParameters()){
					if(parameter.getName().equals(parameterId)){
						return parameter;
					}
				}
				throw new InvalidParameterException("Parameter "+parameterId+" was not found in query "+queryId);
			}
		}
		throw new InvalidParameterException("Query "+queryId+" was not found");
	}
	private static String getLuceneSuggestionQuery(MetadataQueryParameter parameter,String value){
		//return "("+queries.getBasequery()+") AND ("+parameter.getStatement().replace("${value}","*"+QueryParser.escape(value)+"*")+")";
		return parameter.getStatement(value).replace("${value}","*"+QueryParser.escape(value)+"*");		
	}
	private static List<? extends  SuggestOracle.Suggestion> getSuggestionsSolr(MetadataQueryParameter parameter, MetadataWidget widget, String value, List<MdsQueryCriteria> criterias, MetadataSetV2 mds, String query)  {

		List<SuggestOracle.Suggestion> result = new ArrayList<SuggestOracle.Suggestion>();
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		SearchService searchService = (SearchService)applicationContext.getBean("scopedSearchService");

		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);	
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);

		searchParameters.setSkipCount(0);
		searchParameters.setMaxItems(1);

		String luceneQuery = "(TYPE:\"" + CCConstants.CCM_TYPE_IO + "\"" +") AND ("+getLuceneSuggestionQuery(parameter, value)+")";
		if(criterias != null && criterias.size() > 0 ) {
			
			Map<String,String[]> criteriasMap=new HashMap<>();
			for(MdsQueryCriteria criteria : criterias){
				criteriasMap.put(criteria.getProperty(),criteria.getValues().toArray(new String[0]));
			}
			
			MetadataQueries queries = mds.getQueries();
			MetadataQuery queryObj = queries.findQuery(query);
			queryObj.setApplyBasequery(false);
			queryObj.setBasequery(null);
			
			SearchCriterias scParam = new SearchCriterias();
			scParam.setRepositoryId(mds.getRepositoryId());
			scParam.setMetadataSetId(mds.getId());
			scParam.setMetadataSetQuery(query);
			try {
				luceneQuery = "(" + luceneQuery + ") AND " +  MetadataSearchHelper.getLuceneString(queries,queryObj,scParam, criteriasMap);
				//System.out.println("MetadataSearchHelper lucenequery suggest:" +luceneQuery);
			} catch (IllegalArgumentException e) {
				logger.error(e.getMessage(), e);
			} 
			
		}
		searchParameters.setQuery(luceneQuery);

		String facetName = "@" + parameter.getName();
		List<String> facets = parameter.getFacets() == null ? Arrays.asList(new String[]{facetName}) : parameter.getFacets();
		for(String facet : facets){
			FieldFacet fieldFacet = new FieldFacet(facet);
			fieldFacet.setLimit(100);
			fieldFacet.setMinCount(1);
			searchParameters.addFieldFacet(fieldFacet);
		}

		ResultSet rs = searchService.query(searchParameters);
		Map<String, MetadataKey> captions = widget.getValuesAsMap();

		for(String facet : facets) {
			List<Pair<String, Integer>> facettPairs = rs.getFieldFacet(facet);

			for (Pair<String, Integer> pair : facettPairs) {

				//solr 4 bug: leave out zero values
				if (pair.getSecond() == 0) {
					continue;
				}

				String hit = pair.getFirst(); // new String(pair.getFirst().getBytes(), "UTF-8");

				if (hit.toLowerCase().contains(value.toLowerCase())) {

					SuggestFacetDTO dto = new SuggestFacetDTO();
					dto.setFacet(hit);
					dto.setDisplayString(captions.containsKey(hit) ? captions.get(hit).getCaption() : null);

					result.add(dto);
				}
			}
		}
		return result;
		
	}

	public static List<? extends  SuggestOracle.Suggestion> getSuggestions(String repoId,MetadataSetV2 mds,String queryId,String parameterId,String value, List<MdsQueryCriteria> criterias) throws IllegalArgumentException  {
		MetadataWidget widget=mds.findWidget(parameterId);
		
		String source=widget.getSuggestionSource();
		if(source==null){
			source=widget.getValues()!=null ? MetadataReaderV2.SUGGESTION_SOURCE_MDS : MetadataReaderV2.SUGGESTION_SOURCE_SOLR;
		}
		
		/**
		 * remote repo
		 */
		if(!ApplicationInfoList.getHomeRepository().getAppId().equals(repoId)) {
			return SearchServiceFactory.getSearchService(repoId).getSuggestions(mds, queryId, parameterId, value, criterias);
		}
		
		/**
		 * local repo
		 */
		if(source.equals(MetadataReaderV2.SUGGESTION_SOURCE_SOLR)){
			MetadataQueryParameter parameter = getParameter(mds.getQueries(),queryId,parameterId);
			return getSuggestionsSolr(parameter, widget, value, criterias, mds, queryId);
		}
		if(source.equals(MetadataReaderV2.SUGGESTION_SOURCE_MDS)){
			return getSuggestionsMds(widget, value);
		}
		if(source.equals(MetadataReaderV2.SUGGESTION_SOURCE_SQL)){
			return getSuggestionsSql(widget, value);
		}
		throw new IllegalArgumentException("Unknow suggestionSource "+source+" for widget "+parameterId+
				", use "+MetadataReaderV2.SUGGESTION_SOURCE_MDS+", "+
				MetadataReaderV2.SUGGESTION_SOURCE_SOLR+" or "+
				MetadataReaderV2.SUGGESTION_SOURCE_SQL
		);
	}
	
	private static List<? extends Suggestion> getSuggestionsSql(MetadataWidget widget,
			String value) throws IllegalArgumentException {
		String query=widget.getSuggestionQuery();
		List<SQLKeyword> result = new ArrayList<SQLKeyword>();
		Connection con = null;
		PreparedStatement statement = null;
		if(query == null || query.trim().equals("")){
			throw new IllegalArgumentException("suggestionSource "+MetadataReaderV2.SUGGESTION_SOURCE_SQL+" at widget "+widget.getId()+" needs an suggestionQuery, but none was found");
		}
		
		ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
		try{			
			con = dbAlf.getConnection();
			statement = con.prepareStatement(query);
			
			value = StringEscapeUtils.escapeSql(value);
			statement.setString(1,"%" + value.toLowerCase() + "%");
			
			java.sql.ResultSet resultSet = statement.executeQuery();
		
			while(resultSet.next()){
				String kwValue = resultSet.getString(1);
				SQLKeyword sqlKw = new SQLKeyword();
				sqlKw.setKeyword(kwValue.trim());
				result.add(sqlKw);
			}	
		}catch(Throwable e){
		}finally {
			dbAlf.cleanUp(con, statement);
		}
		return result;
	}
	private static List<? extends Suggestion> getSuggestionsMds(MetadataWidget widget,
			String value) throws IllegalArgumentException {
		if(widget.getValues()==null)
			throw new IllegalArgumentException("Requested suggestion type "+MetadataReaderV2.SUGGESTION_SOURCE_MDS+" for widget "+widget.getId()+", but widget has no values attached");
		List<SuggestOracle.Suggestion> result = new ArrayList<SuggestOracle.Suggestion>();
		value=value.toLowerCase();
		for(MetadataKey key : widget.getValues()){
			if(key.getKey().toLowerCase().contains(value)
					|| key.getCaption().toLowerCase().contains(value)
					){
				SuggestFacetDTO dto = new SuggestFacetDTO();
				dto.setFacet(key.getKey());
				dto.setDisplayString(key.getCaption());
				result.add(dto);
			}
		}
		return result;
	}
	private static String getSearchString(String field,String[] types) {
		String result = "(";
		
		Iterator<String> iter = Arrays.asList(types).iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			result = result +field+":\"" + key + "\"";
			if (iter.hasNext())
				result = result + " OR ";

		}
		result = result + ")";
		return result;
	}
	public static String getLuceneString(MetadataQueries queries,MetadataQuery query, SearchCriterias searchCriterias,Map<String,String[]> parameters) throws IllegalArgumentException {
		String lucene=getLuceneSearchQuery(query, parameters);
		if(query.isApplyBasequery()){
			String andQuery="";
			if(lucene!=null && !lucene.trim().isEmpty())
				andQuery=" AND (" + lucene + ")";
			if(queries.getBasequery()!=null && !queries.getBasequery().isEmpty())
				lucene=queries.getBasequery()+andQuery;
			lucene = applyCondition(queries, lucene);
		}
		lucene = applyCondition(query, lucene);
		lucene = convertSearchCriteriasToLucene(lucene,searchCriterias);
		return lucene;
	}

	private static String applyCondition(MetadataQueryBase query, String lucene) {
		for(MetadataQueryCondition condition : query.getConditions()){
			boolean conditionState= MetadataHelper.checkConditionTrue(condition.getCondition());
			if(conditionState && condition.getQueryTrue()!=null)
				lucene += " AND ("+condition.getQueryTrue()+")";
			if(!conditionState && condition.getQueryFalse()!=null)
				lucene += " AND ("+condition.getQueryFalse()+")";
		}
		return lucene;
	}

	public static String convertSearchCriteriasToLucene(String lucene,SearchCriterias searchCriterias) {
		String searchTypesString = null;
		String searchAspectsString = null;
		if(searchCriterias!=null){
			if(searchCriterias.getContentkind() == null || searchCriterias.getContentkind().length < 1){
				//default search for IO's -> no, if no content kind is request, it equals "ALL"
				//searchTypesString = "TYPE:\"" + CCConstants.CCM_TYPE_IO + "\"";
			}else{
				searchTypesString = getSearchString("TYPE",searchCriterias.getContentkind());
			}
			if(searchCriterias.getAspects()!=null){
				searchAspectsString = getSearchString("ASPECT",searchCriterias.getAspects());

			}

			if (lucene != null && !lucene.trim().equals("") && searchTypesString!=null) {
				lucene += " AND " + searchTypesString;

			}else if(lucene == null || lucene.trim().equals("")){
				lucene = searchTypesString;
			}
			if(searchAspectsString!=null)
				lucene+=" AND "+searchAspectsString;
		}
		return lucene;
	}
}