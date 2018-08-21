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

import java.util.Calendar;
import java.util.HashMap;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;

public class UserEnvironmentTool {
	
	private static Log logger = LogFactory.getLog(UserEnvironmentTool.class);
	MCAlfrescoBaseClient mcBaseClient = null;
	String username = null;
	
	/**
	 * use this for running this class in an runAs context
	 * @param appId
	 * @param username
	 * @throws Throwable
	 */
	public UserEnvironmentTool(String runAsUser) throws Throwable{
		username = runAsUser;
		mcBaseClient = (MCAlfrescoBaseClient)RepoFactory.getInstanceForRepo(ApplicationInfoList.getHomeRepository(), null);
	}
	
	public UserEnvironmentTool(String repositoryId, HashMap<String,String> authInfo) throws Throwable{
		username = authInfo.get(CCConstants.AUTH_USERNAME);
		ApplicationInfo appInfo = (repositoryId != null)? ApplicationInfoList.getRepositoryInfoById(repositoryId) : ApplicationInfoList.getHomeRepository();
		mcBaseClient = (MCAlfrescoBaseClient)RepoFactory.getInstanceForRepo(appInfo, authInfo);
	}
	
	public String getDefaultUserDataFolder() throws Throwable{
		
		String homeFolderId = mcBaseClient.getHomeFolderID(username);
		
		logger.info("homefolder:"+homeFolderId);
		String result = null;
		
		HashMap<String,Object> defaultDataFolderProps = mcBaseClient.getChild(homeFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS);
		if(defaultDataFolderProps != null){
			result = (String)defaultDataFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
		}else{
			logger.error("something went wrong! no datafolder for current user "+username+" found!");
		}
		
		return result;
	}
	
	public String getDefaultImageFolder() throws Throwable{
		
		String homeFolderId = mcBaseClient.getHomeFolderID(username);
		
		logger.info("homefolder:"+homeFolderId);
		String result = null;
		
		HashMap<String,Object> defaultImageFolderProps = mcBaseClient.getChild(homeFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_IMAGES);
		if(defaultImageFolderProps != null){
			result = (String)defaultImageFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
		}else{
			logger.error("something went wrong! no image folder for current user "+username+" found!");
		}
		
		return result;
	}
	
	public String getEdu_SharingSystemFolderBase() throws Throwable{
		if(!mcBaseClient.isAdmin()){
			throw new Exception("Admin group required");
		}
		String companyHomeNodeId = mcBaseClient.getCompanyHomeNodeId();
		HashMap<String, Object> edu_SharingSysMap = mcBaseClient.getChild(companyHomeNodeId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
		
		String result = null;
		if(edu_SharingSysMap == null){
			
			String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE);
			HashMap newEdu_SharingSysMapProps  = new HashMap();
			newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
			
			HashMap i18nTitle = new HashMap();
			i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "de_DE"));
			i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "en_EN"));
			i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "en_US"));
			
			newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
			newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
			result = mcBaseClient.createNode(companyHomeNodeId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
		}else{
			result = (String)edu_SharingSysMap.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return result;
	}
	
	public String getEdu_SharingSystemFolderUpdate() throws Throwable{
		if(!mcBaseClient.isAdmin()){
			throw new Exception("Admin group required");
		}
		
		String systemFolderId = getEdu_SharingSystemFolderBase();
		HashMap<String, Object> edu_SharingSystemFolderUpdate = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_UPDATE);
	
		String result = null;
		if(edu_SharingSystemFolderUpdate == null){
			String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_UPDATE);
			HashMap newEdu_SharingSysMapProps  = new HashMap();
			newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
			
			HashMap i18nTitle = new HashMap();
			i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_UPDATE, "de_DE"));
			i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_UPDATE, "en_EN"));
			i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_UPDATE, "en_US"));
			
			newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
			newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_UPDATE);
			result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
		}else{
			result = (String)edu_SharingSystemFolderUpdate.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return result;
	}
	
	
	public String getEdu_SharingNotifyFolder() throws Throwable{
		String systemFolderId = getEdu_SharingSystemFolderBase();
		
		String currentScope = NodeServiceInterceptor.getEduSharingScope();
		
		//String notifyRootFolderName = (currentScope == null || currentScope.trim().isEmpty()) ? CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY : CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY + "_" + currentScope;
		
		String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY);
		systemFolderName = (currentScope == null || currentScope.trim().isEmpty()) ? systemFolderName: systemFolderName + "_" + currentScope;
		HashMap<String, Object> edu_SharingSystemFolderNotify = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, systemFolderName);
		String result = null;
		if(edu_SharingSystemFolderNotify == null){
			
			HashMap newEdu_SharingSysMapProps  = new HashMap();
			newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
			
			HashMap i18nTitle = new HashMap();
			i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY, "de_DE"));
			i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY, "en_EN"));
			i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY, "en_US"));
			
			newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
			newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);
			result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
		}else{
			result = (String)edu_SharingSystemFolderNotify.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return result;
	}
	
	public String getEdu_SharingTemplateFolder() throws Throwable{
	    return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE,CCConstants.I18n_SYSTEMFOLDER_TEMPLATE);
	    /*
		String systemFolderId = getEdu_SharingSystemFolderBase();
		HashMap<String, Object> edu_SharingSystemFolderTemplate = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE);
		String result = null;
		if(edu_SharingSystemFolderTemplate == null){
			String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE);
			HashMap newEdu_SharingSysMapProps  = new HashMap();
			newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
			
			HashMap i18nTitle = new HashMap();
			i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE, "de_DE"));
			i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE, "en_EN"));
			i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE, "en_US"));
			
			newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
			newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE);
			result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
		}else{
			result = (String)edu_SharingSystemFolderTemplate.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return result;
		*/
	}
	public String getOrCreateSystemFolderByName(String constantName,String i18nId) throws Throwable {
        String result;
        String systemFolderId = getEdu_SharingSystemFolderBase();
        HashMap<String, Object> edu_SharingSystemFolderTemplate = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, constantName);
        if(edu_SharingSystemFolderTemplate == null){
            String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TEMPLATE);
            HashMap<String,Object> newEdu_SharingSysMapProps  = new HashMap();
            newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);

            HashMap<String,String> i18nTitle = new HashMap();
            i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(i18nId, "de_DE"));
            i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(i18nId, "en_EN"));
            i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(i18nId, "en_US"));

            newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
            newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE);
            result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
        }else{
            result = (String)edu_SharingSystemFolderTemplate.get(CCConstants.SYS_PROP_NODE_UID);
        }
        return result;
    }
	
	
	/**
	 * returns a folder where notify Objects can be safed
	 * 
	 * @return
	 * @throws Throwable
	 */
	public String getEdu_SharingNotifyFolderToSafe() throws Throwable{
		String notifyFolder = getEdu_SharingNotifyFolder();
		
		String year = new Integer(Calendar.getInstance().get(Calendar.YEAR)).toString();
		String yearMapId = getMap(notifyFolder,year,CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);
		
		String month = new Integer(Calendar.getInstance().get(Calendar.MONTH)).toString();
		String monthMapId = getMap(yearMapId,month,CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);
		
		String day = new Integer(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).toString();
		String dayMapId = getMap(monthMapId,day,CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);
		
		return dayMapId;
	}
	
	public String getEdu_SharingValuespaceFolder() throws Throwable{
		String systemFolderId = getEdu_SharingSystemFolderBase();
		HashMap<String, Object> edu_SharingSystemFolderValuespace = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_VALUESPACE);
		String result = null;
		if(edu_SharingSystemFolderValuespace == null){
			String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_VALUESPACE);
			HashMap newEdu_SharingSysMapProps  = new HashMap();
			newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
			
			HashMap i18nTitle = new HashMap();
			i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_VALUESPACE, "de_DE"));
			i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_VALUESPACE, "en_EN"));
			i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_VALUESPACE, "en_US"));
			
			newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
			newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_VALUESPACE);
			result = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
		}else{
			result = (String)edu_SharingSystemFolderValuespace.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return result;
	}
	
		
	/**
	 * returns the child map with name. when it does not exist it will be created
	 * @param parentId
	 * @param name
	 * @return
	 * @throws Throwable
	 */
	private String getMap(String parentId, String name, String mapType) throws Throwable{
		HashMap<String,Object> childProps = mcBaseClient.getChild(parentId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, name);
		String childId = null;
		
		if(childProps == null){
			HashMap<String,Object> props = new HashMap<String,Object>();
			props.put(CCConstants.CM_NAME, name);
			props.put(CCConstants.CM_PROP_TITLE, name);
			if(mapType != null){
				props.put(CCConstants.CCM_PROP_MAP_TYPE, mapType);
			}
			childId = mcBaseClient.createNode(parentId, CCConstants.CCM_TYPE_MAP, props);
		}else{
			childId = (String)childProps.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return childId;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
