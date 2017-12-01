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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.mail.EmailException;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.service.usage.AlfServicesWrapper;
import org.edu_sharing.webservices.authentication.Authentication;
import org.edu_sharing.webservices.authentication.AuthenticationServiceLocator;
import org.springframework.context.ApplicationContext;



public class AuthMethodTrustedRepository implements AuthMethodInterface {
	Logger logger = Logger.getLogger(AuthMethodTrustedRepository.class);

	private MutableAuthenticationDao authenticationDao;

	private PersonService personService;

	private NodeService nodeService;

	private SearchService searchService;

	private ServiceRegistry serviceRegistry;

	private AuthenticationService authenticationService;
	
	
	/**
	 * get Userdata from ldap or something
	 */
	private UserDataService userDataService;

	public static String PARAM_APPLICATIONID = "PARAM_APPLICATIONID";

	public static String PARAM_USERNAME = "PARAM_USERNAME";

	public static String PARAM_EMAIL = "PARAM_EMAIL";

	public static String PARAM_TICKET = "PARAM_TICKET";

	public static String PARAM_CREATEUSER = "PARAM_CREATEUSER";

	public static String PARAM_CLIENTHOST = "PARAM_CLIENTHOST";
	
	public static String test = null;
	
	
	public void init(){
		
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		
		this.authenticationDao = (MutableAuthenticationDao)applicationContext.getBean("authenticationDao");
	
		this.serviceRegistry = (ServiceRegistry) applicationContext.getBean("ServiceRegistry");
		
		this.personService = this.serviceRegistry.getPersonService();

		this.nodeService = this.serviceRegistry.getNodeService();

		this.searchService = this.serviceRegistry.getSearchService();

		this.authenticationService = this.serviceRegistry.getAuthenticationService();
		

	}

	public String authenticate(HashMap<String, String> params) throws AuthenticationException {
		final String username = (String) params.get(PARAM_USERNAME);
		final String ticket = (String) params.get(PARAM_TICKET);
		final String applicationId = (String) params.get(PARAM_APPLICATIONID);

		try {
			
			
			final ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
			authenticationService.authenticate(homeRepository.getUsername(), homeRepository.getPassword().toCharArray());
			
			final ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(applicationId);
			if (appInfo == null || appInfo.getTrustedclient() == null || !appInfo.getTrustedclient().equals("true")) {
				logger.info(AuthenticationExceptionMessages.INVALID_APPLICATION +" "+appInfo);
				throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_APPLICATION);
			}
			String authenticationWebservicesurl = appInfo.getAuthenticationwebservice();
			String clientHost = (String) params.get(PARAM_CLIENTHOST);
			// String wsendpoint
			String email = (String) params.get(PARAM_EMAIL);

			boolean createUser = new Boolean(params.get(PARAM_CREATEUSER));
			
			if(email == null || email.trim().length() == 0 || ticket == null || ticket.trim().length() == 0 || applicationId == null || applicationId.trim().length() == 0 || username == null || username.trim().length() == 0){
				logger.error(AuthenticationExceptionMessages.MISSING_PARAM);
				logger.error("email:"+email +" ticket:"+ticket +" applicationId:"+applicationId +" username:"+username +" ( clientHost:"+clientHost+")");
				throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
			}else{
				email = email.trim().toLowerCase();
			}

			if (username != null && username.trim().length() > 0 && ticket != null && ticket.length() > 0 && authenticationWebservicesurl != null && authenticationWebservicesurl.trim().length() > 0
					&& email != null && email.trim().length() > 0) {

				// überprüfe ob es ein valider Host ist

				//if (clientHost == null || !clientHost.equals(appInfo.getHost())) {
				if (clientHost == null || !appInfo.isTrustedHost(clientHost)) {	
					logger.error(AuthenticationExceptionMessages.INVALID_HOST + " clientHost:" + clientHost + " appInfo.trusted hosts:" + appInfo.getHost() +" "+ appInfo.getHostAliases() +" "+appInfo.getDomain() +" appInfo.getAppId():"+appInfo.getAppId() +" appfile:"+appInfo.getAppFile() +" param appid:"+applicationId);
					throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_HOST);
				}

				// überprüfe ob session der client Anwendung gültig ist
				logger.info("clientAuthWSUrl:" + authenticationWebservicesurl);
				boolean validSession = false;
				try {
					Authentication ccAuthService = getAuthenticationServiceByEndpointAddress(authenticationWebservicesurl);
					validSession = ccAuthService.checkTicket(username, ticket);
				} catch (RemoteException e) {
					logger.error("clientAuthWSUrl:"+authenticationWebservicesurl);
					logger.error(AuthenticationExceptionMessages.CHECKTICKETSERVICE_NOTREACHABLE);
					e.printStackTrace();
					throw new AuthenticationException(AuthenticationExceptionMessages.CHECKTICKETSERVICE_NOTREACHABLE, e.getCause());
				} catch (Throwable e) {
					logger.error("clientAuthWSUrl:"+authenticationWebservicesurl);
					e.printStackTrace();
					throw new AuthenticationException(e.getMessage(), e);
				}

				if (!validSession) {
					logger.info(AuthenticationExceptionMessages.INVALID_CLIENT_SESSION);
					throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_CLIENT_SESSION);
				}

				// suche user lokal

				StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
				
				
				
				//default: use mail as username cause its the only possibility to validate that the user is the one he pretends to be
				//in an environment with distributed applications where is no central authentication point
				String repositoryUsername = email;
				
				//app has trusted email configured and user comes with this email set repositoryUsername to appUsername
				String trustedEmailAddress = appInfo.getTrustedEmailAddress();
				if( (trustedEmailAddress != null && trustedEmailAddress.equals(email)) || appInfo.getAuthByAppUsernameProp().equals(ApplicationInfo.AUTHBYAPP_USERNAME_PROP_USERNAME) ){
					
					logger.info("authentication with username:"+username+"( email:"+email+", trustedemail:"+trustedEmailAddress +" AuthByAppUsernameProp:"+appInfo.getAuthByAppUsernameProp()+") ");
					repositoryUsername = username;
				}

				//allow mapping from username param to ldap prop
				if(appInfo.getAuthByAppUsernameMappingDirectoryUsername() != null && appInfo.getAuthByAppUsernameMappingRepositoryUsername() != null && !(trustedEmailAddress != null && trustedEmailAddress.equals(email)) && userDataService != null){
					Map<String, String> result = userDataService.getDirectoryUserProperties(appInfo.getAuthByAppUsernameMappingDirectoryUsername(), repositoryUsername, new String[]{ appInfo.getAuthByAppUsernameMappingRepositoryUsername() });
					if(result != null && result.size() > 0){
						String userDirectoryUn = result.get(appInfo.getAuthByAppUsernameMappingRepositoryUsername());
						if(userDirectoryUn != null && !userDirectoryUn.trim().equals("")){
							repositoryUsername = userDirectoryUn;
						}else{
							logger.error("could not find an entry in userdirectory for "+repositoryUsername +" cause userDirectoryUn is empty or null");
						}
					}else{
						logger.error("could not find an entry in userdirectory for "+repositoryUsername);
					}
				}
				
				
				final String repositoryUsername_as_final = repositoryUsername;
				final String email_as_final = email;
				
				NodeRef tmpUserNodeRef = null; 
				try{
					tmpUserNodeRef = personService.getPerson(repositoryUsername_as_final, false);
				}catch(NoSuchPersonException e){
					
				}
				
				final NodeRef userNodeRef = tmpUserNodeRef;
				
				String accesskey = new KeyTool().getKey();
				// user gefunden
				if (userNodeRef != null) {
					
					logger.info("found user with repsitoryusername:" + repositoryUsername_as_final);
										
					RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>() {
						public Object execute() throws Exception {
							List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(userNodeRef, QName.createQName(CCConstants.CM_ASSOC_PERSON_ACCESSLIST),
									RegexQNamePattern.MATCH_ALL);
							HashMap<String, Object> result = new HashMap<String, Object>();
							for (ChildAssociationRef childAssocRef : childAssocRefs) {
								Map<QName, Serializable> properties = nodeService.getProperties(childAssocRef.getChildRef());
								String propAppId = (String) properties.get(QName.createQName(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCAPPID));
								String propUserName = (String) properties.get(QName.createQName(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCUSERID));
								if (propAppId != null && propAppId.equals(applicationId) && propUserName != null && propUserName.equals(username)) {

									result.put("APPLICATIONID", propAppId);
									result.put("USERNAME", propUserName);
									result.put("ACCESS", properties.get(QName.createQName(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACCESS)));
									result.put("NODEIDPERSACCESS", childAssocRef.getChildRef().getId());
									result.put("NODEREFPERSACCESS", childAssocRef.getChildRef());
									//make sure that when more entries with the correct appId and UserId are there return the one with access true
									if((Boolean)result.get("ACCESS") == true){
										return result;
									}
									 

								}
							}
							return result;
						}
					};

					TransactionService transactionService = serviceRegistry.getTransactionService();
					HashMap<String, Object> persAccessData = (HashMap<String, Object>) transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, true);
					final Boolean tmpAccess = (Boolean) persAccessData.get("ACCESS");
					final String tmpAppId = (String) persAccessData.get("APPLICATIONID");
					final String tmpUserName = (String) persAccessData.get("USERNAME");

					// Application ist freigeschaltet
					// Host ist OK, Client Session ist OK, Client Application
					// wurde freigeschaltet --> anmelden
					if (tmpAppId != null && tmpAppId.equals(applicationId) && tmpAccess != null && tmpAccess.booleanValue() == true && tmpUserName != null
							&& tmpUserName.equals(username)) {
						// email wird als alfresco username genommen die
						// Rückgabe bewirkt das CCAuthenticationComponent den
						// User als authentifiziert erklärt
						logger.info("access for user:" + repositoryUsername_as_final + " appId:" + tmpAppId + " appUSerId:" + tmpUserName + " was allowed!" +" email:"+email+" trustedEmailAddress:"+trustedEmailAddress);
						return repositoryUsername_as_final;
					}
					// Application ist nicht freigeschaltet
					else {

						logger.info("application" + applicationId + " is not activated.creating new PersonAccessElement and sending activation mail");
						
						
						String persAccElementId = null;
						String appType = appInfo.getType();
						
						try {
							
							//app type service
							if(appType.equals(ApplicationInfo.TYPE_RENDERSERVICE)){
								persAccElementId = this.createNewPersonAccessElement(userNodeRef, username, applicationId, true, accesskey);
								logger.info("access for user:"+repositoryUsername_as_final+" email:" + email + " appId:" + tmpAppId + " appUSerId:" + tmpUserName + " was allowed without activating cause it's an service app!");
								return repositoryUsername_as_final;
							//app has trusted email configured and user comes with this email
							}else if( (trustedEmailAddress != null && trustedEmailAddress.equals(email)) || !appInfo.isAuthByAppSendMail()){
								persAccElementId = this.createNewPersonAccessElement(userNodeRef, username, applicationId, true, accesskey);
								logger.info("access for user:"+repositoryUsername_as_final+" email:" + email + " appId:" + tmpAppId + " appUSerId:" + tmpUserName + " was allowed without activating cause it's an trusted_email app or auth_by_app_sendmail is false! trustedEmailAddress:"+trustedEmailAddress);
								//returning username cause trustedEmailAddress is for all App Users 
								return repositoryUsername_as_final;
							}else{
								persAccElementId = this.createNewPersonAccessElement(userNodeRef, username, applicationId, false, accesskey);
							}
						}  catch (Throwable e) {
							e.printStackTrace();
							throw new AuthenticationException(e.getMessage(), e);
						}

						if (persAccElementId != null && !appType.equals(ApplicationInfo.TYPE_RENDERSERVICE) ) {
							this.sendActivationRequestMail(email, applicationId, username, accesskey);
						}
						
						

						throw new AuthenticationException(AuthenticationExceptionMessages.APPLICATIONACCESS_NOT_ACTIVATED_BY_USER);

					}
				} else {
					if (!createUser) {
						logger.info("no user found and createUser == false so returning null");
						throw new AuthenticationException(AuthenticationExceptionMessages.USERNOTFOUND);

					} else {
						logger.info("no user found and createUser == true creating new user with username:" + repositoryUsername_as_final);

						

						if (homeRepository == null) {
							logger.error("missing homerepository config!!!");
							throw new AuthenticationException(AuthenticationExceptionMessages.REPOSITORY_FAULT);
						}

						RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>() {
							public Object execute() throws Exception {
								authenticationDao.createUser(repositoryUsername_as_final, new KeyTool().getRandomPassword().toCharArray());
								
								Map<QName, Serializable> persProps = new HashMap<QName, Serializable>();
								persProps.put(QName.createQName(CCConstants.PROP_USERNAME), repositoryUsername_as_final);
								persProps.put(QName.createQName(CCConstants.PROP_USER_EMAIL), email_as_final);
								personService.setPersonProperties(repositoryUsername_as_final, persProps);
								NodeRef newPersNodeRef = personService.getPerson(repositoryUsername_as_final);
								
								
								logger.info("user created!!!");
								return newPersNodeRef;
							}
						};
						TransactionService transactionService = serviceRegistry.getTransactionService();
						NodeRef newPersNodeRef = (NodeRef) transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
						
						//do some userdata updating from ldap or sth
						if(userDataService != null){
							userDataService.updateUser(repositoryUsername_as_final, repositoryUsername_as_final);
						}
						
						String persAccElementId = null;
						try {
							
							 if( (trustedEmailAddress != null && trustedEmailAddress.equals(email))  || !appInfo.isAuthByAppSendMail()){
									this.createNewPersonAccessElement(newPersNodeRef, username, applicationId, true, accesskey);
									logger.info("access for new user:"+repositoryUsername_as_final+" email:" + email + " appId:" + applicationId + " appUSerId:" + username + " was allowed without activating cause it's an trusted_email app or auth_by_app_sendmail is false! trustedEmailAddress:"+trustedEmailAddress);
									//returning username cause trustedEmailAddress is for all App Users 
									return repositoryUsername_as_final;
							 }else{
							
								 persAccElementId = this.createNewPersonAccessElement(newPersNodeRef, username, applicationId, false, accesskey);
							 }
						}catch (Throwable e) {
							e.printStackTrace();
							throw new AuthenticationException(e.getMessage(), e);
						}
						if (persAccElementId != null) {
							this.sendActivationRequestMail(email, applicationId, username, accesskey);
						}

						throw new AuthenticationException(AuthenticationExceptionMessages.SENDACTIVATIONLINK_SUCCESS);
					}
				}

				// nodeService.
				// nodeService.createNode(arg0, arg1, arg2, arg3, arg4)

				// nodeService.createNode( nodeIO, QName.createQName(
				// Educational.LOM_ASSOCIATION ), QName.createQName(
				// Educational.LOM_TYPE ), QName.createQName(
				// Educational.LOM_TYPE ), eduProps );

				// wenn vorhanden schau nach ob ClientApplication für den
				// Zugriff freigeschaltet ist
				// wenn freigeschaltet authentifiziere und gebe Ticket zurück
				// wenn nicht freigeschaltet verschicke Mail mit Aufforderung
				// zur Bestätigung und Werfe Exception die über den Zustand
				// Informiert
				// wenn nicht vorhanden + createUser == true
				// erzeuge User verschicke Mail mit Aufforderung zur Bestätigung
				// und Werfe Exception die über den Zustand Informiert

			}

		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			if (e instanceof AuthenticationException) {
				throw (AuthenticationException) e;
			} else {
				throw new AuthenticationException(e.getMessage(), e);
			}
		}finally{
			//make admin session invalid
			authenticationService.invalidateTicket(authenticationService.getCurrentTicket());
			authenticationService.clearCurrentSecurityContext();
		}

		return null;
	}

	private void sendActivationRequestMail(String receivermail, String applicationId, String username, String accesskey) {
		logger.info("start sending ActivationReauestMail...");
		
		try{
			new AlfServicesWrapper().sendActivationRequestMail(receivermail, applicationId, username, accesskey);
		}
		catch (EmailException e) {
			logger.error("Sending Activation Link failed", e);
			throw new AuthenticationException(AuthenticationExceptionMessages.SENDACTIVATIONLINK_FAILED);
		}
		
		logger.info("... return");
	}

	/**
	 * 
	 * @param personRef
	 * @param username
	 * @param applicationId
	 * @param accessAllowed
	 * @return NodeId of PersonAccessElement
	 */
	private String createNewPersonAccessElement(NodeRef personRef, String username, String applicationId, Boolean accessAllowed, String accesskey) {
		logger.info("starting...");
		ApplicationInfo homerepository = ApplicationInfoList.getHomeRepository();
		if (homerepository == null) {
			logger.error("no home repository found. check Repository configuration!");
			throw new AuthenticationException(AuthenticationExceptionMessages.REPOSITORY_FAULT);

		}
		
		
		AlfServicesWrapper alfServWrapper = new AlfServicesWrapper();
		alfServWrapper.authenticate(homerepository.getUsername(), homerepository.getPassword());
		String persAccEleId = alfServWrapper.createPersonAccessElement(personRef.getId(), username, applicationId, accessAllowed, accesskey);
		
		//make admin session invalid
		authenticationService.invalidateTicket(authenticationService.getCurrentTicket());
		
		if (persAccEleId == null) {
			logger.error("error by creating " + CCConstants.CM_TYPE_PERSONACCESSELEMENT + " ersAccEleId");
			throw new AuthenticationException(AuthenticationExceptionMessages.REPOSITORY_FAULT);
		}
		logger.info("...returning");
		return persAccEleId;

	}


	/**
	 * builds shadowUsername with pattern:
	 * {username}#{emailaddress}#{repositoryid}
	 * 
	 * @param username
	 * @param email
	 * @param repId
	 * @return
	 */
	private String getShadowUN(String username, String email, String repId) {
		return username + "#" + email + "#" + repId;
	}

	public void setAuthenticationDao(MutableAuthenticationDao authenticationDao) {
		this.authenticationDao = authenticationDao;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
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
	
	
	
}
