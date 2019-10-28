package org.edu_sharing.service.toolpermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.connector.Connector;
import org.edu_sharing.service.connector.ConnectorList;
import org.edu_sharing.service.connector.ConnectorService;
import org.edu_sharing.service.connector.ConnectorServiceFactory;
import org.springframework.context.ApplicationContext;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;

public class ToolPermissionService {


	Logger logger = Logger.getLogger(ToolPermissionService.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	private ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	private PermissionService permissionService = serviceRegistry.getPermissionService();

	private NodeService nodeService = serviceRegistry.getNodeService();
	private ConnectorService connectorService;

	org.edu_sharing.service.nodeservice.NodeService eduNodeService;
	
	AuthenticationService authService = serviceRegistry.getAuthenticationService();

	private static String toolPermissionFolder=null;


	public void setEduNodeService(org.edu_sharing.service.nodeservice.NodeService eduNodeService) {
		this.eduNodeService = eduNodeService;
	}
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
		for(String permission : this.getAllToolPermissions()){
			if(hasToolPermission(permission))
				allowed.add(permission);
		}
		return allowed;
	}
	
	public List<String> getAllToolPermissions(){
		RunAsWork<List<String>> runas = new RunAsWork<List<String>>() {
			@Override
			public List<String> doWork() throws Exception {
				List<String> result = new ArrayList<String>();
				try {
					
					
					String tpFolder = getEdu_SharingToolPermissionsFolder();
					List<ChildAssociationRef> childAssocRefs = eduNodeService.getChildrenChildAssociationRef(tpFolder);
					for(ChildAssociationRef childAssocRef : childAssocRefs) {
						String name = eduNodeService.getProperty(childAssocRef.getChildRef().getStoreRef().getProtocol(), childAssocRef.getChildRef().getStoreRef().getIdentifier(), childAssocRef.getChildRef().getId(), CCConstants.CM_NAME);
						result.add(name);
					}
				}catch(Throwable e) {
					logger.error(e.getMessage(), e);
				}
				return result;
			}
		};
		
		return AuthenticationUtil.runAsSystem(runas);
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
		AccessStatus accessStatusDenied = permissionService.hasPermission(new NodeRef(Constants.storeRef, toolNodeId), CCConstants.PERMISSION_DENY);
		if(accessStatusDenied.equals(AccessStatus.ALLOWED)) {
			logger.info("Toolpermission "+toolPermission+" has explicit Deny permission");;
		}
		return accessStatus.equals(AccessStatus.ALLOWED) && !accessStatusDenied.equals(AccessStatus.ALLOWED);
	}
	static Map<String,String> toolPermissionNodeCache = new HashMap<>();
	public String getToolPermissionNodeId(String toolPermission) throws Throwable{
		if(toolPermissionNodeCache.containsKey(toolPermission)) {
			String nodeId=toolPermissionNodeCache.get(toolPermission);
			// validate that the cached node is not deleted
			if(eduNodeService.exists(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId))
				return nodeId;
		}
		String systemFolderId = getEdu_SharingToolPermissionsFolder();


        NodeRef sysObject = eduNodeService.getChild(Constants.storeRef, systemFolderId, CCConstants.CCM_TYPE_TOOLPERMISSION, CCConstants.CM_NAME, toolPermission);
		
		if(sysObject == null){

			String result = createToolpermission(toolPermission, systemFolderId);
	
			return result;
			
		}else{
			String nodeId=sysObject.getId();
			toolPermissionNodeCache.put(toolPermission, nodeId);
			return nodeId;
		}
	}

	private String createToolpermission(String toolPermission, String systemFolderId) throws Exception {
		logger.info("ToolPermission" + toolPermission+ " does not exists. will create it.");
		HashMap props = new HashMap();
		props.put(CCConstants.CM_NAME, toolPermission);
		String result = eduNodeService.createNodeBasic(systemFolderId, CCConstants.CCM_TYPE_TOOLPERMISSION, props);
		//set admin as owner cause if it was created by runAs with admin the current user not the runas on is taken
		eduNodeService.setOwner(result, ApplicationInfoList.getHomeRepository().getUsername());
		if(getAllDefaultAllowedToolpermissions().contains(toolPermission)){
			logger.info("ToolPermission" + toolPermission+ " is allowed by default. Will set GROUP_EVERYONE.");
			eduNodeService.setPermissions(result,PermissionService.ALL_AUTHORITIES, new String[]{CCConstants.PERMISSION_READ}, false);
		}
		else{
			eduNodeService.setPermissions(result, null,null, false);
		}
		return result;
	}

	public String getEdu_SharingSystemFolderBase() throws Throwable{
		if(!isAdmin()){
			throw new Exception("Admin group required");
		}
		String companyHomeNodeId = eduNodeService.getCompanyHome();
		NodeRef edu_SharingSysMap = eduNodeService.getChild(Constants.storeRef, companyHomeNodeId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
		
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
			result = edu_SharingSysMap.getId();
		}
		return result;
	}
	
	
	public String getEdu_SharingToolPermissionsFolder() throws Throwable{
		if(toolPermissionFolder!=null)
			return toolPermissionFolder;
		logger.info("fully: "+AuthenticationUtil.getFullyAuthenticatedUser() +" runAs:"+AuthenticationUtil.getRunAsUser());
		String systemFolderId = getEdu_SharingSystemFolderBase();
		NodeRef edu_SharingSystemFolderToolPermissions = eduNodeService.getChild(Constants.storeRef, systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TOOLPERMISSIONS);
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
			result = edu_SharingSystemFolderToolPermissions.getId();
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
			for(String tp : this.getAllToolPermissions()){
				session.removeAttribute(tp);
			}
		}catch(Throwable t){
			// may fails when no session is active, not an issue
		}
	}

	public void init(){
		try {
			initToolPermissions(getAllPredefinedToolPermissions());
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}
	public List<String> getAllDefaultAllowedToolpermissions(){
		List<String> toInit=getAllPredefinedToolPermissions();
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL); // safe
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_EDITORIAL); // editorial collections
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CURRICULUM); // curriculum collections
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING); // pin collections
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_HANDLESERVICE); // use handle id
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS);
		return toInit;
	}
	public List<String> getAllPredefinedToolPermissions(){
		List<String> toInit=new ArrayList<String>();
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE);

		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_STREAM);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_LINK);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SAFE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE_SAFE);

		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_LICENSE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_UNCHECKEDCONTENT);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_WORKSPACE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL);

		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CHANGE_OWNER);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_EDITORIAL);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CURRICULUM);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_HANDLESERVICE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS);

		addConnectorToolpermissions(toInit);
		return toInit;
	}

	private void addConnectorToolpermissions(List<String> toInit) {
		ConnectorList connectorList =  ConnectorServiceFactory.getConnectorList();
		for(Connector c : connectorList.getConnectors()){
			String tp = CCConstants.CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX + c.getId();
			toInit.add(tp);

			String tp_safe = tp + "_safe";
			toInit.add(tp_safe);
		}
	}
}
