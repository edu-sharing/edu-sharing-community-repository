package org.edu_sharing.service.organization;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public interface OrganizationServiceFactory extends AppContextServiceFactory<OrganizationService> {

    static OrganizationServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(OrganizationServiceFactory.class);
    }

}
