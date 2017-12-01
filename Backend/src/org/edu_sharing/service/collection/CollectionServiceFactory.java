package org.edu_sharing.service.collection;

import org.edu_sharing.spring.ApplicationContextFactory;

public class CollectionServiceFactory {
	
	public static CollectionService getCollectionService(String appId){
		CollectionServiceConfig config = (CollectionServiceConfig)ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
		return new CollectionServiceImpl(appId, config.getPattern(), config.getPath());
	}
	
}
