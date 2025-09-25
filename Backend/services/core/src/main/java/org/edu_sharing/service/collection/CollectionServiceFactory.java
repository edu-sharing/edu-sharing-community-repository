package org.edu_sharing.service.collection;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.spring.ApplicationContextFactory;


/**
 * Factory interface for retrieving instances of {@link CollectionService}.
 *
 * The {@code CollectionServiceFactory} extends the generic {@code AppContextServiceFactory} to
 * provide collection-related service instances within an application context. This interface
 * integrates methods for resolving the application's collection service and accessing
 * configurations and resources tied to the collection service's functionality.
 *
 * Key Responsibilities:
 * - Supports retrieval of service instances for collection-related operations.
 * - Provides utility methods to resolve the home directory for collections using configured paths.
 *
 * Thread Safety:
 * Implementations are typically thread-safe as they utilize centralized application context mechanisms.
 *
 * Methods:
 * - `getInstance`: Retrieves the singleton instance of this factory from the application context.
 * - `getCollectionHome`: Resolves and retrieves the home directory for the collections based on the configuration.
 */
public interface CollectionServiceFactory extends AppContextServiceFactory<CollectionService> {
    static CollectionServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(CollectionServiceFactory.class);
    }

    static NodeRef getCollectionHome() {
        CollectionServiceConfig config = (CollectionServiceConfig) ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
        String[] path = config.path().split(":");
        return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, NodeServiceFactory.getInstance().getLocalService().findNodeByName(NodeServiceHelper.getCompanyHome().getId(), path[path.length - 1]));
    }
}
