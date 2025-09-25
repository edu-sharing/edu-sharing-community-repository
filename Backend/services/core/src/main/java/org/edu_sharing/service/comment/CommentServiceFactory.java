package org.edu_sharing.service.comment;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;


public interface CommentServiceFactory extends AppContextServiceFactory<CommentService> {
    static CommentServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(CommentServiceFactory.class);
    }
}
