package org.edu_sharing.service.toolpermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.springframework.context.ApplicationContext;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;

public class ToolPermissionService {


	Logger logger = Logger.getLogger(ToolPermissionService.class);
	
	static String[] validToolPermissions = new String[]{
			CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH,
			CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY,
			CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE,
			CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE,
			CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE,
			CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE,
			CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE,
			CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SAFE,
			CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE_SAFE,
			CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES, 
			CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY,CCConstants.CCM_VALUE_TOOLPERMISSION_INVITED,
			CCConstants.CCM_VALUE_TOOLPERMISSION_LICENSE,CCConstants.CCM_VALUE_TOOLPERMISSION_UNCHECKEDCONTENT,
			CCConstants.CCM_VALUE_TOOLPERMISSION_WORKSPACE, CCConstants.CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX, 
			CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL};

	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	
	
	
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	PermissionService permissionService = serviceRegistry.getPermissionService();
	
	NodeService nodeService = serviceRegistry.getNodeService();
	
	org.edu_sharing.service.nodeservice.NodeService eduNodeService = NodeServiceFactory.getNodeService(ApplicationInfoList.getHomeRepository().getAppId());
	
	AuthenticationService authService = serviceRegistry.getAuthenticationService();

	private static String toolPermissionFolder=null;
	
	
	public boolean hasToolPermissionForConnector(String connectorId){
   		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
		String scope=authTool.getScope();
		if(scope==null)
			scope="";
		else
			scope="_"+scope;
		return hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX + connectorId+scope);
	}
	public List<String> getAllAvailableToolPermissions(){
		List<String> allowed=new ArrayList<>();
		for(String permission : ToolPermissionServiceFactory.getAllToolPermissions()){
			if(hasToolPermission(permission))
				allowed.add(permission);
		}
		return allowed;
	}
	public boolean hasToolPermission(String toolPermission) {
		
		
		try{
			if(isAdmin()){
				return true;
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		
		/**
		 * try to use session cache
		 */
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		Boolean hasToolPerm = (Boolean)session.getAttribute(toolPermission);
		if(hasToolPerm == null){
			hasToolPerm = hasToolPermissionWithoutCache(toolPermission);
			session.setAttribute(toolPermission, hasToolPerm);
		}
		return hasToolPerm;
		
	}
	
	
	private boolean hasToolPermissionWithoutCache(String toolPermission) {
		final String repoAdmin = ApplicationInfoList.getHomeRepository().getUsername();
		AuthenticationUtil.RunAsWork<String> workTP= new AuthenticationUtil.RunAsWork<String>() {
			@Override
			public String doWork() throws Exception {
				try {
					
					return getToolPermissionNodeId(toolPermission);
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
					return null;
				}
			}
		};
		
		try{
			if (isAdmin()) {
				return true;
			}
		}catch(Exception e){
			
		}

		String toolNodeId = AuthenticationUtil.runAsSystem(workTP);
		AccessStatus accessStatus = permissionService.hasPermission(new NodeRef(Constants.storeRef, toolNodeId), PermissionService.READ);
		return (0 == accessStatus.compareTo(AccessStatus.ALLOWED));
	}
	
	public String getToolPermissionNodeId(String toolPermission) throws Throwable{
		String systemFolderId = getEdu_SharingToolPermissionsFolder();
		
		
		HashMap<String, Object> sysObject = eduNodeService.getChild(Constants.storeRef, systemFolderId, CCConstants.CCM_TYPE_TOOLPERMISSION, CCConstants.CM_NAME, toolPermission);
		
		if(sysObject == null){
			
			boolean validToolPermission = false;
			for(String tp : validToolPermissions){
				if(toolPermission.startsWith(tp)){
					validToolPermission = true;
				}
			}
			
			if(!validToolPermission) throw new Exception("Invalid ToolPermission " + toolPermission);
			
			logger.info("ToolPermission" + toolPermission+ " does not exsist. will create it.");
			HashMap props = new HashMap();
			props.put(CCConstants.CM_NAME, toolPermission);
			String result = eduNodeService.createNodeBasic(systemFolderId, CCConstants.CCM_TYPE_TOOLPERMISSION, props);
			//set admin as owner cause if it was created by runAs with admin the current user not the runas on is taken
			eduNodeService.setOwner(result, ApplicationInfoList.getHomeRepository().getUsername());
			if(!toolPermission.equals(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL)){
				eduNodeService.setPermissions(result,PermissionService.ALL_AUTHORITIES, new String[]{CCConstants.PERMISSION_READ}, false);
			}
			else{
				eduNodeService.setPermissions(result, null,null, false);
			}
	
			return result;
			
		}else{
			return (String)sysObject.get(CCConstants.SYS_PROP_NODE_UID);
		}
	}
	
	private String getEdu_SharingSystemFolderBase() throws Throwable{
		if(!isAdmin()){
			throw new Exception("Admin group required");
		}
		String companyHomeNodeId = eduNodeService.getCompanyHome();
		HashMap<String, Object> edu_SharingSysMap = eduNodeService.getChild(Constants.storeRef, companyHomeNodeId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
		
		String result = null;
		if(edu_SharingSysMap == null){
			
			String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE);
			HashMap<String,Object> newEdu_SharingSysMapProps  = new HashMap<String,Object>();
			newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
			
			HashMap<String,String> i18nTitle = new HashMap<String,String>();
			i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "de_DE"));
			i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "en_EN"));
			i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "en_US"));
			
			newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
			newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
			
			result = eduNodeService.createNodeBasic(companyHomeNodeId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
			permissionService.setInheritParentPermissions(new NodeRef(Constants.storeRef,result),false);
		}else{
			result = (String)edu_SharingSysMap.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return result;
	}
	
	
	private String getEdu_SharingToolPermissionsFolder() throws Throwable{
		if(toolPermissionFolder!=null)
			return toolPermissionFolder;
		logger.info("fully: "+AuthenticationUtil.getFullyAuthenticatedUser() +" runAs:"+AuthenticationUtil.getRunAsUser());
		String systemFolderId = getEdu_SharingSystemFolderBase();
		HashMap<String, Object> edu_SharingSystemFolderToolPermissions = eduNodeService.getChild(Constants.storeRef, systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TOOLPERMISSIONS);
		String result = null;
		if(edu_SharingSystemFolderToolPermissions == null){
			logger.info("ToolPermission Folder does not exsist. will create it.");
			String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TOOLPERMISSIONS);
			HashMap<String,Object>  newEdu_SharingSysMapProps  = new HashMap<String,Object> ();
			newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
			
			HashMap<String,String> i18nTitle = new HashMap<String,String>();
			i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TOOLPERMISSIONS, "de_DE"));
			i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TOOLPERMISSIONS, "en_EN"));
			i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_TOOLPERMISSIONS, "en_US"));
			
			newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
			newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TOOLPERMISSIONS);
			result = eduNodeService.createNodeBasic(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
		}else{
			result = (String)edu_SharingSystemFolderToolPermissions.get(CCConstants.SYS_PROP_NODE_UID);
		}
		this.toolPermissionFolder=result;
		return result;
	}
	
	
	protected void initToolPermissions(List<String> toolPermissions) throws Throwable{
		
		for(String toolPermission : toolPermissions){
			getToolPermissionNodeId(toolPermission);
		}
		
	}
	
	
	private boolean isAdmin(){
		 try {
			   Set<String> testUsetAuthorities = serviceRegistry.getAuthorityService().getAuthoritiesForUser(AuthenticationUtil.getRunAsUser());
			   for (String testAuth : testUsetAuthorities) {

			    if (testAuth.equals("GROUP_ALFRESCO_ADMINISTRATORS")) {
			     return true;
			    }
			   }
			  } catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
			  }catch(AuthenticationCredentialsNotFoundException e){
				  System.out.println(e.getMessage());
				  return false;
			  }
		 
		if(AuthenticationUtil.isRunAsUserTheSystemUser())
			return true;
		
		return false;
	}
	/**
	 * Clears previously stored tool permissions in the current http session, e.g. when user changes
	 */
	public void invalidateSessionCache() {
		try{
			HttpSession session = Context.getCurrentInstance().getRequest().getSession();
			for(String tp : ToolPermissionServiceFactory.getAllToolPermissions()){
				session.removeAttribute(tp);
			}
		}catch(Throwable t){
			// may fails when no session is active, not an issue
		}
	}
}
