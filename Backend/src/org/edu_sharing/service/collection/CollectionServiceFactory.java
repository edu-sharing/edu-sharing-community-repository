package org.edu_sharing.service.collection;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;

public class CollectionServiceFactory {
	
	public static CollectionService getCollectionService(String appId){
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
		if(!ProviderHelper.hasProvider(appInfo)){
			return getLocalService();

		}else{
			return ProviderHelper.getProviderByApp(appInfo).getCollectionService();
		}
	}
	public static CollectionService getLocalService() {
		CollectionServiceConfig config = (CollectionServiceConfig)ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
		return CollectionServiceImpl.build(ApplicationInfoList.getHomeRepository().getAppId());
	}
	public static NodeRef getCollectionHome(){
		CollectionServiceConfig config = (CollectionServiceConfig)ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
		String[] path=config.getPath().split(":");
		return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,NodeServiceFactory.getLocalService().findNodeByName(NodeServiceHelper.getCompanyHome().getId(),path[path.length-1]));
	}
}
