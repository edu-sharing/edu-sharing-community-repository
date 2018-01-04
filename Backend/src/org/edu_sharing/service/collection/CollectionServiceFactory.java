package org.edu_sharing.service.collection;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.spring.ApplicationContextFactory;

public class CollectionServiceFactory {
	
	public static CollectionService getCollectionService(String appId){
		CollectionServiceConfig config = (CollectionServiceConfig)ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
		return new CollectionServiceImpl(appId, config.getPattern(), config.getPath());
	}
	public static CollectionService getLocalService() {
		return CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
	}
}
