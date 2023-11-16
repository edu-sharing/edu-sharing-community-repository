package org.edu_sharing.service.search;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.provider.ElasticSearchProvider;
import org.edu_sharing.service.provider.Provider;
import org.edu_sharing.service.provider.ProviderHelper;

public class SearchServiceFactory {
	
	static Logger logger = Logger.getLogger(SearchServiceFactory.class);
	
	public static SearchService getSearchService(String appLicationId){
		
		SearchService searchService = null;
		ApplicationInfo appInfo = (appLicationId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appLicationId);

		if(!ProviderHelper.hasProvider(appInfo)){
			searchService = new SearchServiceImpl(appInfo.getAppId());
		}else{
			return ProviderHelper.getProviderByApp(appInfo).getSearchService();
		}
		
		return searchService;
	}

	public static SearchService getLocalService() {
		ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
		if(ProviderHelper.hasProvider(appInfo)){
			Provider provider = ProviderHelper.getProviderByApp(appInfo);
			if(appInfo.ishomeNode()) {
				if(provider instanceof ElasticSearchProvider) {
					return provider.getSearchService();
				} else {
					logger.error("Invalid provider for home repo: " + provider.getClass().getName() + ". Check your homeApp config key for " + ApplicationInfo.KEY_REMOTE_PROVIDER);
					return new SearchServiceImpl(appInfo.getAppId());
				}
			}
			return provider.getSearchService();
		}else{
			return new SearchServiceImpl(appInfo.getAppId());
		}

	}
}
