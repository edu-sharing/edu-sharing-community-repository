package org.edu_sharing.service.organization;

import com.drew.lang.annotations.NotNull;
import org.alfresco.service.namespace.QName;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public interface OrganizationService {

	String createOrganization(String orgName, String groupDisplayName) throws Throwable;

	String createOrganization(String orgName, String groupDisplayName, String metadataSet, String scope) throws Throwable;

	Map<QName, Serializable> getOrganisation(String orgName);

    @NotNull
    Set<String> getAssignedOrganisations(@NotNull String userName);

    @NotNull
    Set<String> getAssignedOrganisations();
}
