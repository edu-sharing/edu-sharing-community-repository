package org.edu_sharing.repository.server.appcontext;


import org.springframework.stereotype.Indexed;

/**
 * A generic factory interface for providing application context-specific services.
 *
 * The {@code AppContextServiceFactory} interface serves as a contract for obtaining
 * services that are bound to either a specific application context or the local
 * application context. This abstraction is particularly useful in multi-application
 * or multi-tenant environments where individual services may need to be resolved
 * or instantiated dynamically based on the target application context.
 *
 * Key Features:
 * - Allows the resolution of context-aware services via application-specific identifiers.
 * - Supports retrieval of services for the local application context.
 * - Facilitates modular and scalable design patterns in multi-application environments.
 *
 * @param <T> The type of the service object that is being resolved and returned by the factory.
 */
@Indexed
public interface AppContextServiceFactory<T> {
    T getService(String appId);
    T getLocalService();
    T getService();
}
