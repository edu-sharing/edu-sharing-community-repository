package org.edu_sharing.service.license;

import java.util.List;

import org.edu_sharing.repository.client.rpc.AssignedLicense;

public class AssignedLicenseService {

	
	AssignedLicenseDAO assignedLicenseDAO = new AssignedLicenseDAOSubTypeImpl();
	
	public List<AssignedLicense> getAssignedLicenses(String repositoryId, String nodeId) throws Throwable{
		return assignedLicenseDAO.getAssignedLicenses(repositoryId, nodeId);
	}
	
	public void setAssignedLicenses(String repositoryId, String nodeId, List<AssignedLicense> assignedLicenses) throws Throwable{
		assignedLicenseDAO.setAssignedLicenses(repositoryId, nodeId, assignedLicenses);
	}
}
