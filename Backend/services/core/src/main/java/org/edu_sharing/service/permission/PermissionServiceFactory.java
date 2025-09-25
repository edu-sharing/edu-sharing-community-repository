package org.edu_sharing.service.permission;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;


public interface PermissionServiceFactory extends AppContextServiceFactory<PermissionService> {
    static PermissionServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(PermissionServiceFactory.class);
    }
}
