package org.edu_sharing.repository.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.SearchCriterias;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.rpc.SearchToken;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tracking.TrackingEvent;
import org.edu_sharing.repository.server.tools.metadataset.MetadataSetHelper;
import org.edu_sharing.repository.server.tracking.TrackingService;

public aspect TrackingSearch {
	
	/**
	 * Search Tracking
	 * @param searchToken
	 */
	pointcut search(MCAlfrescoServiceImpl service, SearchToken searchToken) : target(service) && execution(public SearchResult MCAlfrescoServiceImpl.search(SearchToken)) && args(searchToken);
	
	Logger logger = Logger.getLogger(TrackingSearch.class);
	
	after(MCAlfrescoServiceImpl service, SearchToken searchToken) returning: search(service, searchToken){
		logger.info("starting");
		
		HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>  mdsSearchData = searchToken.getSearchCriterias().getMetadataSetSearchData();
		
		HashMap<String,String> authInfo = null;
		try{
			authInfo = service.getAuthInfo(null);
		}catch(Throwable e){
			e.printStackTrace();
		}
		
		if(mdsSearchData != null){
		
		   	MetadataSetHelper mdsHelper = new MetadataSetHelper();
			
			TrackingService.track(
				TrackingEvent.ACTIVITY.SEARCH_QUERY, 
				new TrackingEvent.CONTEXT_ITEM[] {
					new TrackingEvent.CONTEXT_ITEM(TrackingEvent.CONTEXT.REPOSITORY_ID, searchToken.getRepositoryId()),
					new TrackingEvent.CONTEXT_ITEM(TrackingEvent.CONTEXT.SEARCH_QUERY, mdsHelper.getStatistikQueryString(mdsSearchData)),
					new TrackingEvent.CONTEXT_ITEM(TrackingEvent.CONTEXT.SEARCH_LEVEL, mdsHelper.isExtendedSearch(mdsSearchData) ? "EXTENDED" : "SIMPLE")
				},
				TrackingEvent.PLACE.SEARCH, 
				authInfo
			);
		}
		logger.info("returning");
	}
	
	
	pointcut searchByParentId(MCAlfrescoServiceImpl service, SearchCriterias searchCriterias, String parentId) : target(service) && execution(HashMap<String, HashMap<String, Object>> searchByParentId(SearchCriterias, String)) && args(searchCriterias,parentId);
	
	after(MCAlfrescoServiceImpl service, SearchCriterias searchCriterias, String parentId) returning: searchByParentId(service, searchCriterias,parentId){
	
		HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>  mdsSearchData = searchCriterias.getMetadataSetSearchData();
		
		HashMap<String,String> authInfo = null;
		try{
			authInfo = service.getAuthInfo(null);
		}catch(Throwable e){
			e.printStackTrace();
		}
		
		if(mdsSearchData != null){
		
			MetadataSetHelper mdsHelper = new MetadataSetHelper();
					
			TrackingService.track(
					
					TrackingEvent.ACTIVITY.SEARCH_QUERY,
					new TrackingEvent.CONTEXT_ITEM[] {
							new TrackingEvent.CONTEXT_ITEM(TrackingEvent.CONTEXT.REPOSITORY_ID, searchCriterias.getRepositoryId()),
							new TrackingEvent.CONTEXT_ITEM(TrackingEvent.CONTEXT.SEARCH_QUERY, mdsHelper.getStatistikQueryString(mdsSearchData)),
							new TrackingEvent.CONTEXT_ITEM(TrackingEvent.CONTEXT.SEARCH_LEVEL, mdsHelper.isExtendedSearch(mdsSearchData) ? "EXTENDED" : "SIMPLE")
					},					
					TrackingEvent.PLACE.EXPLORER, 
					authInfo
					);
									
		}

	}
	
}
