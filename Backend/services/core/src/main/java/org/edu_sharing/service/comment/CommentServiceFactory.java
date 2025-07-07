package org.edu_sharing.service.comment;

import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.spring.ApplicationContextFactory;

public class CommentServiceFactory {
	public static CommentService getCommentService(String appId){

		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);

		if(!ProviderHelper.hasProvider(appInfo)) {
			return getLocalService();

		}else{
			return ProviderHelper.getProviderByApp(appInfo).getCommentService();
		}
	}

	public static CommentService getLocalService(){
		return ApplicationContextFactory.getApplicationContext().getBean(CommentService.class);
	}
}
