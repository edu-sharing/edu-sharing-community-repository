package org.edu_sharing.service.search;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class SearchServiceFactory {
	
	static Logger logger = Logger.getLogger(SearchServiceFactory.class);
	
	public static SearchService getSearchService(String appLicationId){
		
		SearchService searchService = null;
		ApplicationInfo appInfo = (appLicationId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appLicationId);
		
		if(appInfo.getSearchService() == null || appInfo.getSearchService().trim().equals("")){
			searchService = new SearchServiceImpl(appLicationId);
		}else{
			try{
				Class clazz = Class.forName(appInfo.getSearchService());
				Object obj = clazz.getConstructor(new Class[] { String.class}).newInstance(new Object[] { appLicationId });
				searchService = (SearchService)obj;
			}catch(Exception e){
				logger.error(e.getMessage(),e);
				throw new RuntimeException(e);
			}
		}
		
		return searchService;
	}

	public static SearchServiceImpl getLocalService() {
		return new SearchServiceImpl(ApplicationInfoList.getHomeRepository().getAppId());
	}
}
