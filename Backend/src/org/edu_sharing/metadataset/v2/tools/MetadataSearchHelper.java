package org.edu_sharing.metadataset.v2.tools;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
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
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.Suggestion;
import org.springframework.context.ApplicationContext;

import com.sun.star.lang.IllegalArgumentException;

public class MetadataSearchHelper {
	
	static Logger logger = Logger.getLogger(MetadataSearchHelper.class);

	public static Map<String, String[]> convertCriterias(List<MdsQueryCriteria> criterias){
		Map<String,String[]> criteriasMap=new HashMap<>();
		for(MdsQueryCriteria criteria : criterias){
			criteriasMap.put(criteria.getProperty(),criteria.getValues().toArray(new String[0]));
		}
		return criteriasMap;
	}
	static String getLuceneSearchQuery(MetadataQueries queries, String queryId, Map<String,String[]> parameters) throws IllegalArgumentException{
		return getLuceneSearchQuery(queries.findQuery(queryId), parameters);
	}
	static String getLuceneSearchQuery(MetadataQuery query, Map<String,String[]> parameters) throws IllegalArgumentException{

				// We need to add the basequery, it's currently still getting the base query from the old mds -> added at other stage
				//String queryString="("+queries.getBasequery()+")";
				String queryString="";
				String basequery = query.findBasequery(parameters != null ? parameters.keySet() : null);
				if(basequery!=null && !basequery.trim().isEmpty()){
					queryString+="("+replaceCommonQueryVariables(basequery)+")";
				}
				if(parameters == null){
					return queryString;
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
							queryString+="("+replaceCommonQueryVariables(getStatmentForValue(parameter,value))+")";
							i++;
						}
					}
					else if(values.length>1){
						throw new InvalidParameterException("Trying to search for multiple values of a non-multivalue field "+parameter.getName());
					}
					else{
						queryString+=replaceCommonQueryVariables(getStatmentForValue(parameter, values[0]));
					}
					queryString+=")";
				}
				return queryString;
			
	}

	/**
	 * replaces globally supported variables for queries (like ${user.<property>} )
	 */
	public static String replaceCommonQueryVariables(String statement) {
		NodeRef ref = AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(AuthenticationUtil.getFullyAuthenticatedUser());
		try {
			Map<String, Object> props = NodeServiceHelper.transformLongToShortProperties(NodeServiceHelper.getProperties(ref));
			for(Map.Entry<String, Object> prop : props.entrySet()){
				statement = statement.replace("${user."+prop.getKey() + "}", prop.getValue().toString());
			}
		} catch (Throwable t) {
			logger.warn("replaceCommonQueryVariables failed: " + t.getMessage());
		}
		return statement;
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
	public static List<? extends Suggestion> getSuggestions(String repoId, MetadataSetV2 mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criterias) throws IllegalArgumentException  {
		MetadataWidget widget=mds.findWidget(parameterId);
		
		String source=widget.getSuggestionSource();
		if(source==null){
			source=widget.getValues()!=null ? MetadataReaderV2.SUGGESTION_SOURCE_MDS : MetadataReaderV2.SUGGESTION_SOURCE_SEARCH;
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
		if(source.equals(MetadataReaderV2.SUGGESTION_SOURCE_SEARCH)){
			return SearchServiceFactory.getSearchService(repoId).getSuggestions(mds, queryId, parameterId, value, criterias);
		}
		if(source.equals(MetadataReaderV2.SUGGESTION_SOURCE_MDS)){
			return getSuggestionsMds(widget, value);
		}
		if(source.equals(MetadataReaderV2.SUGGESTION_SOURCE_SQL)){
			return getSuggestionsSql(widget, value);
		}
		throw new IllegalArgumentException("Unknow suggestionSource "+source+" for widget "+parameterId+
				", use "+MetadataReaderV2.SUGGESTION_SOURCE_MDS+", "+
				MetadataReaderV2.SUGGESTION_SOURCE_SEARCH+" or "+
				MetadataReaderV2.SUGGESTION_SOURCE_SQL
		);
	}
	
	private static List<? extends Suggestion> getSuggestionsSql(MetadataWidget widget,
			String value) throws IllegalArgumentException {
		String query=widget.getSuggestionQuery();
		List<Suggestion> result = new ArrayList<>();
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
				Suggestion sqlKw = new Suggestion();
				sqlKw.setKey(kwValue.trim());

				try {
					String displayString = resultSet.getString(2);
					sqlKw.setDisplayString(displayString);
				}catch (SQLException e){
					//no display string in result
				}

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
		List<Suggestion> result = new ArrayList<>();
		value=value.toLowerCase();
		for(MetadataKey key : widget.getValues()){
			if(key.getKey().toLowerCase().contains(value)
					|| key.getCaption().toLowerCase().contains(value)
					){
				Suggestion dto = new Suggestion();
				dto.setKey(key.getKey());
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
	public static String getLuceneString(String queryId,Map<String,String[]> parameters) throws Exception {
		MetadataQueries queries = MetadataHelper.getLocalDefaultMetadataset().getQueries(MetadataReaderV2.QUERY_SYNTAX_LUCENE);
		return getLuceneString(queries, queries.findQuery(queryId), null, parameters);
	}
	public static String getLuceneString(MetadataQueries queries,MetadataQuery query, SearchCriterias searchCriterias,Map<String,String[]> parameters) throws IllegalArgumentException {
		if(parameters == null){
			parameters = new HashMap<>();
		}
		String lucene=getLuceneSearchQuery(query, parameters);
		if(query.isApplyBasequery()){
			String andQuery="";
			if(lucene!=null && !lucene.trim().isEmpty())
				andQuery=" AND (" + lucene + ")";
			if(queries.findBasequery(parameters.keySet())!=null &&
					!queries.findBasequery(parameters.keySet()).isEmpty()) {
				lucene = queries.findBasequery(parameters.keySet()) + andQuery;
			}
			lucene = applyCondition(queries, lucene);
		}
		lucene = applyCondition(query, lucene);
		lucene = convertSearchCriteriasToLucene(lucene,searchCriterias);
		return lucene;
	}

	private static String applyCondition(MetadataQueryBase query, String lucene) {
		for(MetadataQueryCondition condition : query.getConditions()){
			boolean conditionState= MetadataHelper.checkConditionTrue(condition.getCondition());
			if(conditionState && condition.getQueryTrue()!=null) {
				String conditionString = condition.getQueryTrue();
				conditionString = replaceCommonQueryVariables(conditionString);
				lucene += " AND (" + conditionString + ")";
			}
			if(!conditionState && condition.getQueryFalse()!=null) {
				String conditionString =condition.getQueryFalse();
				conditionString = replaceCommonQueryVariables(conditionString);
				lucene += " AND (" + conditionString + ")";
			}
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