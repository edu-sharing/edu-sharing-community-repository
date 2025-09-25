package org.edu_sharing.service.rendering;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;


/**
 * Factory interface for creating and retrieving instances of {@code RenderingService}.
 *
 * The {@code RenderingServiceFactory} extends the {@code AppContextServiceFactory<RenderingService>}
 * to provide application context-specific access to rendering services. It facilitates the dynamic resolution
 * and retrieval of {@code RenderingService} instances in a multi-application or context-aware environment.
 *
 * This factory provides methods to interact with rendering functionalities encapsulated within
 * {@code RenderingService}, supporting tasks such as rendering metadata retrieval, content rendering,
 * and version-specific rendering operations. The integration with {@code ApplicationContextFactory}
 * allows seamless access to this factory as a bean in the Spring application context.
 *
 * Responsibilities:
 * - Provides access to {@code RenderingService} via application context.
 * - Supports retrieval of services that are specific to the active application context.
 *
 * Static Methods:
 * - {@code getInstance()}: Retrieves the singleton instance of the {@code RenderingServiceFactory}
 *   from the Spring application context. The factory itself is assumed to be configured
 *   and managed within the Spring container.
 *
 * The {@code RenderingServiceFactory} plays a key role in scenarios where rendering services need to operate
 * across modular, distributed, or tenant-aware components within an application.
 */
public interface RenderingServiceFactory extends AppContextServiceFactory<RenderingService> {
    static RenderingServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(RenderingServiceFactory.class);
    }
}
