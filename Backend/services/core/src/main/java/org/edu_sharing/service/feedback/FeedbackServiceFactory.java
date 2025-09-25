package org.edu_sharing.service.feedback;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;


public interface FeedbackServiceFactory extends AppContextServiceFactory<FeedbackService> {
    static FeedbackServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(FeedbackServiceFactory.class);
    }
}
