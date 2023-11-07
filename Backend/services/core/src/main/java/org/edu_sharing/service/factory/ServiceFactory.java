package org.edu_sharing.service.factory;

import org.edu_sharing.service.notification.NotificationService;

public interface ServiceFactory {
    NotificationService getServiceByAppId(String appId);

    NotificationService getLocalService();
}
