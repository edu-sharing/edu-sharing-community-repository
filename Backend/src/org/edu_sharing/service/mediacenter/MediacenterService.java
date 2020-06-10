package org.edu_sharing.service.mediacenter;

import java.io.InputStream;

public interface MediacenterService {
	
	public int importMediacenters(InputStream csv);

	public int importOrganisations(InputStream csv);

	public int importOrgMcConnections(InputStream csv, boolean removeSchoolsFromMC);

	/**
	 * adjustment of licenses for mediacenters on nodes given by an {@link MediacenterLicenseProvider}
	 */
	public void manageNodeLicenses();

	public boolean isActive(String authorityName);

	public void setActive(boolean active, String authorityName);

	public String getMediacenterAdminGroup(String authorityName);

	public void isAllowedToManage(String authorityName);
}
