package org.edu_sharing.service.notification;

import org.edu_sharing.spring.ApplicationContextFactory;

public class NotificationServiceFactory {


	private static final NotificationService notificationService = ApplicationContextFactory.getApplicationContext().getBean(NotificationService.class);


	public static NotificationService getServiceByAppId(String appId) {
		return notificationService;
	}

	public static NotificationService getLocalService() {
		return notificationService;
	}
}
