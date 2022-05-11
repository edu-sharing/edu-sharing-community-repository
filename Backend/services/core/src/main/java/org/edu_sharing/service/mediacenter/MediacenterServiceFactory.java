package org.edu_sharing.service.mediacenter;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.provider.ProviderHelper;

public class MediacenterServiceFactory {
	
	public static MediacenterService getInstance() throws NotAnAdminException{
		if(!AuthorityServiceFactory.getLocalService().isGlobalAdmin()){
			throw new NotAnAdminException();
		}
		return new MediacenterServiceImpl();
	}
	public static MediacenterService getLocalService(){
		return new MediacenterServiceImpl();
	}

	public static MediacenterService getMediacenterService(String appId){

		MediacenterService mediacenterService = null;
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);

		if(!ProviderHelper.hasProvider(appInfo)){
			return getLocalService();

		}else{
			if(appInfo.ishomeNode()){
				return getLocalService();
			}
			//return ProviderHelper.getProviderByApp(appInfo).getMediacenterService();
			throw new RuntimeException("not yet implemented");
		}
	}

}
