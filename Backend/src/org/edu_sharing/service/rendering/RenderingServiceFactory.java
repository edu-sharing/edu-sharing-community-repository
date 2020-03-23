package org.edu_sharing.service.rendering;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.provider.ProviderHelper;

public class RenderingServiceFactory {
	
	public static RenderingService getRenderingService(String appId){
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
		if(!ProviderHelper.hasProvider(appInfo)) {
			return new RenderingServiceImpl(appId);
		} else {
			return ProviderHelper.getProviderByApp(appInfo).getRenderingService();
		}
	}
	public static RenderingService getLocalService(){
		return new RenderingServiceImpl(ApplicationInfoList.getHomeRepository().getAppId());
	}
}
