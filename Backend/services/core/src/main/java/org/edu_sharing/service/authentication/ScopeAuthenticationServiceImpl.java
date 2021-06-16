package org.edu_sharing.service.authentication;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

public class ScopeAuthenticationServiceImpl extends ScopeAuthenticationServiceAbstract{

	
	
	@Override
	public boolean checkScope(String username, String scope) {
		return AuthenticationUtil.runAs(new RunAsWork<Boolean>() {
			@Override
			public Boolean doWork() throws Exception {
				ToolPermissionService toolPermissionService = ToolPermissionServiceFactory.getInstance();
				if(toolPermissionService.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL)){
					return true;
				}else{
					return false;
				}
			}
		}, username);
	}

	@Override
	public int getSessionTimeout() {
		return 10*60;
	}
}
