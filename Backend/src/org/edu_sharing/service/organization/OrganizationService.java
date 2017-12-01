package org.edu_sharing.service.organization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.restservices.DAOException;

public interface OrganizationService {

	String createOrganization(String orgName, String groupDisplayName) throws Throwable;
}
