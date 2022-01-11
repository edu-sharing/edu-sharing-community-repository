package org.edu_sharing.service.organization;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.springframework.context.ApplicationContext;

public class OrganizationServiceImpl implements OrganizationService {

	Logger logger = Logger.getLogger(OrganizationServiceImpl.class);

	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
	MCAlfrescoAPIClient baseClient = new MCAlfrescoAPIClient();
	ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService node = NodeServiceFactory.getLocalService();
	AuthorityService authorityService = AuthorityServiceFactory.getLocalService();
	
	org.edu_sharing.alfresco.service.OrganisationService eduOrganisationService = (org.edu_sharing.alfresco.service.OrganisationService) alfApplicationContext.getBean("eduOrganisationService");
	
	@Override
	public String createOrganization(String orgName, String groupDisplayName) throws Throwable {
		return this.createOrganization(orgName, groupDisplayName, null, null);
	}

	@Override
	public String createOrganization(String orgName, String groupDisplayName, String metadataSet, String scope) throws Throwable {
			return (String)baseClient.doInTransaction(new RetryingTransactionCallback<String>() {

				@Override
				public String execute() throws Throwable {			
					String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
					if (!baseClient.isAdmin(currentUser) && !AuthenticationUtil.isRunAsUserTheSystemUser()) {
						throw new AccessDeniedException(currentUser);
					}
					
					try {
						return eduOrganisationService.createOrganization(orgName, groupDisplayName, metadataSet, scope);
					}catch(Throwable e) {
						logger.error(e.getMessage(),e);
						throw e;
					}
				}
			});							
	}

	@Override
	public Map<QName, Serializable> getOrganisation(String orgName) {
		return eduOrganisationService.getOrganisation(orgName);
	}

}
