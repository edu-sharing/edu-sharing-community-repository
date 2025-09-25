package org.edu_sharing.service.editlock;

import org.edu_sharing.spring.ApplicationContextFactory;

/**
 * Factory class to retrieve an instance of {@link EditLockService}.
 *
 * This class is marked as deprecated, indicating that its usage is no longer recommended.
 * Consider using an alternative mechanism for getting instances of {@link EditLockService}.
 *
 * The method provided in this class retrieves the {@link EditLockService} implementation
 * from the Spring application context.
 *
 * The {@link ApplicationContextFactory} is used to access the Spring application context
 * and fetch the required service bean.
 *
 * Deprecated. Use dependency injection for getting service instances.
 */
@Deprecated
public class EditLockServiceFactory {
	public static EditLockService getEditLockService(){
        return  ApplicationContextFactory.getApplicationContext().getBean(EditLockService.class);
	}
}
