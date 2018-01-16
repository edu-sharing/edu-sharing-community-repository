package org.edu_sharing.service.notification;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;

import com.sun.star.lang.IllegalArgumentException;

public class NotificationServiceFactory {

	
	public static NotificationService getNotificationService(String appId) throws IllegalArgumentException{
		
		NotificationService notificationService = null;
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
		
		if(appInfo.ishomeNode()){
			notificationService = new NotificationServiceImpl(appId);
		}else{
			notificationService = new NotificationServiceImpl(appId);
			//throw new IllegalArgumentException("NotificationService is currently only available for home repository");
		}
		
		return notificationService;
	}
	public static NotificationService getLocalService() {
		return new NotificationServiceImpl(ApplicationInfoList.getHomeRepository().getAppId());
	}
}
