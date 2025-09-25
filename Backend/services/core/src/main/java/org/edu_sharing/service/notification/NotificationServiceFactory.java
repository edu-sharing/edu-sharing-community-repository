package org.edu_sharing.service.notification;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;


public interface NotificationServiceFactory extends AppContextServiceFactory<NotificationService> {

    static NotificationServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(NotificationServiceFactory.class);
    }

}
