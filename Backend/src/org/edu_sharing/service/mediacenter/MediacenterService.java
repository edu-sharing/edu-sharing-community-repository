package org.edu_sharing.service.mediacenter;

import java.io.InputStream;

public interface MediacenterService {
	
	public int importMediacenters(InputStream csv);

	public int importOrganisations(InputStream csv);

	public int importOrgMcConnections(InputStream csv);


}
