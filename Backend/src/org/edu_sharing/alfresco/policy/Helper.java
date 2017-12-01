package org.edu_sharing.alfresco.policy;

import java.util.Set;

import org.alfresco.service.cmr.security.AuthorityService;

public class Helper {

	AuthorityService authorityService;
	public Helper(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
	
	public boolean isAdmin(String username){
		//boolean result = false;
		try{
			Set<String> testUsetAuthorities = authorityService.getAuthoritiesForUser(username);
			for(String testAuth:testUsetAuthorities){
				
				if(testAuth.equals("GROUP_ALFRESCO_ADMINISTRATORS")){
					
					return true;
				}
			}
		}catch(org.alfresco.repo.security.permissions.AccessDeniedException e){
			//username+" is no admin!!!");
		}
		
		
		return false;
	}
	
}
