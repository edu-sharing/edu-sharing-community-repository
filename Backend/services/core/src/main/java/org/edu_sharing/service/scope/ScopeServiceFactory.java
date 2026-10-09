package org.edu_sharing.service.scope;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public interface ScopeServiceFactory extends AppContextServiceFactory<ScopeService> {
    static ScopeServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(ScopeServiceFactory.class);
    }
}
