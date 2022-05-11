package org.edu_sharing.service.license;

import java.util.List;

import org.edu_sharing.repository.client.rpc.AssignedLicense;

public interface AssignedLicenseDAO {
	
	public List<AssignedLicense> getAssignedLicenses(String repositoryId, String nodeId) throws Throwable;
	
	public void setAssignedLicenses(String repositoryId, String nodeId, List<AssignedLicense> assignedLicenses) throws Throwable;
	
}
