package org.edu_sharing.service.comment;

import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.provider.ProviderHelper;

public class CommentServiceFactory {
	public static CommentService getCommentService(String appId){

		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);

		if(!ProviderHelper.hasProvider(appInfo)) {
			return getLocalService();

		}else{
			return ProviderHelper.getProviderByApp(appInfo).getCommentService();
		}
	}

	private static CommentService getLocalService(){
		return new CommentServiceImpl();
	}
}
