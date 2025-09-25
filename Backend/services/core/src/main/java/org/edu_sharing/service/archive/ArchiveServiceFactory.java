package org.edu_sharing.service.archive;

import org.edu_sharing.repository.server.appcontext.AppContextServiceLocator;


/**
 * A factory class for providing access to {@link ArchiveService} instances.
 *
 * This class uses the context-aware service locator {@link AppContextServiceLocator}
 * to retrieve beans of type {@link ArchiveService} either for a specified application
 * context or for the local application context.
 *
 * Note: This class is marked as deprecated and usage should be avoided in favor of
 * dependency injection or other contemporary methods of service discovery and management.
 *
 * Dependency injection and context-aware mechanisms are suggested as alternatives for better modularity and maintainability.
 * - Use Qualifier for forced local services or the ArchiveService Interface for dynamic resolution.
 * - You can also use the AppContextServiceLocator.get(ArchiveService.class) method to retrieve a service instance.
 */
@Deprecated
public class ArchiveServiceFactory {

    private static final AppContextServiceLocator locator = AppContextServiceLocator.getInstance();

    public static ArchiveService getArchiveService(String appId) {
        return locator.get(ArchiveService.class, appId);
    }

    public static ArchiveService getLocalService() {
        return locator.getLocal(ArchiveService.class);
    }
}
