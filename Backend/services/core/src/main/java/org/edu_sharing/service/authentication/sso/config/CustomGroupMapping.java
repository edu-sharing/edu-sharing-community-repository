package org.edu_sharing.service.authentication.sso.config;

import java.util.HashMap;

import org.edu_sharing.service.authentication.SSOAuthorityMapper;

public interface CustomGroupMapping {

	
	public void map(HashMap<String, String> ssoAttributes);
	
	
	public void setSSOAuthorityMapper(SSOAuthorityMapper ssoAuthorityMapper);
}
