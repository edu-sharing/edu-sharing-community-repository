package org.edu_sharing.metadataset.v2;

import java.security.InvalidParameterException;
import java.security.Policy.Parameters;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.SQLKeyword;
import org.edu_sharing.repository.client.rpc.SuggestFacetDTO;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.suggest.ConnectionDBAlfresco;
import org.edu_sharing.service.suggest.ConnectionPool;
import org.springframework.context.ApplicationContext;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.sun.star.lang.IllegalArgumentException;

public class MetadataSearchHelper {
	
	
	static Logger logger = Logger.getLogger(MetadataSearchHelper.class);
	public static String getLuceneSearchQuery(MetadataQueries queries,String queryId,Map<String,String[]> parameters) throws IllegalArgumentException{
		for(MetadataQuery query : queries.getQueries()){
			if(query.getId().equals(queryId)){
				// We need to add the basequery, it's currently still getting the base query from the old mds -> added at other stage
				//String queryString="("+queries.getBasequery()+")";
				String queryString="";
				for(String name : parameters.keySet()){
					MetadataQueryParameter parameter = query.findParameterByName(name);
					if(parameter==null)
						throw new IllegalArgumentException("Could not find parameter "+name+" in the query "+queryId);
					
					String[] values=parameters.get(parameter.getName());
					if(values==null || values.length==0)
						continue;
					if(!queryString.isEmpty())
						queryString+=" "+query.getJoin()+" ";
					queryString+="(";
					if(parameter.isMultiple()){
						int i=0;
						for(String value : values){
							if(i>0)
								queryString+=" "+parameter.getMultiplejoin()+" ";
							queryString+="("+parameter.getStatement().replace("${value}", QueryParser.escape(value))+")";
							i++;
						}
					}
					else if(values.length>1){
						throw new InvalidParameterException("Trying to search for multiple values of a non-multivalue field "+parameter.getName());
					}
					else{
						queryString+=parameter.getStatement().replace("${value}", QueryParser.escape(values[0]));
					}
					queryString+=")";
				}
				return queryString;
			}
		}
		throw new InvalidParameterException("Query id "+queryId+" not found");
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
		return parameter.getStatement().replace("${value}","*"+QueryParser.escape(value)+"*");		
	}
	private static List<? extends  SuggestOracle.Suggestion> getSuggestionsSolr(MetadataQueryParameter parameter,MetadataWidget widget,String value)  {

		List<SuggestOracle.Suggestion> result = new ArrayList<SuggestOracle.Suggestion>();
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		SearchService searchService = (SearchService)applicationContext.getBean("scopedSearchService");

		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);	
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);

		searchParameters.setSkipCount(0);
		searchParameters.setMaxItems(1);

		searchParameters.setQuery("(TYPE:\"" + CCConstants.CCM_TYPE_IO + "\"" +") AND "+getLuceneSuggestionQuery(parameter, value));
		
		String facetName = "@" + parameter.getName();		

		FieldFacet fieldFacet = new FieldFacet(facetName);
		fieldFacet.setLimit(100);
		fieldFacet.setMinCount(1);
		searchParameters.addFieldFacet(fieldFacet);

		ResultSet rs = searchService.query(searchParameters);
		
		List<Pair<String, Integer>> facettPairs = rs.getFieldFacet(facetName);
		
		Map<String, String> captions = widget.getValuesAsMap();
		for (Pair<String, Integer> pair : facettPairs) {
			
			//solr 4 bug: leave out zero values
			if(pair.getSecond() == 0){
				continue;
			}

			String hit = pair.getFirst(); // new String(pair.getFirst().getBytes(), "UTF-8");
			
			if(hit.toLowerCase().contains(value.toLowerCase())){
			
				SuggestFacetDTO dto = new SuggestFacetDTO();
				dto.setFacet(hit);
				dto.setDisplayString(captions.get(hit));
				
				result.add(dto);
			}
		}
		return result;
		
	}

	public static List<? extends  SuggestOracle.Suggestion> getSuggestions(MetadataSetV2 mds,String queryId,String parameterId,String value) throws IllegalArgumentException  {
		MetadataWidget widget=mds.findWidget(parameterId);
		
		String source=widget.getSuggestionSource();
		if(source==null){
			source=widget.getValues()!=null ? MetadataReaderV2.SUGGESTION_SOURCE_MDS : MetadataReaderV2.SUGGESTION_SOURCE_SOLR;
		}
		if(source.equals(MetadataReaderV2.SUGGESTION_SOURCE_SOLR)){
			MetadataQueryParameter parameter = getParameter(mds.getQueries(),queryId,parameterId);
			return getSuggestionsSolr(parameter, widget, value);
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
			logger.error(e.getMessage());
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
}