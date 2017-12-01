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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.SearchCriterias;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueries;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSets;
import org.edu_sharing.repository.client.rpc.metadataset.Validator;
import org.edu_sharing.repository.client.rpc.metadataset.ValidatorMinimalOneCriteria;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.metadataset.MetadataCache;

public abstract class QueryBuilderBase implements QueryBuilderInterface{
	
	
	Logger logger = Logger.getLogger(QueryBuilderBase.class);
	
	
	String[] globalValidationIgnoreList = new String[]{MetadataSetBaseProperty.PROPERTY_NAME_CONSTANT_EDU_SHARING_REPOSITORIES};
	
	public void setSearchCriterias(SearchCriterias searchCriterieas) throws QueryValidationFailedException{
		
		validate(searchCriterieas);
		
		this.setContentKind(searchCriterieas.getContentkind());
		this.setAspects(searchCriterieas.getAspects());
		
		//setSearchWord before setMetadataSetQuery  -->QueryBuilder.java
		this.setSearchWord(searchCriterieas.getSearchWord());
		
		//if(searchCriterieas.getMetadataSetSearchData() != null){
		this.setMetadataSetQuery(searchCriterieas.getRepositoryId(), searchCriterieas.getMetadataSetId(), searchCriterieas.getStandaloneMetadataSetName(), searchCriterieas.getMetadataSetSearchData());
		//}
	}
	

	private void validate(SearchCriterias searchCriterieas) throws QueryValidationFailedException{
		
		
		
		//do global validation:
		MetadataSets metadataSets = null;
		if(searchCriterieas.getRepositoryId() == null){
			metadataSets =  RepoFactory.getMetadataSetsForRepository(ApplicationInfoList.getHomeRepository().getAppId());
		}else{
			metadataSets = RepoFactory.getMetadataSetsForRepository(searchCriterieas.getRepositoryId());
			
			//if no special metadataset for a repository is defined than take the default one of the home repository
			if(metadataSets == null){
				metadataSets =  RepoFactory.getMetadataSetsForRepository(ApplicationInfoList.getHomeRepository().getAppId());
			}
		}
		
		MetadataSet metadataSet = metadataSets.getMetadataSetById(searchCriterieas.getMetadataSetId());
		MetadataSetQueries metadataSetQueries = metadataSet.getMetadataSetQueries();
		
		boolean checkMinimalOneCriteriaIsSet = false;
		boolean minOneCriteriaIsSet = false;
		if(!metadataSetQueries.isAllowSearchWithoutCriteria()){
			checkMinimalOneCriteriaIsSet = true;
		}
		
		
		if(searchCriterieas.getMetadataSetSearchData() != null){
			HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> metadataSetSearchData = searchCriterieas.getMetadataSetSearchData();
			
			for(Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> queryEntry: metadataSetSearchData.entrySet()){
				for(Map.Entry<MetadataSetQueryProperty, String[]> propEntry : queryEntry.getValue().entrySet()){
					
					/**
					 * For Security reasons:
					 * getting the serverside version of the prop (the statement or other params could be manipulated)
					 */
					MetadataSetQueryProperty prop = (MetadataSetQueryProperty)MetadataCache.getMetadataSetProperty(propEntry.getKey().getId());
					if(prop == null){
						logger.error("can not find an serverside representation for property with name:"+propEntry.getKey().getName()+" id:"+propEntry.getKey().getId());
						//this means that it will not be handled by QueryBuilder so we ignore this
						continue;
					}else{
						List<Validator> validators = prop.getValidators();
						String[] values = propEntry.getValue();
						if(validators != null && validators.size() > 0){
							for(Validator validator : validators){
								
								if(values != null && values.length > 0){
									for(String value: values){
										boolean ok = validator.check(value);
										if(!ok){
											throw new QueryValidationFailedException(validator, value, prop);
										}
									}
								}
								
								
							}
						}
						
						
						//check if one criteria is set
						if(checkMinimalOneCriteriaIsSet && !Arrays.asList(globalValidationIgnoreList).contains(prop.getName())){
							if(values != null && values.length > 0){
								for(String value: values){
									if(new ValidatorMinimalOneCriteria().check(value)){
										minOneCriteriaIsSet = true;
									}
								}
							}
						}
							
							
						
					}
					
					
					
				}
			}
			
			
			//check check at least one criteria is set only when searchCriterieas.getMetadataSetSearchData() != null
			//cause we have to allow global search for counting the categories
			//global search is not an performance problem so we allow global search when no criteria is set
			if(checkMinimalOneCriteriaIsSet && !minOneCriteriaIsSet){
				throw new QueryValidationFailedException(new ValidatorMinimalOneCriteria(), "", null);
			}
			
		}
		
		
	}
	
}
