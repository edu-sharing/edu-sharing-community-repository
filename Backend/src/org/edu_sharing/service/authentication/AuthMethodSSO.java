/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.service.authentication;

import java.util.Arrays;
import java.util.HashMap;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;


/**
 * ensure that the authentication method of this class isn't provided through an webservice or something 
 * @author rudi
 *
 */
public class AuthMethodSSO implements AuthMethodInterface {

	
	Logger logger = Logger.getLogger(AuthMethodSSO.class);
	
	private UserDataService userdataService;
	
	private SSOAuthorityMapper ssoAuthorityMapper;
	
	public Boolean createUser = false;
	
	
	public static final String[] allowedAuthTypes = {SSOAuthorityMapper.SSO_TYPE_Shibboleth, SSOAuthorityMapper.SSO_TYPE_CAS}; 
	
	@Override
	public String authenticate(HashMap<String, String> params) throws AuthenticationException {
		//for security reasons: shibboleth auth should be explicit activated in homeApplication.properties.xml
		//so that we don't get an open gate when someone forgets to remove the shibboleth servlet from web.xml and don't protects this url
		
		final ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		String allowedAuthTypesConfig = homeRepository.getAllowedAuthenticationTypes();
		
		String paramAuthType = (String)params.get(SSOAuthorityMapper.PARAM_SSO_TYPE);
		
		//wurde authtype konfiguriert und ist es ein erlaubter authtype
		if(allowedAuthTypesConfig != null && allowedAuthTypesConfig.contains(paramAuthType) && Arrays.asList(allowedAuthTypes).contains(paramAuthType)){
			return ssoAuthorityMapper.mapAuthority(params);
		}else{
			logger.error(AuthenticationExceptionMessages.INVALID_AUTHENTICATION_METHOD +" no SSO(shibboleth,cas) auth configured");
			throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_AUTHENTICATION_METHOD);
		}
		
		
	}


	public void setCreateUser(Boolean createUser) {
		this.createUser = createUser;
	}
	
	

	public UserDataService getUserdataService() {
		return userdataService;
	}

	public void setUserdataService(UserDataService userdataService) {
		this.userdataService = userdataService;
	}
	
	public void setSsoAuthorityMapper(SSOAuthorityMapper ssoAuthorityMapper) {
		this.ssoAuthorityMapper = ssoAuthorityMapper;
	}
	
}
