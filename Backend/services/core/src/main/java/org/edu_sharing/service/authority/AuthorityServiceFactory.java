package org.edu_sharing.service.authority;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.spring.ApplicationContextFactory;


/**
 * Factory interface for creating and retrieving instances of {@code AuthorityService}.
 * This interface extends {@code AppContextServiceFactory} with a specific parameterization
 * for {@code AuthorityService}, enabling the resolution of AuthorityService implementations
 * bound to a specific application context.
 *
 * This factory provides a mechanism to access the {@code AuthorityService} both in a
 * general multi-application environment and for the local application context.
 *
 * Key Functionalities:
 * - Acts as a centralized access point for retrieving {@code AuthorityService} instances.
 * - Leverages the Spring Application Context to ensure proper configuration and context-aware
 *   service resolution.
 * - Facilitates the use of {@code AuthorityService} in applications that require dynamic
 *   or context-specific service instantiation.
 *
 * Usage:
 * The factory ensures that an instance of {@code AuthorityServiceFactory} can be retrieved
 * using the static {@code getInstance} method, which fetches the required bean through the
 * Spring Application Context. This allows for seamless integration within Spring-managed
 * environments.
 */
public interface AuthorityServiceFactory extends AppContextServiceFactory<AuthorityService> {
    static AuthorityServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(AuthorityServiceFactory.class);
    }
}
