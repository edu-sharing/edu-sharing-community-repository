package org.edu_sharing.service.authentication;

import org.alfresco.service.cmr.repository.NodeRef;

public interface ScopeUserHomeService {
	
	public NodeRef getUserHome(String username, String scope, boolean createIfNotExists);
	
	public void setManageEduGroupFolders(boolean manageEduGroupFolders);
	

}
