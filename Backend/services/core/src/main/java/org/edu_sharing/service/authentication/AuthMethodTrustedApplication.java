package org.edu_sharing.service.authentication;

import java.rmi.RemoteException;
import java.util.HashMap;

import javax.xml.rpc.ServiceException;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.webservices.authentication.Authentication;
import org.edu_sharing.webservices.authentication.AuthenticationServiceLocator;

public class AuthMethodTrustedApplication implements AuthMethodInterface {

	Logger logger = Logger.getLogger(AuthMethodTrustedApplication.class);
	
	
	SSOAuthorityMapper ssoAuthorityMapper;
	
	public String authenticate(HashMap<String, String> params) throws AuthenticationException {
		
		String userName = params.get(ssoAuthorityMapper.getSSOUsernameProp());
		
		String applicationId = params.get(SSOAuthorityMapper.PARAM_APP_ID);
		String clientIp = params.get(SSOAuthorityMapper.PARAM_APP_IP);
		
		/**
		 * check params
		 */
		if(applicationId == null || applicationId.trim().length() == 0 || userName == null || userName.trim().length() == 0){
			logger.error(AuthenticationExceptionMessages.MISSING_PARAM);
			logger.error(" username:"+userName +" applicationId:"+applicationId +" ( clientIp:"+clientIp+")");
			throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
		}
		
		/**
		 * check applicationId
		 */
		final ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(applicationId);
		if (appInfo == null || appInfo.getTrustedclient() == null || !appInfo.getTrustedclient().equals("true")) {
			logger.info(AuthenticationExceptionMessages.INVALID_APPLICATION +" "+appInfo);
			throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_APPLICATION);
		}
		

		/**
		 * check host
		 */	
		if (clientIp == null || !appInfo.isTrustedHost(clientIp)) {	
			logger.error(AuthenticationExceptionMessages.INVALID_HOST + " clientHost:" + clientIp + " appInfo.trusted hosts:" + appInfo.getHost() +" "+ appInfo.getHostAliases() +" "+appInfo.getDomain() +" appInfo.getAppId():"+appInfo.getAppId() +" appfile:"+appInfo.getAppFile() +" param appid:"+applicationId);
			throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_HOST);
		}
		
		
		
		params.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_AuthByApp);
		return ssoAuthorityMapper.mapAuthority(params);
	}
	
	
	
	/**
	 * 
	 * we can not use EduWebServiceFactory cause here its deployed in edu-sharing webapp context, 
	 * and this class is deployed in alfreco context
	 * 
	 * so this is a copy from EduWebServiceFactory.  
	 * @param endpointAddress
	 * @return
	 */
	public Authentication getAuthenticationServiceByEndpointAddress(String endpointAddress){
		try{
			AuthenticationServiceLocator locator = new AuthenticationServiceLocator();
			locator.setauthenticationEndpointAddress(endpointAddress);
			return locator.getauthentication();
		}catch(ServiceException e){
			//e.printStackTrace();
			logger.error(e.getMessage(), e);
			
		}
		return null;
	}
	
	public void setSsoAuthorityMapper(SSOAuthorityMapper ssoAuthorityMapper) {
		this.ssoAuthorityMapper = ssoAuthorityMapper;
	}
}
