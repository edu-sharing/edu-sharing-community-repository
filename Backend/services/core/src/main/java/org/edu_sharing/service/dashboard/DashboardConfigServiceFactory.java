package org.edu_sharing.service.dashboard;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

/**
 * Factory interface for creating and managing instances of {@code DashboardConfigService}.
 *
 * The {@code DashboardConfigServiceFactory} is responsible for providing access
 * to the {@code DashboardConfigService}, which manages the retrieval and
 * configuration of dashboard shortcuts for users. It extends the
 * {@code AppContextServiceFactory} to ensure that service instances
 * are context-aware, supporting multi-application or multi-tenant
 * environments.
 *
 * Key Responsibilities:
 * - Provides a centralized mechanism to obtain an instance of {@code DashboardConfigService}.
 * - Leverages the application context to dynamically fetch the factory implementation.
 * - Ensures compatibility with context-specific requirements through the extended
 *   {@code AppContextServiceFactory} contract.
 *
 * Methods:
 * {@code getInstance()}:
 * - Retrieves a singleton instance of the {@code DashboardConfigServiceFactory}
 *   from the Spring application context.
 * - Simplifies access to the factory within the application by encapsulating
 *   the application context lookup logic.
 */
public interface DashboardConfigServiceFactory extends AppContextServiceFactory<DashboardConfigService> {
    static DashboardConfigServiceFactory getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(DashboardConfigServiceFactory.class);
    }
}
