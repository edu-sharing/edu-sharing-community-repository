package org.edu_sharing.service.rating;

import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.comment.CommentServiceImpl;

public class RatingServiceFactory {
	public static RatingService getRatingService(String appId){

		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);

		if(appInfo.getNodeService() == null || appInfo.getNodeService().trim().equals("")){
			return getLocalService();

		}else{
			throw new NotImplementedException("No rating service for remote repos yet");
		}
	}

	public static RatingService getLocalService(){
		return new RatingServiceImpl();
	}
}
