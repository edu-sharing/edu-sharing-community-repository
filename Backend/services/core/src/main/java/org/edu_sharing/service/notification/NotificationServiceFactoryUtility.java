package org.edu_sharing.service.notification;

import org.edu_sharing.service.factory.ServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public class NotificationServiceFactoryUtility {
	public static final String NOTIFICATION_SERVICE_FACTORY = "notificationServiceFactory";
	private static ServiceFactory serviceFactory;

	public static NotificationService getNotificationService(String appId) {
		ServiceFactory notificationServiceFactory = getNotificationServiceFactory();
		return notificationServiceFactory.getServiceByAppId(appId);
	}

	public static NotificationService getLocalService() {
		ServiceFactory notificationServiceFactory = getNotificationServiceFactory();
		return notificationServiceFactory.getLocalService();
	}

	private static ServiceFactory getNotificationServiceFactory() {
		if(serviceFactory == null) {
			serviceFactory = (ServiceFactory) ApplicationContextFactory.getApplicationContext().getBean(NOTIFICATION_SERVICE_FACTORY);
		}
		return serviceFactory;
	}

}
