package org.edu_sharing.service.relations;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public interface NodeRelationTraceServiceFactory extends AppContextServiceFactory<NodeRelationTraceService> {

    static NodeRelationTraceServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(NodeRelationTraceServiceFactory.class);
    }
}
