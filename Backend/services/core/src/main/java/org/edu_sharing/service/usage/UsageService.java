package org.edu_sharing.service.usage;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.authentication.AuthenticationExceptionMessages;
import org.edu_sharing.webservices.permission.Permission;
import org.edu_sharing.webservices.permission.PermissionServiceLocator;
import org.springframework.context.ApplicationContext;

/**
 * do not share instances between requests cause this class uses class variables
 * which are not thread save (unlike local variables)
 * 
 * @author rudi
 */
public class UsageService {
	
	Logger logger = Logger.getLogger(UsageService.class);
	
	ServiceRegistry serviceRegistry = null;
	
	int subtypeIdx = 0;
	int deleteIdx = 1;
	int updateIdx = 2;
	
	/**
	 * see config "repository.lmsCodes"
	 */
	static String[][] lmsCodes = null;
	
	String requestIp = null;
	String appId = null;
	String repositoryTicket = null;
	
	public static final String MISSING_PARAM = "MISSING_PARAM";
	public static final String NO_CCPUBLISH_PERMISSION = "NO_CCPUBLISH_PERMISSION";
	
	UsageDAO usageDao = new AlfServicesWrapper();
	
	public UsageService(String requestIp, String appId, String repositoryTicket) {
		this.requestIp = requestIp;
		this.appId = appId;
		this.repositoryTicket = repositoryTicket;
		ApplicationContext applicationContext =  AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	}
	
	public UsageService() {
	}
	
	public Usage getUsage(String courseId, String parentNodeId, String appUser, String resourceId) throws AuthenticationException, UsageException{
		isAccessAllowed();
		
		Usage result = null;
		
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		serviceRegistry.getAuthenticationService().authenticate(homeRepository.getUsername(), homeRepository.getPassword().toCharArray());
		HashMap<String, Object> usage = null;
		try{
			usage = usageDao.getUsage(appId, courseId, parentNodeId, resourceId);
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new UsageException(e.getMessage());
		}
		
		if (usage != null) {
			result = getUsageResult(usage);
		}
		
		//make admin session invalid
		serviceRegistry.getAuthenticationService().invalidateTicket(serviceRegistry.getAuthenticationService().getCurrentTicket());
		serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
		logger.info("return");
		return result;
	}
	
	public ArrayList<Usage> getUsageByParentNodeId(java.lang.String parentNodeId) throws AuthenticationException{
		
		logger.info("starting");
		isAccessAllowed();
		
		//admin Authentication 
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
	    
		serviceRegistry.getAuthenticationService().authenticate(homeRepository.getUsername(), homeRepository.getPassword().toCharArray());
			
		HashMap<String, HashMap<String, Object>> usages = usageDao.getUsages(parentNodeId);
		logger.info("usages.keySet().size():"+usages.keySet().size());
		
		ArrayList<Usage> result = new ArrayList<Usage>();
		
		for (String key : usages.keySet()) {
			result.add(getUsageResult(usages.get(key)));
		}
		
		//make admin session invalid
		serviceRegistry.getAuthenticationService().invalidateTicket(serviceRegistry.getAuthenticationService().getCurrentTicket());
		serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
		logger.info("return");		
		return result;
	}
	
	public boolean deleteUsage(java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String resourceId) throws AuthenticationException, UsageException{
		
		logger.info("starting");
		isAccessAllowed();
		
		ApplicationInfo lmsAppInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		if(lmsAppInfo == null){	
			throw new UsageException("lmsId:"+ this.appId + " does not exist!!!");
		}
		
		String permissionWebservice = lmsAppInfo.getPermissionwebservice();
		if(permissionWebservice == null){
			throw new UsageException("permissionWebservice location is null");
		}
		
		//ask the app if operation is allowed
		PermissionServiceLocator permServLocator = new PermissionServiceLocator();
		permServLocator.setpermissionEndpointAddress(permissionWebservice);
		try{
			
			Permission ccpermission = permServLocator.getpermission();
			String updatecode = null;
			for(String[] lmsrow: getLMSCodes()){
				if(lmsrow[subtypeIdx].equals(lmsAppInfo.getSubtype())){
					updatecode = lmsrow[updateIdx]; 
				}
			}
			logger.info("calling ccpermission.getPermission updatecode:"+updatecode+" resourceId:"+resourceId +" courseId:"+courseId+" appSessionId:"+appSessionId);
			boolean deleteAllowed = ccpermission.getPermission(appSessionId, Integer.parseInt(courseId), updatecode, resourceId);
			if(!deleteAllowed){
				
				logger.info("deleteUsages is not allowed for params: repositoryTicket:"+repositoryTicket+ "appSessionId:"+appSessionId +" appCurrentUserId:"+appCurrentUserId +" lmsId:"+appId+" courseId:"+courseId);
				throw new UsageException("deleteUsages is not allowed for params: repositoryTicket:"+repositoryTicket+ "appSessionId:"+appSessionId +" appCurrentUserId:"+appCurrentUserId +" lmsId:"+appId+" courseId:"+courseId);
			}
		}catch(ServiceException e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		}catch(RemoteException e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		}
		
		//admin Authentication 
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		serviceRegistry.getAuthenticationService().authenticate(homeRepository.getUsername(), homeRepository.getPassword().toCharArray());
		try{
			this.usageDao.removeUsage(this.appId, courseId, parentNodeId, resourceId);
			
			//remove IO from cache so that the gui gets the new usage count
			new RepositoryCache().remove(parentNodeId);
			
			return true;
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		} finally{
			//make admin session invalid
			serviceRegistry.getAuthenticationService().invalidateTicket(serviceRegistry.getAuthenticationService().getCurrentTicket());
			serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
		}
	}
	
	public boolean usageAllowed(String nodeId, String courseId) throws AuthenticationException{
		
		logger.info("starting objectNodeId:"+nodeId +" lmsId:"+this.appId +" courseId:"+courseId+" ####");
		isAccessAllowed();
		
		//admin authentication
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		serviceRegistry.getAuthenticationService().authenticate(homeRepository.getUsername(), homeRepository.getPassword().toCharArray());
	
		boolean result = false;
		HashMap<String, HashMap<String, Object>> usages = usageDao.getUsages(nodeId);
		if (usages != null) {
			for (String key : usages.keySet()) {
				Usage usageResult = getUsageResult(usages.get(key));
				if (usageResult != null) {
					logger.info("usageResult != null usageResult.getParentNodeId():"+usageResult.getParentNodeId() +" usageResult.getLmsId():"+usageResult.getLmsId() +" usageResult.getCourseId():"+usageResult.getCourseId());
					if (nodeId != null && nodeId.equals(usageResult.getParentNodeId()) && this.appId != null && this.appId.equals(usageResult.getLmsId()) && courseId != null
							&& courseId.equals(usageResult.getCourseId())) {
						result = true;
					}
				}
			}
		}
		
		//make admin session invalid
		serviceRegistry.getAuthenticationService().invalidateTicket(serviceRegistry.getAuthenticationService().getCurrentTicket());
		serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
		logger.info("returning" + result);
		return result;
	}
	
	public void setUsage(String courseId, String parentNodeId, String appUser, String appUserMail, Calendar fromUsed, Calendar toUsed, Integer distinctPersons, String version, String resourceId, String xmlParams) throws AuthenticationException, UsageException{
		
	    	//ignore what you get as repositoryUsername, get the username which belongs to the ticket
	    serviceRegistry.getAuthenticationService().validate(repositoryTicket);
	    String repositoryUsername = serviceRegistry.getAuthenticationService().getCurrentUserName();
	    		
	    	logger.info("starting repositoryUsername:"+repositoryUsername+" repositoryTicket:"+repositoryTicket +" parentNodeId:"+parentNodeId);
			
			if(fromUsed != null){
				logger.info("fromUsed timezone:" + fromUsed.getTimeZone() +" getTime:"+ fromUsed.getTime() +" ISO:"+ISO8601DateFormat.format(fromUsed.getTime()));
			}
			if(toUsed != null){
				logger.info("toUsed timezone:" + toUsed.getTimeZone() +" getTime:"+ toUsed.getTime() +" ISO:"+ISO8601DateFormat.format(toUsed.getTime()));
			}
			
			isAccessAllowed();
			if (appUser == null || appUser.trim().equals("") || this.appId == null || this.appId.trim().equals("") || courseId == null || courseId.trim().equals("") || parentNodeId == null
					|| parentNodeId.trim().equals("")) {
				throw new UsageException(MISSING_PARAM);
			}
			
			//admin Authentication 
			ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
			
			serviceRegistry.getAuthenticationService().authenticate(homeRepository.getUsername(), homeRepository.getPassword().toCharArray());
						
			HashMap<String, Object> usage = null;
			
			try{
				
				logger.info("before alfServicesWrapper.hasPermissions");
				
				usage = usageDao.getUsage(this.appId, courseId, parentNodeId, resourceId);
				
				//only check publish permission for new content so that an teacher who modifies the course/wysiwyg can safe changes of permission
				if(usage == null){
				
					boolean hasPublishPerm = ((MCAlfrescoClient)RepoFactory.getInstance(homeRepository.getAppId(), (HashMap)null)).hasPermissions(parentNodeId, repositoryUsername, new String[]{CCConstants.PERMISSION_CC_PUBLISH});
					
					if(!hasPublishPerm){
						throw new UsageException(NO_CCPUBLISH_PERMISSION);
					}
				}

			}catch(Throwable e){
				logger.error(e.getMessage(), e);
				throw new UsageException(e.getMessage());
			}
			
			HashMap<String, Object> properties = new HashMap<String,  Object>();
			
			String guid = null;
			NodeRef personRef = serviceRegistry.getPersonService().getPerson(repositoryUsername);
			if(personRef != null){
				Map<QName, Serializable> personProps = serviceRegistry.getNodeService().getProperties(personRef);
				guid = (String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_GUID));
			}
			
			if(guid == null){
				guid = repositoryUsername;
			}
			
			properties.put(CCConstants.CCM_PROP_USAGE_APPID, this.appId);
			properties.put(CCConstants.CCM_PROP_USAGE_COURSEID, courseId);
			properties.put(CCConstants.CCM_PROP_USAGE_PARENTNODEID, parentNodeId);
			properties.put(CCConstants.CCM_PROP_USAGE_APPUSER, appUser);
			properties.put(CCConstants.CCM_PROP_USAGE_APPUSERMAIL, appUserMail);
			if(fromUsed != null){
				properties.put(CCConstants.CCM_PROP_USAGE_FROM, ISO8601DateFormat.format(fromUsed.getTime()));
			}
			if(toUsed != null){
				properties.put(CCConstants.CCM_PROP_USAGE_TO, ISO8601DateFormat.format(toUsed.getTime()));
			}
			properties.put(CCConstants.CCM_PROP_USAGE_MAXPERSONS, distinctPersons);
			properties.put(CCConstants.CCM_PROP_USAGE_COUNTER, new Integer(1).toString());
			properties.put(CCConstants.CCM_PROP_USAGE_VERSION, version);
			properties.put(CCConstants.CCM_PROP_USAGE_RESSOURCEID, resourceId);
			properties.put(CCConstants.CCM_PROP_USAGE_XMLPARAMS, xmlParams);
			
			if(guid != null){
				logger.info("will safe guid "+guid +" for user "+repositoryUsername);
				properties.put(CCConstants.CCM_PROP_USAGE_GUID, guid);
			}
		
			// if null only set counter @TODO Unique constraint in Schema that
			// prevents bypassing unique with standard alfresco services
			if (usage != null) {
				logger.info("usage != null");
				String counter = (String) usage.get(CCConstants.CCM_PROP_USAGE_COUNTER);
				
				String usageNodeId = (String) usage.get(CCConstants.SYS_PROP_NODE_UID);
				logger.info("usageNodeId:" + usageNodeId);
			
				
				logger.info("before updating usage with props:");
				for(Map.Entry<String, Object> entry: properties.entrySet()){
					logger.info("key:"+entry.getKey() +" val:"+entry.getValue());
				}
				
				usageDao.updateUsage(usageNodeId, properties);
			} else {
				logger.info("usage is null");
				usageDao.createUsage(parentNodeId, properties);
			}
			
			//remove IO from cache so that the gui gets the new usage count
			new RepositoryCache().remove(parentNodeId);
			
			//make admin session invalid
			serviceRegistry.getAuthenticationService().invalidateTicket(serviceRegistry.getAuthenticationService().getCurrentTicket());
			serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
			logger.info("returning");
	}
	
	public boolean deleteUsages(java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String courseId) throws AuthenticationException, UsageException{
		
		logger.info("starting");
		isAccessAllowed();
		
		ApplicationInfo lmsAppInfo = ApplicationInfoList.getRepositoryInfoById(this.appId);
		if(lmsAppInfo == null){		
			throw new UsageException("lmsId:"+this.appId + " does not exist!!!");
		}
		
		String permissionWebservice = lmsAppInfo.getPermissionwebservice();
		if(permissionWebservice == null){
			throw new UsageException("permissionWebservice location is null");
		}
			
		//ask the app if operation is allowed
		PermissionServiceLocator permServLocator = new PermissionServiceLocator();
		permServLocator.setpermissionEndpointAddress(permissionWebservice);
		try{
			Permission ccpermission = permServLocator.getpermission();
			String deletecode = null;
			for(String[] lmsrow: getLMSCodes()){
				if(lmsrow[subtypeIdx].equals(lmsAppInfo.getSubtype())){
					deletecode = lmsrow[deleteIdx]; 
				}
			}
			boolean deleteAllowed = ccpermission.getPermission(appSessionId, Integer.parseInt(courseId), deletecode, "-1");
			if(!deleteAllowed){
				throw new UsageException("deleteUsages is not allowed for params: repositoryTicket:"+repositoryTicket+ " appSessionId:"+appSessionId +" appCurrentUserId:"+appCurrentUserId +" lmsId:"+this.appId+" courseId:"+courseId);
			}
		}catch(ServiceException e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		}catch(RemoteException e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		}
		
		//admin User
		//admin Authentication 
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		serviceRegistry.getAuthenticationService().authenticate(homeRepository.getUsername(), homeRepository.getPassword().toCharArray());
		
		try{
			
			//remember for removing parent io's from cache
			HashMap<String, HashMap<String, Object>> usagesByCourse = usageDao.getUsagesByCourse(this.appId, courseId);
			
			boolean result = usageDao.removeUsages(this.appId, courseId);
			
			for(Map.Entry<String, HashMap<String, Object>> usage : usagesByCourse.entrySet()){
				HashMap<String, Object> usageProps = usage.getValue();
				String ioId = (String)usageProps.get(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID);
				if(ioId != null){
					//remove IO from cache so that the gui gets the new usage count
					new RepositoryCache().remove(ioId);
				}else{
					logger.error("found no primary parent for usage:" +usage.getKey()+" can not remove IO from cache");
				}
			}
			
			return result;
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		}finally{
			//make admin session invalid
			serviceRegistry.getAuthenticationService().invalidateTicket(serviceRegistry.getAuthenticationService().getCurrentTicket());
			serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
		}
	}
	
	
	public Usage getUsageResult(HashMap<String, Object> usage) {
		
		logger.info("starting");
		Usage usageResult = new Usage();
		usageResult.setAppUser((String) usage.get(CCConstants.CCM_PROP_USAGE_APPUSER));
		usageResult.setAppUserMail((String) usage.get(CCConstants.CCM_PROP_USAGE_APPUSERMAIL));
		usageResult.setCourseId((String) usage.get(CCConstants.CCM_PROP_USAGE_COURSEID));
		
		String maxPersonsString = (String) usage.get(CCConstants.CCM_PROP_USAGE_MAXPERSONS);
		if(maxPersonsString != null) usageResult.setDistinctPersons(new Integer(maxPersonsString));
		
		usageResult.setUsageVersion((String) usage.get(CCConstants.CCM_PROP_USAGE_VERSION));
		usageResult.setUsageXmlParams((String) usage.get(CCConstants.CCM_PROP_USAGE_XMLPARAMS));

		Object usageFrom = usage.get(CCConstants.CCM_PROP_USAGE_FROM);
		logger.info("CCM_PROP_USAGE_FROM:" + usageFrom);
		
		if(usageFrom != null){
			Calendar calFrom = Calendar.getInstance();
			
			Date usageFromDate = new DateTool().getDate((String)usageFrom);
			if(usageFromDate != null){
				calFrom.setTime(usageFromDate);
				usageResult.setFromUsed(calFrom);
			}
		}
		usageResult.setLmsId((String) usage.get(CCConstants.CCM_PROP_USAGE_APPID));
		usageResult.setParentNodeId((String) usage.get(CCConstants.CCM_PROP_USAGE_PARENTNODEID));

		usageResult.setNodeId((String) usage.get(CCConstants.SYS_PROP_NODE_UID));
		
		usageResult.setResourceId((String)usage.get(CCConstants.CCM_PROP_USAGE_RESSOURCEID));
		
		usageResult.setGuid((String)usage.get(CCConstants.CCM_PROP_USAGE_GUID));

		Object usageTo = usage.get(CCConstants.CCM_PROP_USAGE_TO);
		if(usageTo != null){
			Calendar calTo = Calendar.getInstance();
			Date usageToDate = new DateTool().getDate((String)usageTo);
			if(usageToDate != null){
				calTo.setTime(usageToDate);
				usageResult.setToUsed(calTo);
			}
		}
		
		Object usageCounter = usage.get(CCConstants.CCM_PROP_USAGE_COUNTER);
		logger.info("CCM_PROP_USAGE_COUNTER:" + usageCounter);
		if (usageCounter != null) {
			usageResult.setUsageCounter(new Integer((String) usageCounter));
		}

		logger.info("returning");
		return usageResult;
	}
	
	 private boolean isAccessAllowed() throws AuthenticationException {
			
		 	ApplicationInfo renderApplicationInfo = null;
			for (Map.Entry<String,ApplicationInfo> appInfo : ApplicationInfoList.getApplicationInfos().entrySet()) {
				if(appInfo.getValue().getType().equals(ApplicationInfo.TYPE_RENDERSERVICE)){
					renderApplicationInfo = appInfo.getValue();
				}
			}
		 
			ApplicationInfo appInfo = null;
			for (String appKey : ApplicationInfoList.getApplicationInfos().keySet()) {
				ApplicationInfo tmpAppInfo = ApplicationInfoList.getApplicationInfos().get(appKey);
				//if (tmpAppInfo != null && new Boolean(tmpAppInfo.getTrustedclient()) && tmpAppInfo.getHost().equals(ipAddress)) {
				if (this.appId.equals(appKey) && tmpAppInfo != null) {
					if(new Boolean(tmpAppInfo.getTrustedclient()) && tmpAppInfo.isTrustedHost(requestIp)){
						appInfo = tmpAppInfo;
					}//if the renderer asks for the lms means aksing server is render server appid is from lms
					else if(renderApplicationInfo != null && renderApplicationInfo.isTrustedHost(requestIp)){
						appInfo = tmpAppInfo;
					}
				}
			}
			
			logger.info("after trusted host check");

			if (appInfo == null && !requestIp.equals("127.0.0.1")) {
				logger.error(AuthenticationExceptionMessages.INVALID_HOST);
				throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_HOST);
			}
			logger.info("after appinfo == null check");

			
			// check Ticket
			try{
				serviceRegistry.getAuthenticationService().validate(repositoryTicket);
			}catch(org.alfresco.repo.security.authentication.AuthenticationException e){
				logger.error(AuthenticationExceptionMessages.INVALID_CLIENTTICKET );
				throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_CLIENTTICKET);
			}
	
			logger.info("returning true");
			return true;
		}
	 
	 static String[][] getLMSCodes(){
		 
		 if(lmsCodes == null){
			 synchronized(UsageService.class){
			 
				 Logger logger = Logger.getLogger(UsageService.class);
				 String propValue = LightbendConfigLoader.get().getString("repository.lmsCodes");
				 if(propValue == null){
					 logger.error("no repository.lmsCodes found in config! can not speak with connect lms");
					 return null;
				 }
				 
				 String[] lmsField = propValue.split(";");
				 if(lmsField.length == 0){
					 logger.error("no value forr epository.lmsCodes found in config! can not speak with connect lms");
					 return null;
				 }
				 
				 lmsCodes = new String[lmsField.length][];
				 for(int i = 0; i < lmsField.length;i++){
					 lmsCodes[i] = lmsField[i].split(",");
					 
					 if(lmsCodes[i] == null || lmsCodes[i].length < 3){
						 logger.error("field does not have the correct length of 3! idx:"+i);
						 return null;
					 }
				 }
			 }
		 }
		 return lmsCodes;
	 }
	
}
