package org.edu_sharing.service.nodeservice;


import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

/**
 * Factory interface for obtaining instances of {@link NodeService} within an application context.
 *
 * The {@code NodeServiceFactory} extends the {@code AppContextServiceFactory} interface, specifically
 * for the resolution and management of {@code NodeService} instances. It leverages the application
 * context to locate and retrieve services that are relevant to the specific application or tenant.
 *
 * This factory provides a static method to obtain its implementation from the application context,
 * enabling seamless integration with the underlying service mechanisms.
 *
 * Responsibilities:
 * - Provide instances of {@code NodeService} for a specific application or the local context.
 * - Ensure proper resolution of {@code NodeService} instances leveraging the application context.
 *
 * Thread Safety:
 * Implementations of this factory are typically thread-safe as they rely on a centrally managed
 * application context for service resolution.
 */
public interface NodeServiceFactory extends AppContextServiceFactory<NodeService> {
    static NodeServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(NodeServiceFactory.class);
    }
}

