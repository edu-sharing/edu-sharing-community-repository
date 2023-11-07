package org.edu_sharing.service.notification;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.factory.ServiceFactory;

public class NotificationServiceFactoryImpl implements ServiceFactory {
	@Override
	public NotificationService getServiceByAppId(String appId) {
		return new NotificationServiceImpl(appId);
	}

	@Override
	public NotificationService getLocalService() {
		return new NotificationServiceImpl(ApplicationInfoList.getHomeRepository().getAppId());
	}
}
