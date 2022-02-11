package org.edu_sharing.service.rating;

import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.comment.CommentServiceImpl;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.spring.ApplicationContextFactory;

public class RatingServiceFactory {
	public static RatingService getRatingService(String appId){

		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);

		if(!ProviderHelper.hasProvider(appInfo)){
			return getLocalService();

		}else{
			return ProviderHelper.getProviderByApp(appInfo).getRatingService();
		}
	}

	public static RatingService getLocalService(){
		return (RatingService) ApplicationContextFactory.getApplicationContext().getBean("ratingService");
	}
}
