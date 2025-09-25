package org.edu_sharing.service.relations;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public interface RelationServiceFactory extends AppContextServiceFactory<RelationService> {

    static RelationServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(RelationServiceFactory.class);
    }
}
