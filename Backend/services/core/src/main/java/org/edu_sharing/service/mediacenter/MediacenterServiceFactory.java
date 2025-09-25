package org.edu_sharing.service.mediacenter;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.admin.AdminServiceImpl;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;


/**
 * Factory for managing instances of {@link MediacenterService}.
 * Provides methods to retrieve the {@link MediacenterService} implementation for different contexts,
 * allowing applications to manage mediacenter-specific operations such as data import,
 * license management, and mediacenter administration.
 *
 * This factory extends {@link AppContextServiceFactory}, inheriting functionality to retrieve
 * service instances configured per application context.
 *
 * The {@link MediacenterServiceFactory} can be obtained as a Spring bean from the application context.
 */
public interface MediacenterServiceFactory extends AppContextServiceFactory<MediacenterService> {

    static MediacenterService getInstance() throws NotAnAdminException {
		if(!AuthorityServiceFactory.getInstance().getLocalService().isGlobalAdmin()){
			throw new NotAnAdminException();
		}
		return ApplicationContextFactory.getApplicationContext().getBean(MediacenterService.class);
	}
}
