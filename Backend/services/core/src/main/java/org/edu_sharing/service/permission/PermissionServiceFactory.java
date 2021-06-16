package org.edu_sharing.service.permission;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.spring.ApplicationContextFactory;

public class PermissionServiceFactory {


	public static PermissionService getPermissionService(String appId){
		
		PermissionService permissionService = null;
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);

		if(!ProviderHelper.hasProvider(appInfo)){
			permissionService = getLocalService();
		}else{
			return ProviderHelper.getProviderByApp(appInfo).getPermissionService();
		}
		
		return permissionService;
	}

	public static PermissionService getLocalService() {
		return (PermissionService)ApplicationContextFactory.getApplicationContext().getBean("permissionService");
	}
	
}
