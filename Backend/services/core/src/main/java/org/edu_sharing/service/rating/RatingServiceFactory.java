package org.edu_sharing.service.rating;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;


public interface RatingServiceFactory extends AppContextServiceFactory<RatingService> {
    static RatingServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(RatingServiceFactory.class);
    }
}
