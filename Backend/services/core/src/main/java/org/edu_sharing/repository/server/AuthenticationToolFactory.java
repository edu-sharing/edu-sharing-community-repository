package org.edu_sharing.repository.server;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;


/**
 * A factory interface for creating and retrieving instances of {@link AuthenticationTool}
 * specific to an application context.
 *
 * The {@code AuthenticationToolFactory} provides a means to obtain implementations of
 * {@link AuthenticationTool} that are scoped to a particular application context or the
 * local application context. This is particularly useful in environments with multiple applications
 * or tenants, enabling authentication-related operations to be dynamically resolved based on the
 * current context.
 *
 * This interface extends {@link AppContextServiceFactory}, inheriting methods to retrieve
 * services based on application identifiers or the local context.
 *
 * Key Responsibilities:
 * - Facilitates the resolution of context-aware authentication tools.
 * - Supports multi-tenant or multi-application authentication requirements.
 * - Ensures dynamic retrieval of {@link AuthenticationTool} implementations for different contexts.
 */
public interface AuthenticationToolFactory extends AppContextServiceFactory<AuthenticationTool> {
}
