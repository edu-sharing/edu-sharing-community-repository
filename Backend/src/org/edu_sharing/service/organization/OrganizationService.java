package org.edu_sharing.service.organization;

import org.alfresco.service.namespace.QName;

import java.io.Serializable;
import java.util.Map;

public interface OrganizationService {

	String createOrganization(String orgName, String groupDisplayName) throws Throwable;

	String createOrganization(String orgName, String groupDisplayName, String metadataSet, String scope) throws Throwable;

	Map<QName, Serializable> getOrganisation(String orgName);
}
