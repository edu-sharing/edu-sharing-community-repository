package org.edu_sharing.service.bulk;

import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;

public class BulkServiceFactory {

	public static BulkService getInstance() throws NotAnAdminException{
		if(!AuthorityServiceFactory.getLocalService().isGlobalAdmin()){
			throw new NotAnAdminException();
		}
		return new BulkServiceImpl();
	}
}
