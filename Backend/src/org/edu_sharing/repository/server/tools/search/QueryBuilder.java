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
package org.edu_sharing.repository.server.tools.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueries;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.search.QueryHandler;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.metadataset.MetadataCache;

import com.google.gwt.user.client.rpc.IsSerializable;

public class QueryBuilder extends QueryBuilderBase implements IsSerializable {

	String searchString = "";

	public static int operatorAND = 0;

	public static int operatorOR = 1;

	public static HashMap typeSearchMap = null;

	String[] contentkind = null;
	String[] aspects = null;

	String searchWord = null;
	
	Logger logger = Logger.getLogger(QueryBuilder.class);

	public QueryBuilder() {
	}

	private String getSearchString(String field,String[] types) {
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

	public void setSearchWord(String _searchWord) {
		this.searchWord = _searchWord;
	}
	
	public void setSearchWord_OLD(String _searchWord) {
		
		if (_searchWord != null && !_searchWord.trim().equals("")) {

			String placeholder = "#searchstring#";
			
			// not fuzzy

			String[] tosearch = new String[] { "TEXT:" + placeholder, "@cm\\:name:" + placeholder,
					"@cclom\\:title:" + placeholder, "@cclom\\:general_description:" + placeholder,
					"@ccm\\:publisher:" + placeholder, "@ccm\\:license:" + placeholder, "@ccm\\:author:" + placeholder,
					"@ccm\\:audience:" + placeholder, "@ccm\\:semantictype:" + placeholder,
					"@cclom\\:taxon_entry:" + placeholder, "@cclom\\:classification_keyword:" + placeholder,
					"@cclom\\:learningresourcetype:" + placeholder, "@cclom\\:interactivitytype:" + placeholder,
					"@cclom\\:context:" + placeholder, "@ccm\\:learninggoal:" + placeholder,
					"@ccm\\:guidancestudents:" + placeholder, "@ccm\\:guidanceteachers:" + placeholder };

			if (!searchString.equals(""))
				searchString += " AND (";
			else
				searchString += " (";
			
			for (int i = 0; i < tosearch.length; i++) {
				if (i == 0)
					searchString += tosearch[i].replaceAll(placeholder, _searchWord);
				else
					searchString += " OR " + tosearch[i].replaceAll(placeholder, _searchWord);
			}
			searchString += ")";

		}
	}

	public void setContentKind(String[] _contentkind) {
		contentkind = _contentkind;
	}

	public String getSearchString() {
		
		String searchTypesString = null;
		String searchAspectsString = null;
		if(this.contentkind == null || this.contentkind.length < 1){
			//default search for IO's
			searchTypesString = "TYPE:\"" + CCConstants.CCM_TYPE_IO + "\"";
		}else{
			searchTypesString = getSearchString("TYPE",this.contentkind);
		}
		
		if(this.aspects!=null){
			searchAspectsString = getSearchString("ASPECT",this.aspects);
		}
			
		if (searchString != null && !searchString.trim().equals("")) {
			searchString += " AND " + searchTypesString;
			
		}else if(searchString == null || searchString.trim().equals("")){
			searchString = searchTypesString;
		}
		
		if(searchAspectsString!=null)
			searchString+=" AND "+searchAspectsString;
		return searchString;
	}
	
	public void setMetadataSetQuery(String repositoryId, String metadataSetId, String standaloneMetadataSetName, HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> map) {
		
		//search word stuff
		if(this.searchWord != null && !searchWord.trim().equals("")){
			
			//escape evil chars
			
			logger.info("searchWord:"+this.searchWord);
			this.searchWord = QueryParser.escape(this.searchWord);
			logger.info("searchWord escaped:"+this.searchWord);
			
			if(map != null && map.entrySet() != null){
			
				MetadataSetQuery mdsq = map.entrySet().iterator().next().getKey();
				if(mdsq != null){
			
					MetadataSetQueries mdsqs = mdsq.getParent();
					if(mdsqs != null){
						String statementSearchWord = mdsqs.getStatementsearchword();
				
						if(statementSearchWord != null){
							
							statementSearchWord = statementSearchWord.replace("${value}", this.searchWord );
							if (searchString.equals("")) searchString = statementSearchWord;
							else searchString += " AND " +statementSearchWord;
						}
					}else{
						logger.info("trying a searchWord query, but missing the MetadataSetQueries object(parent of MetadataSetQuery)");
					}
				}
			}
		}
			
		String result = null;
		if(map != null){
			for (Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> entry : map.entrySet()) {
				String querystring = null;
				MetadataSetQuery mdsQuery = entry.getKey();
				String rawQueryStatement = mdsQuery.getStatement();
				String queryJoin = mdsQuery.getJoin();
				queryJoin = (queryJoin == null || queryJoin.equals("")) ? "OR" : queryJoin;
				String handlerclass = mdsQuery.getHandlerclass();
				if (handlerclass != null && !handlerclass.trim().equals("")) {
					try {
						Class clazz = Class.forName(handlerclass);
						QueryHandler qh = null;
	
						qh = (QueryHandler) clazz.getConstructor(new Class[] {}).newInstance(new Object[] {});
						querystring = qh.getStatement(mdsQuery, entry.getValue());
					} catch (Exception e) {
						e.printStackTrace();
					}
	
				} else {
					HashMap<MetadataSetQueryProperty, String[]> propValMap = entry.getValue();
					
					for (Map.Entry<MetadataSetQueryProperty, String[]> propValueMapEntry : propValMap.entrySet()) {
						MetadataSetQueryProperty clientSideProp = propValueMapEntry.getKey();
						/**
						 * For security reasons:
						 * getting the serverside version of the prop (the statement or other params could be manipulated)
						 */
						MetadataSetQueryProperty prop = (MetadataSetQueryProperty)MetadataCache.getMetadataSetProperty(clientSideProp.getId());
						if(prop == null){
							logger.error("can not find an serverside representation for property with name:"+clientSideProp.getName()+" id:"+clientSideProp.getId());
							continue;
						}
						
						String[] values = propValueMapEntry.getValue();
						
						//we want to keep the original searchCriterias (for the SearchResult) so clone before escape 
						values = values.clone();
						if (values != null && values.length > 0) {
							
							//escape evil chars
							if(prop.getEscape()){
								for(int i = 0; i < values.length; i++){
									String value = values[i];
									
									if(value != null){
										//http://issues.alfresco.com/jira/browse/ALF-4717
										//remove double quotes bevore escape 
										value = value.replace("\"", "");
										value = QueryParser.escape(value);
									}
									values[i] = value;
								}
							}else{
								logger.info("dont escape:"+prop.getName());
							}
							
							//do a trim to prevent searches like "*geschichte *" bringing alf34e/lucene down
							for(int i = 0; i < values.length; i++){
								String value = values[i];
								if(value != null){
									value = value.trim();
								}
								values[i] = value;
							}
						
					
							if (rawQueryStatement != null && !rawQueryStatement.equals("")) {
								// take the property name to replace the value
								// placeholder in the query statement
								// only one value is allowed
								if (values[0] != null) {
									rawQueryStatement = rawQueryStatement.replace("${" + prop.getName() + "}",
											values[0]);
								}
							}else {
								String statement = null;
								if (prop.getMultiple()) {
									for (String value : values) {
										if (value != null && !value.trim().equals("")) {
											
											String tmpstatement = null;
											//default get statement from statement prop
											if(prop.getStatement() != null && !prop.getStatement().trim().equals("")){
												tmpstatement =  prop.getStatement().replace(CCConstants.metadatasetsearch_valuekey, value);
											}
											//else get statement from valuespace
											else if(prop.getValuespace() != null && prop.getValuespace().size() > 0 && prop.getValuespace().get(0).getStatement() != null){
												MetadataSetValueKatalog kata = prop.getMetadataSetValueKatalog(value);
												if(kata != null && kata.getStatement() != null && !kata.getStatement().trim().equals("")){
													tmpstatement = kata.getStatement();
												}
											}
											if(tmpstatement != null){
												String multipleJoin = prop.getMultiplejoin();
												multipleJoin = (multipleJoin == null || multipleJoin.equals("")) ? "OR"
														: multipleJoin;
												if (statement == null) {
													statement = "(" + tmpstatement;
												} else {
													statement += " " + multipleJoin + " " + tmpstatement;
												}
											}
										}
									}
									if (statement != null)
										statement += ")";
								} else {
									if (values[0] != null && !values[0].trim().equals("")) {
										//default get statement from statement prop
										if(prop.getStatement() != null && !prop.getStatement().trim().equals("")){
											statement = prop.getStatement().replace(CCConstants.metadatasetsearch_valuekey, values[0]);
										}
										//else get statement from valuespace
										else{
											MetadataSetValueKatalog kata = prop.getMetadataSetValueKatalog(values[0]);
											if(kata != null && kata.getStatement() != null && !kata.getStatement().trim().equals("")){
												statement = kata.getStatement();
											}
										}
									}
								}
	
								if (statement != null) {
									if (querystring == null) {
										querystring = statement;
									} else {
										querystring += " " + queryJoin + " " + statement;
									}
								}
							}
	
						}
					}
				}
	
				if (querystring != null) {
					if(result == null) result = querystring;
					else result += " AND "+ querystring;
				}
				if (rawQueryStatement != null && !rawQueryStatement.contains("${") && !rawQueryStatement.contains("}")) {
					if (result == null) {
						result = rawQueryStatement;
					} else {
						result += " AND " + rawQueryStatement;
					}
				}
	
			}
		}

		if (result != null && !result.trim().equals("")) {
			if (this.searchString != null && !this.searchString.trim().equals("")) {
				this.searchString += " AND " + result;
			} else {
				this.searchString = result;
			}
		}
		
		
		String basequery = null;
		try{
			MetadataSetV2 mds = MetadataHelper.getMetadataset(ApplicationInfoList.getRepositoryInfoById(repositoryId), metadataSetId);
			basequery=mds.getQueries().getBasequery();
		}catch(Exception e){
			
		}
				
		logger.info("THE BASEQUERY:"+basequery);
		if(basequery != null && !basequery.trim().equals("")){
			if (this.searchString != null && !this.searchString.trim().equals("")) {
				this.searchString += " AND " + basequery;
			} else {
				this.searchString = basequery;
			}
		}
	}

	@Override
	public void setAspects(String[] _aspects) {
		this.aspects=_aspects;
	}

}
