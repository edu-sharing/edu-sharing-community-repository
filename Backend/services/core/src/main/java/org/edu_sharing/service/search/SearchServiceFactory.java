package org.edu_sharing.service.search;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.provider.ProviderHelper;

public class SearchServiceFactory {
	
	static Logger logger = Logger.getLogger(SearchServiceFactory.class);
	
	public static SearchService getSearchService(String appLicationId){
		
		SearchService searchService = null;
		ApplicationInfo appInfo = (appLicationId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appLicationId);

		if(!ProviderHelper.hasProvider(appInfo)){
			searchService = new SearchServiceImpl(appLicationId);
		}else{
			return ProviderHelper.getProviderByApp(appInfo).getSearchService();
		}
		
		return searchService;
	}

	public static SearchService getLocalService() {
		ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
		if(ProviderHelper.hasProvider(appInfo)){
			return ProviderHelper.getProviderByApp(appInfo).getSearchService();
		}else{
			return new SearchServiceImpl(appInfo.getAppId());
		}

	}
}
