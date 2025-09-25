package org.edu_sharing.service.search;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;


public interface SearchServiceFactory extends AppContextServiceFactory<SearchService> {
    static SearchServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(SearchServiceFactory.class);
    }
}
