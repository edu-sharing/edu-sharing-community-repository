package org.edu_sharing.service.rendering;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class RenderingServiceFactory {
	
	public static RenderingService getRenderingService(String appId){
		return new RenderingServiceImpl(appId);
	}
	public static RenderingService getLocalService(){
		return new RenderingServiceImpl(ApplicationInfoList.getHomeRepository().getAppId());
	}
}
