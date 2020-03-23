package org.edu_sharing.service.nodeservice;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.spring.ApplicationContextFactory;

public class NodeServiceFactory {

	public static NodeService getNodeService(String appId){
		
		NodeService nodeService = null;
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
		
		if(!ProviderHelper.hasProvider(appInfo)){
			return getLocalService();

		}else{
			return ProviderHelper.getProviderByApp(appInfo).getNodeService();
		}
	}

	public static NodeService getLocalService() {
		return (NodeService)ApplicationContextFactory.getApplicationContext().getBean("nodeService");
	}
}
