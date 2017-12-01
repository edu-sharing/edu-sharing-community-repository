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
package org.edu_sharing.repository.server.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.webservice.authentication.AuthenticationFault;
import org.apache.axis.AxisFault;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.webservices.authbyapp.AuthByApp;
import org.edu_sharing.webservices.authbyapp.AuthByAppServiceLocator;
import org.edu_sharing.webservices.authentication.AuthenticationException;
import org.edu_sharing.webservices.authentication.AuthenticationResult;
import org.edu_sharing.webservices.types.KeyValue;
import org.edu_sharing.webservices.util.AuthenticationDetails;
import org.edu_sharing.webservices.util.AuthenticationUtils;
import org.springframework.context.ApplicationContext;

public class AuthenticatorRemoteRepository {
	
	private static Log logger = LogFactory.getLog(AuthenticatorRemoteRepository.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
	
	SSOAuthorityMapper ssoAuthorityMapper = (SSOAuthorityMapper)eduApplicationContext.getBean("ssoAuthorityMapper");
	
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	/**
	 * authenticates at remote app with actual local userdata, if fails an guest ticket and the exception message will be returned 
	 * @param localAuthInfo
	 * @param remoteAppInfo
	 * @param createRemoteUser
	 * @return AuthenticatorRemoteAppResult
	 */
	public AuthenticatorRemoteAppResult getAuthInfoForApp(HashMap<String,String> localAuthInfo, ApplicationInfo remoteAppInfo) throws Throwable{
		
		HashMap<String,String> resultAuthInfo = new HashMap<String,String>();
		
		MCAlfrescoBaseClient mcAlfrescoBaseClient =	(MCAlfrescoBaseClient)RepoFactory.getInstance(null, localAuthInfo);
		
		logger.info("getting userinfo for" + localAuthInfo.get(CCConstants.AUTH_USERNAME));
		HashMap<String,String> userInfo = mcAlfrescoBaseClient.getUserInfo(localAuthInfo.get(CCConstants.AUTH_USERNAME));
		String exceptionMessage = null;
		try{
			logger.info("starting session with remoteAppId:"+remoteAppInfo.getAppId()+ " local usename:"+localAuthInfo.get(CCConstants.AUTH_USERNAME)+ " propUserMail:"+userInfo.get(CCConstants.PROP_USER_EMAIL) +" Ticket:"+localAuthInfo.get(CCConstants.AUTH_TICKET));
			if(remoteAppInfo.getAuthenticationwebservice() != null && !remoteAppInfo.getAuthenticationwebservice().trim().equals("")){
				
				//AuthenticationUtil.startSession(remoteAppInfo.getAppId(), localAuthInfo.get(CCConstants.AUTH_USERNAME) , userInfo.get(CCConstants.PROP_USER_EMAIL), localAuthInfo.get(CCConstants.AUTH_TICKET), createRemoteUser);
				remoteAuth(remoteAppInfo.getAppId(),localAuthInfo.get(CCConstants.AUTH_USERNAME),localAuthInfo.get(CCConstants.AUTH_TICKET),(MCAlfrescoAPIClient)mcAlfrescoBaseClient);
			
			}else{
				return null;
			}
		}catch(AxisFault e){
			
			logger.info("REMOTE REPOSITORY AUTH FAILED"+e.getMessage());
			exceptionMessage = e.getMessage();
			
			//don't login as guest better throw exception so that we can inform the user that an email was send
			throw e;
			
		}
		//TODO if exception repository unreachable -> special handling
		logger.info("REMOTE APPID:"+ remoteAppInfo.getAppId() +"REMOTE USERNAME:"+AuthenticationUtils.getAuthenticationDetails().getUserName()+" REMOTETICKET:"+AuthenticationUtils.getAuthenticationDetails().getTicket()) ;
		resultAuthInfo.put(CCConstants.AUTH_USERNAME,AuthenticationUtils.getAuthenticationDetails().getUserName());
		resultAuthInfo.put(CCConstants.AUTH_TICKET,AuthenticationUtils.getAuthenticationDetails().getTicket());
		AuthenticatorRemoteAppResult result = new AuthenticatorRemoteAppResult();
		result.setAuthenticationInfo(resultAuthInfo);
		result.setExceptionMessage(exceptionMessage);
		logger.info("REMOTE USERNAME2:"+resultAuthInfo.get(CCConstants.AUTH_USERNAME)+" REMOTETICKET:"+resultAuthInfo.get(CCConstants.AUTH_TICKET)) ;
		return result;
	}
	
	private void remoteAuth(String appId, String username, String localTicket, MCAlfrescoAPIClient apiClient) throws Exception{
		String localAppId = ApplicationInfoList.getHomeRepository().getAppId();
    	System.out.println("startSession remoteApplicationId:"+appId +" localAppId:"+localAppId);
    	
    	ApplicationInfo appInfoRemoteApp = ApplicationInfoList.getRepositoryInfoById(appId);
    	
    	HashMap<String,String> personMapping = ssoAuthorityMapper.getMappingConfig().getPersonMapping();
    	
    	List<KeyValue> ssoData = new ArrayList<KeyValue>();
    	
    	HashMap<String,String> personData = apiClient.getUserInfo(username);
    
    	String esuid = personData.get(CCConstants.PROP_USER_ESUID);
	if(esuid == null || esuid.trim().equals("")){
		throw new Exception("missing esuid for user!!!");
	}
    	
    	for(Map.Entry<String, String> entry : personMapping.entrySet()){
    		
    		String val = personData.get(entry.getValue());
    		
    		/**
    		 * add an domain with appid to esuid to prevent interference with an existing user in remote repo
    		 */
    		if(entry.getValue().equals(CCConstants.CM_PROP_PERSON_USERNAME)){
    			val = esuid + "@" + localAppId;	
    		}
    		ssoData.add(new KeyValue(entry.getKey(), val));
    	}
    	
    	//add global groups
    	
    	String globalGroups = null;
    	
    	AuthorityService authorityService = serviceRegistry.getAuthorityService();
    	logger.info(serviceRegistry.getAuthenticationService().getCurrentUserName());
    	Set<String> authoritiesForUser = authorityService.getAuthorities();
    	for(String authority : authoritiesForUser){
    		logger.info("authority:"+authority);
    		NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authority);
    		//i.i. noderef for GROUP_EVERYONE is null
    		if(authorityNodeRef == null) continue;
    		String scopeType = (String)serviceRegistry.getNodeService().getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE));
			
			if(CCConstants.CCM_VALUE_SCOPETYPE_GLOBAL.equals(scopeType)){
				if(globalGroups == null){
					globalGroups = authority;
				}else{
					globalGroups += ";"+authority;
				}
			}
    	}
    	
    	if(globalGroups != null){
    		ssoData.add(new KeyValue(CCConstants.EDU_SHARING_GLOBAL_GROUPS, globalGroups));
    	}
    	
		try{
			
			AuthByAppServiceLocator locator = new AuthByAppServiceLocator();
			
			locator.setauthbyappEndpointAddress(appInfoRemoteApp.getWebServiceHotUrl()+"/authbyapp");
			
			AuthByApp stub = locator.getauthbyapp();
			
			String timestamp = ""+System.currentTimeMillis();
			
			//sign essuid so that man in the middle can not change webservice data, essuid must be tested on serverside
			String signData = esuid+"localAppId"+timestamp;
			
			Signing signing = new Signing();
			
			byte[] signature = signing.sign(signing.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM), signData, CCConstants.SECURITY_SIGN_ALGORITHM);
			signature = new Base64().encode(signature);
			
			((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","timestamp",timestamp));
			((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","appId",localAppId));
			((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signature",new String(signature)));
			((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signed",signData));
			
			AuthenticationResult authResult = stub.authenticateByTrustedApp(localAppId, ssoData.toArray(new KeyValue[ssoData.size()]));
			AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(authResult.getUsername(), authResult.getTicket(),null));
		
		}catch(AuthenticationException e){
			
			System.out.println("EXCEPTION CLASS:"+e.getClass());
			AuthenticationFault authFault = new AuthenticationFault();
			authFault.addFaultDetail(javax.xml.namespace.QName.valueOf(CCConstants.CC_EXCEPTIONPARAM_REPOSITORY_CAPTION), appInfoRemoteApp.getAppCaption());
			authFault.setMessage1(((AuthenticationException)e).getMessage1());
			throw authFault; 
			
		} catch(Exception e){
			
			System.out.println("EXCEPTION CLASS:"+e.getClass());
			e.printStackTrace();
			AuthenticationFault authFault = new AuthenticationFault();
			authFault.setMessage1(e.getMessage());
			throw authFault; 
		}
		
	}
	
}
