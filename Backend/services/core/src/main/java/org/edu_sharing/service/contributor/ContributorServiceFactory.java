package org.edu_sharing.service.contributor;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public interface ContributorServiceFactory extends AppContextServiceFactory<ContributorService> {
    static ContributorServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(ContributorServiceFactory.class);
    }
}
