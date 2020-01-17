package org.edu_sharing.service.comment;

import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class CommentServiceFactory {
	public static CommentService getCommentService(String appId){

		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);

		if(appInfo.getNodeService() == null || appInfo.getNodeService().trim().equals("")){
			return getLocalService();

		}else{
			throw new NotImplementedException("No comment service for remote repos yet");
		}
	}

	private static CommentService getLocalService(){
		return new CommentServiceImpl();
	}
}
