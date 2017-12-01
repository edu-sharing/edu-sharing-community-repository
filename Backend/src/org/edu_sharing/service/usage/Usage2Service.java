package org.edu_sharing.service.usage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.springframework.context.ApplicationContext;

public class Usage2Service {
	
Logger logger = Logger.getLogger(Usage2Service.class);
	
	ServiceRegistry serviceRegistry = null;
	

	UsageDAO usageDao = new AlfServicesWrapper();
	
	public Usage2Service() {
		ApplicationContext applicationContext =  AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	}
	
	public Usage getUsage(String lmsId, String courseId, String parentNodeId, String resourceId) throws UsageException{
		
		Usage result = null;
		
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		AuthenticationTool authTool = new AuthenticationToolAPI();
		HashMap<String, String> currentAuthentication = authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
		HashMap<String,String> adminAuthentication = null;
		
		
		HashMap<String, Object> usage = null;
		try{
			adminAuthentication = authTool.createNewSession(homeRepository.getUsername(), homeRepository.getPassword());
			usage = usageDao.getUsage(lmsId, courseId, parentNodeId, resourceId);
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new UsageException(e.getMessage());
		}finally{
		    //set authentication back to current user
			if(adminAuthentication != null){
				authTool.logout(adminAuthentication.get(CCConstants.AUTH_TICKET));
			}
		    if(currentAuthentication != null){
		    	authTool.validateTicket(currentAuthentication.get(CCConstants.AUTH_TICKET));
		    }
		  }
		
		if (usage != null) {
			result = getUsageResult(usage);
		}
		
		logger.info("return");
		return result;
		
	}
	
	
	public List<Usage> getUsagesByCourse(String appId, String courseId) throws UsageException{
		List<Usage> result = new ArrayList<Usage>();
		
		AuthenticationTool authTool = new AuthenticationToolAPI();
		HashMap<String, String> currentAuthentication = authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
		HashMap<String,String> adminAuthentication = null;
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		try{
			adminAuthentication = authTool.createNewSession(homeRepository.getUsername(), homeRepository.getPassword());
			HashMap<String,HashMap<String,Object>> usages =  usageDao.getUsagesByCourse(appId, courseId);
			for(Map.Entry<String, HashMap<String,Object>> entry : usages.entrySet()){
				result.add(getUsageResult(entry.getValue()));
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		}finally{
		    //set authentication back to current user
			if(adminAuthentication != null){
				authTool.logout(adminAuthentication.get(CCConstants.AUTH_TICKET));
			}
		    if(currentAuthentication != null){
		    	authTool.validateTicket(currentAuthentication.get(CCConstants.AUTH_TICKET));
		    }
		  }
		
		return result;
	}
	
	
	public List<Usage> getUsages(String appId)throws UsageException{
		
		List<Usage> result = new ArrayList<Usage>();
		
		AuthenticationTool authTool = new AuthenticationToolAPI();
		HashMap<String, String> currentAuthentication = authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
		HashMap<String,String> adminAuthentication = null;
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		try{
			adminAuthentication = authTool.createNewSession(homeRepository.getUsername(), homeRepository.getPassword());
			HashMap<String,HashMap<String,Object>> usages =  usageDao.getUsagesByAppId(appId);
			for(Map.Entry<String, HashMap<String,Object>> entry : usages.entrySet()){
				result.add(getUsageResult(entry.getValue()));
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		}finally{
		    //set authentication back to current user
			if(adminAuthentication != null){
				authTool.logout(adminAuthentication.get(CCConstants.AUTH_TICKET));
			}
		    if(currentAuthentication != null){
		    	authTool.validateTicket(currentAuthentication.get(CCConstants.AUTH_TICKET));
		    }
		  }
		
		return result;
	}
	
	
	public Usage setUsage(String repoId, String user, String lmsId, String courseId, String parentNodeId, String userMail, Calendar fromUsed, Calendar toUsed, int distinctPersons, String version, String resourceId, String xmlParams) throws UsageException{
		if (user == null || user.trim().equals("") || lmsId == null || lmsId.trim().equals("") || courseId == null || courseId.trim().equals("") || parentNodeId == null
				|| parentNodeId.trim().equals("")) {
			throw new UsageException(UsageService.MISSING_PARAM);
		}
		
		//admin Authentication 
		
					
		HashMap<String, Object> usage = null;
		
		AuthenticationTool authTool = new AuthenticationToolAPI();
		HashMap<String, String> currentAuthentication = authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
		HashMap<String,String> adminAuthentication = null;
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		try{
			adminAuthentication = authTool.createNewSession(homeRepository.getUsername(), homeRepository.getPassword());
			
			logger.info("before alfServicesWrapper.hasPermissions");
			
			usage = usageDao.getUsage(lmsId, courseId, parentNodeId, resourceId);
			
			//only check publish permission for new content so that an teacher who modifies the course/wysiwyg can safe changes of permission
			if(usage == null){
			
				boolean hasPublishPerm = ((MCAlfrescoClient)RepoFactory.getInstance(homeRepository.getAppId(), (HashMap)null)).hasPermissions(parentNodeId, user, new String[]{CCConstants.PERMISSION_CC_PUBLISH});
				
				if(!hasPublishPerm){
					logger.info("User "+user+" has no publish permission on "+resourceId);
					throw new UsageException(UsageService.NO_CCPUBLISH_PERMISSION);
				}
			}

			
			
		
		
			HashMap<String, Object> properties = new HashMap<String,  Object>();
			
			String guid = null;
			NodeRef personRef = serviceRegistry.getPersonService().getPerson(user);
			if(personRef != null){
				Map<QName, Serializable> personProps = serviceRegistry.getNodeService().getProperties(personRef);
				guid = (String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_GUID));
			}
			
			if(guid == null){
				guid = user;
			}
			
			properties.put(CCConstants.CCM_PROP_USAGE_APPID, lmsId);
			properties.put(CCConstants.CCM_PROP_USAGE_COURSEID, courseId);
			properties.put(CCConstants.CCM_PROP_USAGE_PARENTNODEID, parentNodeId);
			properties.put(CCConstants.CCM_PROP_USAGE_APPUSER, user);
			properties.put(CCConstants.CCM_PROP_USAGE_APPUSERMAIL, userMail);
			if(fromUsed != null){
				properties.put(CCConstants.CCM_PROP_USAGE_FROM, ISO8601DateFormat.format(fromUsed.getTime()));
			}
			if(toUsed != null){
				properties.put(CCConstants.CCM_PROP_USAGE_TO, ISO8601DateFormat.format(toUsed.getTime()));
			}
			properties.put(CCConstants.CCM_PROP_USAGE_MAXPERSONS, distinctPersons);
			properties.put(CCConstants.CCM_PROP_USAGE_COUNTER, new Integer(1).toString());
			
			if(version == null || version.trim().equals("")){
				
				Object ov = serviceRegistry.getNodeService().getProperty(new NodeRef(AlfServicesWrapper.storeRef,parentNodeId),QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION));
				
				if(ov !=null){
					version = (ov instanceof MLText) ? ((MLText)ov).getDefaultValue() : ov.toString();
				}
				version = (version == null) ? (String)serviceRegistry.getNodeService().getProperty(new NodeRef(AlfServicesWrapper.storeRef,parentNodeId),ContentModel.PROP_VERSION_LABEL) : version;
			}
			
			if(version != null){
				properties.put(CCConstants.CCM_PROP_USAGE_VERSION, version);
			}
			
			properties.put(CCConstants.CCM_PROP_USAGE_RESSOURCEID, resourceId);
			properties.put(CCConstants.CCM_PROP_USAGE_XMLPARAMS, xmlParams);
			
			if(guid != null){
				properties.put(CCConstants.CCM_PROP_USAGE_GUID, guid);
			}
			
	
			// if null only set counter @TODO Unique constraint in Schema that
			// prevents bypassing unique with standard alfresco services
			
			String usageNodeId = null;
			if (usage != null) {
				logger.info("usage != null");
				String counter = (String) usage.get(CCConstants.CCM_PROP_USAGE_COUNTER);
				
				usageNodeId = (String) usage.get(CCConstants.SYS_PROP_NODE_UID);
				logger.info("usageNodeId:" + usageNodeId);
			
				
				logger.info("before updating usage with props:");
				for(Map.Entry<String, Object> entry: properties.entrySet()){
					logger.info("key:"+entry.getKey() +" val:"+entry.getValue());
				}
				
				usageDao.updateUsage(usageNodeId, properties);
			} else {
				logger.info("usage is null");
				usageNodeId = usageDao.createUsage(parentNodeId, properties);
			}
			
			
			Usage result = getUsageResult(usageDao.getUsage(usageNodeId));
			
			//remove IO from cache so that the gui gets the new usage count
			new RepositoryCache().remove(parentNodeId);
			
			logger.info("returning");
			
			return result;
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new UsageException(e.getMessage());
		}finally{
		    //set authentication back to current user
			if(adminAuthentication != null){
				authTool.logout(adminAuthentication.get(CCConstants.AUTH_TICKET));
			}
		    if(currentAuthentication != null){
		    	authTool.validateTicket(currentAuthentication.get(CCConstants.AUTH_TICKET));
		    }
		  }
		
	}
	
	
	public List<Usage> getUsageByParentNodeId(String repoId, String user, String parentNodeId) throws UsageException {
		logger.info("starting");
		//admin Authentication 
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
	    
		AuthenticationTool authTool = new AuthenticationToolAPI();
		HashMap<String, String> currentAuthentication = authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
		HashMap<String,String> adminAuthentication = null;
		
		try{
			adminAuthentication = authTool.createNewSession(homeRepository.getUsername(), homeRepository.getPassword());
			
			HashMap<String, HashMap<String, Object>> usages = usageDao.getUsages(parentNodeId);
			logger.info("usages.keySet().size():"+usages.keySet().size());
			ArrayList<Usage> result = new ArrayList<Usage>();
			for (String key : usages.keySet()) {
				result.add(getUsageResult(usages.get(key)));
			}
			
			return result;
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new UsageException(e.getMessage());
		}finally{
		    //set authentication back to current user
			if(adminAuthentication != null){
				authTool.logout(adminAuthentication.get(CCConstants.AUTH_TICKET));
			}
		    if(currentAuthentication != null){
		    	authTool.validateTicket(currentAuthentication.get(CCConstants.AUTH_TICKET));
		    }
		  }
	
    }

    public boolean deleteUsage(String repoId, String user, String lmsId, String courseId, String parentNodeId, String resourceId) throws UsageException {
    	logger.info("starting");
		
		
		//admin Authentication 
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		AuthenticationTool authTool = new AuthenticationToolAPI();
		HashMap<String, String> currentAuthentication = authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
		HashMap<String,String> adminAuthentication = null;
		
		
		try{
			adminAuthentication = authTool.createNewSession(homeRepository.getUsername(), homeRepository.getPassword());
			this.usageDao.removeUsage(lmsId, courseId, parentNodeId, resourceId);
			
			//remove IO from cache so that the gui gets the new usage count
			new RepositoryCache().remove(parentNodeId);
			
			return true;
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			throw new UsageException(e.getMessage());
		} finally{
			  //set authentication back to current user
			if(adminAuthentication != null){
				authTool.logout(adminAuthentication.get(CCConstants.AUTH_TICKET));
			}
		    if(currentAuthentication != null){
		    	authTool.validateTicket(currentAuthentication.get(CCConstants.AUTH_TICKET));
		    }
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
}
