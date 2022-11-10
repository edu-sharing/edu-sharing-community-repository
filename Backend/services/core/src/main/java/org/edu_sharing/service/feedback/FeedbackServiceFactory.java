package org.edu_sharing.service.feedback;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.spring.ApplicationContextFactory;

public class FeedbackServiceFactory {
	public static FeedbackService getFeedbackService(String appId){

		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);

		if(!ProviderHelper.hasProvider(appInfo)){
			return getLocalService();

		}else{
			return ProviderHelper.getProviderByApp(appInfo).getFeedbackService();
		}
	}

	public static FeedbackService getLocalService(){
		return (FeedbackService) ApplicationContextFactory.getApplicationContext().getBean("feedbackService");
	}
}
