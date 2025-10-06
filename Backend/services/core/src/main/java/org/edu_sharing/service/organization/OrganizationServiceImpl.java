package org.edu_sharing.service.organization;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {


    private final MCAlfrescoAPIClient baseClient = new MCAlfrescoAPIClient();
    private final org.edu_sharing.alfresco.service.OrganisationService eduOrganisationService;
	
	@Override
	public String createOrganization(String orgName, String groupDisplayName) {
		return this.createOrganization(orgName, groupDisplayName, null, null);
	}

	@Override
	public String createOrganization(String orgName, String groupDisplayName, String metadataSet, String scope) {
			return (String)baseClient.doInTransaction(() -> {
                String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
                if (!baseClient.isAdmin(currentUser) && !AuthenticationUtil.isRunAsUserTheSystemUser()) {
                    throw new AccessDeniedException(currentUser);
                }

                try {
                    return eduOrganisationService.createOrganization(orgName, groupDisplayName, metadataSet, scope);
                }catch(Throwable e) {
                    log.error(e.getMessage(),e);
                    throw e;
                }
            });
	}

	@Override
	public Map<QName, Serializable> getOrganisation(String orgName) {
		return eduOrganisationService.getOrganisation(orgName);
	}

    @Override
    public Set<String> getAssignedOrganisations(String userName) {
        return eduOrganisationService.getAssignedOrganisations(userName);
    }

    @Override
    public Set<String> getAssignedOrganisations() {
        return eduOrganisationService.getAssignedOrganisations();
    }

}
