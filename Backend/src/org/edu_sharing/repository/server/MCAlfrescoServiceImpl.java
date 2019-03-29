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
package org.edu_sharing.repository.server;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.service.cmr.repository.CyclicChildRelationshipException;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.EmailValidator;
import org.edu_sharing.repository.client.MCAlfrescoService;
import org.edu_sharing.repository.client.auth.CCSessionExpiredException;
import org.edu_sharing.repository.client.exception.CCException;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.AssignedLicense;
import org.edu_sharing.repository.client.rpc.Authority;
import org.edu_sharing.repository.client.rpc.CheckForDuplicatesResult;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.EnvInfo;
import org.edu_sharing.repository.client.rpc.Everyone;
import org.edu_sharing.repository.client.rpc.GetPermissions;
import org.edu_sharing.repository.client.rpc.GetPreviewResult;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Owner;
import org.edu_sharing.repository.client.rpc.PermissionContainer;
import org.edu_sharing.repository.client.rpc.RepositoryInfo;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.repository.client.rpc.SearchCriterias;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.rpc.SearchToken;
import org.edu_sharing.repository.client.rpc.ServerUpdateInfo;
import org.edu_sharing.repository.client.rpc.SetPermissions;
import org.edu_sharing.repository.client.rpc.SetPermissionsAndMail;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.search.SearchMetadataHelper;
import org.edu_sharing.repository.client.tracking.TrackingEvent.ACTIVITY;
import org.edu_sharing.repository.client.tracking.TrackingEvent.CONTEXT_ITEM;
import org.edu_sharing.repository.client.tracking.TrackingEvent.PLACE;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.jobs.quartz.ImmediateJobListener;
import org.edu_sharing.repository.server.jobs.quartz.JobHandler;
import org.edu_sharing.repository.server.jobs.quartz.JobHandler.JobConfig;
import org.edu_sharing.repository.server.jobs.quartz.OAIConst;
import org.edu_sharing.repository.server.jobs.quartz.RefreshPublisherListJob;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.CheckAuthentication;
import org.edu_sharing.repository.server.tools.EduGroupTool;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.ISO8601DateFormat;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.repository.server.tools.cache.CacheManagerFactory;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.edu_sharing.repository.server.tools.forms.Helper;
import org.edu_sharing.repository.server.tools.metadataset.MetadataCache;
import org.edu_sharing.repository.server.tools.metadataset.MetadataReader;
import org.edu_sharing.repository.server.tools.search.QueryBuilder;
import org.edu_sharing.repository.server.tools.search.QueryValidationFailedException;
import org.edu_sharing.repository.server.tracking.TrackingService;
import org.edu_sharing.repository.update.ClassificationKWToGeneralKW;
import org.edu_sharing.repository.update.Edu_SharingAuthoritiesUpdate;
import org.edu_sharing.repository.update.Edu_SharingPersonEsuidUpdate;
import org.edu_sharing.repository.update.FixMissingUserstoreNode;
import org.edu_sharing.repository.update.FolderToMap;
import org.edu_sharing.repository.update.KeyGenerator;
import org.edu_sharing.repository.update.Licenses1;
import org.edu_sharing.repository.update.Licenses2;
import org.edu_sharing.repository.update.RefreshMimetypPreview;
import org.edu_sharing.repository.update.Release_1_6_SystemFolderNameRename;
import org.edu_sharing.repository.update.Release_1_7_SubObjectsToFlatObjects;
import org.edu_sharing.repository.update.Release_1_7_UnmountGroupFolders;
import org.edu_sharing.repository.update.Release_3_2_DefaultScope;
import org.edu_sharing.repository.update.Release_3_2_FillOriginalId;
import org.edu_sharing.repository.update.SystemFolderNameToDisplayName;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.environment.EnvironmentService;
import org.edu_sharing.service.license.AssignedLicenseService;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.share.EMailSendFailedException;
import org.edu_sharing.service.share.EMailValidationException;
import org.edu_sharing.service.share.ExpiryDateValidationException;
import org.edu_sharing.service.share.NodeDoesNotExsistException;
import org.edu_sharing.service.share.PermissionFailedException;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.quartz.Scheduler;
import org.springframework.extensions.surf.util.I18NUtil;

import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class MCAlfrescoServiceImpl extends RemoteServiceServlet implements MCAlfrescoService {
	private static Log logger = LogFactory.getLog(MCAlfrescoServiceImpl.class);
	public static String PATH_DELIMITER= "/";

	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		HttpSession httpSession = req.getSession();
		String localeStr = (String)httpSession.getAttribute(CCConstants.AUTH_LOCALE);
		if(localeStr != null && !localeStr.trim().equals("")){
			Locale locale = I18NUtil.parseLocale(localeStr);
			I18NUtil.setLocale(locale);
		}
	
		//call this after req.getSession
		//cause of: java.lang.IllegalStateException: Cannot create a session after the response has been committed
		super.service(req, resp);
	}
	
	public ArrayList<HashMap<String,Object>> getNewestNodes(String repositoryId,Integer from, Integer to) throws CCException{
		try{
			MCAlfrescoBaseClient repoClient = getMCAlfrescoBaseClient(repositoryId);
			return repoClient.getNewestNodes(from,to);
		}catch(Throwable e){
			this.errorHandling(e);
			return null;
		}
	}
	

	public SearchResult search(SearchToken searchToken) throws CCException {
		logger.info("start searching Repository:" + searchToken.getRepositoryId() + " cap:"
				+ ApplicationInfoList.getRepositoryInfoById(searchToken.getRepositoryId()).getAppCaption());
		SearchResult result = null;
		if (searchToken != null) {
			try {
				MCBaseClient mcBaseClient = getMCBaseClient(searchToken.getRepositoryId());
				result = mcBaseClient.search(searchToken);
			} catch (Throwable e) {
				errorHandling(e);
			}
		}
		logger.info("end searching Repository:" + ApplicationInfoList.getRepositoryInfoById(searchToken.getRepositoryId()).getAppCaption() +" critEmpty?"+searchToken.getSearchCriterias().criteriasEmpty());
		return result;
	}
	
	
	public SearchResult searchRecommendObjects(String repositoryId, int startIdx, int nrOfResults) throws CCException {
		try {
			MCBaseClient mcalfClient = getMCBaseClient(repositoryId);
			return mcalfClient.searchRecommend(startIdx, nrOfResults);
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}

	public HashMap<String, HashMap<String, Object>> search(String searchWord, String type) throws CCException {
		try {
			MCAlfrescoClient mcalfClient = getMCAlfrescoBaseClient(null);
			return mcalfClient.search(searchWord, type);
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}
	
	public HashMap<String, HashMap<String, Object>> searchWorkflowTasks(String searchWord) throws CCException {
		try {
			MCAlfrescoClient mcalfClient = getMCAlfrescoBaseClient(null);
			
			String query = "@ccm\\:wf_receiver:"+Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.AUTH_USERNAME);
			if(searchWord != null && !searchWord.trim().equals("")){
				query += " AND cm\\:name:"+ searchWord;
			}
			return mcalfClient.search(query);
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}
	
	public ArrayList<EduGroup> getEduGroups() throws CCException {
		try {
			return getMCAlfrescoBaseClient(null).getEduGroups();
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}
	
	public HashMap<String, HashMap<String, Object>> searchInvited(SearchToken searchToken) throws CCException{
		try {
			MCAlfrescoBaseClient mcBaseClient = (MCAlfrescoBaseClient)getMCAlfrescoBaseClient(searchToken.getRepositoryId());
			
			if (mcBaseClient instanceof MCAlfrescoAPIClient) {
				MCAlfrescoAPIClient apiClient = (MCAlfrescoAPIClient)mcBaseClient;
				return apiClient.searchInvited(searchToken);
			} else {
				logger.error(mcBaseClient.getClass().getName()+" invited search is not implemented!!!");
			}
			
		} catch (Throwable e) {
			errorHandling(e);
		}
		return new HashMap<String, HashMap<String, Object>>();
	}
	
	public HashMap<String, HashMap<String, Object>> searchByParentId(SearchCriterias searchCriterias, String parentId) throws CCException {
				
		try{
			
			MCAlfrescoBaseClient mcalfClient = getMCAlfrescoBaseClient(null);			
			
			String pathParent = (parentId != null) ? mcalfClient.getPath(parentId) : "";
			
			//set the value for the path criteria
			for(Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> queryEntry : searchCriterias.getMetadataSetSearchData().entrySet()){
				if(queryEntry.getKey().getCriteriaboxid().equals("pathquery")){
					for(Map.Entry<MetadataSetQueryProperty, String[]> propEntry : queryEntry.getValue().entrySet()){
						if(propEntry.getKey().getName().equals(MetadataSetQueryProperty.PROPERTY_NAME_CONSTANT_path)){
							propEntry.setValue(new String[]{pathParent});
						}
					}
				}
			}
			
			QueryBuilder qb = new QueryBuilder();
			qb.setSearchCriterias(searchCriterias);
			String query = qb.getSearchString();
						
			List<String[]> prefixs = new ArrayList<String[]>();
			
			prefixs.add(new String[]{pathParent, ""});
			
			HashMap groups = mcalfClient.getGroupFolders();
			for (Object key : groups.keySet()) {
				HashMap<String, Object> props = (HashMap<String, Object>) groups.get(key);
				prefixs.add(new String[]{mcalfClient.getPath((String) key), (String) props.get(CCConstants.CM_NAME)});
			}			
			
			/**
			 * hard limit result for workspace cause of performance.
			 * workspace serverside paging is hard to implement. getChildren(no paging) vs solr search(paging possible) 
			 */
			String[] nodeIds = mcalfClient.searchNodeIds(query,500);
			
			if(nodeIds != null){
				HashMap<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
				for(String nodeId : nodeIds){
					
					if(nodeId.contains("missing")){
						//SOLR tracker not ready
						continue;
					}
					
					HashMap<String, Object> properties = mcalfClient.getProperties(nodeId);
					if(properties != null){
						
						String pathNode = mcalfClient.getPath(nodeId);
						
						if (pathNode != null) { 
			
							
							for (String[] prefix : prefixs) {
								
								if (pathNode.startsWith(prefix[0])) {
									pathNode = pathNode.substring(prefix[0].length());
									
									if (prefix[1].length() > 0) {
										pathNode = PATH_DELIMITER + prefix[1] + pathNode;
									}
									
									break;
								}
							}
							
							if (pathNode.lastIndexOf(PATH_DELIMITER) != -1) {
								pathNode = pathNode.substring(0, pathNode.lastIndexOf(PATH_DELIMITER));
							}
						}
					
						properties.put(CCConstants.EDUSEARCH_PARENTPATH, pathNode); 
						
						result.put(nodeId,properties);
					}					
				}
				return result;
			}else{
				return null;
			}
					
		} catch(Throwable e) {
			this.errorHandling(e);
			return null;
		}
		
	}
	
	public String getNodeIdAuthorityContainer() throws CCException {
		try {
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(ApplicationInfoList.getHomeRepository().getAppId());
			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(null);
			if(mcAlfClient.isAdmin(authTool.validateAuthentication(this.perThreadRequest.get().getSession()).get(CCConstants.AUTH_USERNAME))){
				return mcAlfClient.searchNodeIds("PATH:\"/sys:system/sys:authorities\"")[0];
			}else{
				throw new Exception("no admin rights");
			}
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}

	public HashMap<String,HashMap<String,Object>> getChildren(String parentID, String repositoryId) throws CCException {
		try {
			
			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(repositoryId);
			HashMap<String,HashMap<String,Object>> result = mcAlfClient.getChildren(parentID);
			return result;
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}
	
	
	public HashMap<String,HashMap<String,Object>> getChildren(String parentID, String[] permissions, String repositoryId) throws CCException {
		try {
			
			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(repositoryId);
			HashMap<String,HashMap<String,Object>> result = mcAlfClient.getChildren(parentID,permissions);
			return result;
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}
	
	
	public HashMap getChildrenForGroups(String parentID, String repositoryId, HashMap<String, String> authenticationInfo) throws CCException {
		try {
			MCAlfrescoBaseClient mcBaseClient = getMCAlfrescoBaseClient(repositoryId);
			String nodeIdAuthorityContainer = this.getNodeIdAuthorityContainer();
			
			//getchildrenByGroup does not deliver the childAssocName so use default method and filter non groups
			HashMap<String, HashMap<String, Object>> childGroups = mcBaseClient.getChildren(parentID);
			
			ArrayList<String> filterNonGroups = new ArrayList<String>();
		
			for(String key : childGroups.keySet()){
				
				if(!CCConstants.CM_TYPE_AUTHORITY_CONTAINER.equals(childGroups.get(key).get(CCConstants.NODETYPE))){
					filterNonGroups.add(key);
				}
			}
			for(String nonGroupId:filterNonGroups){
				childGroups.remove(nonGroupId);
			}
			/**
			 * filter all groups that got a parent in the first level
			 */
			if(parentID.equals(nodeIdAuthorityContainer)){
				logger.info("will remove groups with group parents");
				ArrayList<String> filterGroups = new ArrayList<String> ();
				for(String key : childGroups.keySet()){
					HashMap<String, HashMap> parents = mcBaseClient.getParents(key, false);
					for(String groupParentId:parents.keySet()){
						String groupParentType = (String)parents.get(groupParentId).get(CCConstants.NODETYPE);
						if(groupParentType.equals(CCConstants.CM_TYPE_AUTHORITY_CONTAINER)){
							filterGroups.add(key);
						}
					}
				}
				
				for(String filterKey:filterGroups){
					childGroups.remove(filterKey);
				}
				
			}
			
			return childGroups;
			
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}
	

	public HashMap<String, HashMap<String, Object>> getChildrenByType(String nodeId, String type)
			throws CCException {
		try {

			MCBaseClient mcBaseClient = getMCAlfrescoBaseClient(null);
			return mcBaseClient.getChildrenByType(nodeId, type);
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}

	}

	public HashMap<String, HashMap<String, Object>> getChildrenByType(String nodeId, String type, String repositoryId) throws CCException {
		try {
			MCBaseClient mcBaseClient = getMCAlfrescoBaseClient(repositoryId);
			return mcBaseClient.getChildrenByType(nodeId, type);
		} catch (Throwable e) {
			errorHandling(e);
			return null;

		}
	}

	public HashMap<String, HashMap<String, Object>> getChildrenByAssociation(String store, String nodeId, String association, String repositoryId) throws CCException {
		try {
			MCBaseClient mcBaseClient = getMCAlfrescoBaseClient(null);
			if (mcBaseClient instanceof MCAlfrescoClient) {
				MCAlfrescoClient mcAlfrescoApiClient = (MCAlfrescoClient) mcBaseClient;
				return mcAlfrescoApiClient.getChildrenByAssociation(store, nodeId, association);
			} else {
				return null;
			}
		} catch (Throwable e) {
			errorHandling(e);
			return null;

		}
	}

	public HashMap getParents(String nodeId, boolean primary) throws CCException {
		HashMap result = null;
		try {

			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(null);
			result = mcAlfClient.getParents(nodeId, primary);
		} catch (Throwable e) {
			errorHandling(e);
		}
		return result;
	}
	
	public String getBasketId() throws CCException {
		try {
			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(null);
			return mcAlfClient.getFavoritesFolder();
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}

	public String getGroupFolderId() throws CCException {
		try {
			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(null);
			return mcAlfClient.getGroupFolderId();
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}
	
	public HashMap getGroupFolder() throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfClient = getMCAlfrescoBaseClient(null);
			String folderId = mcAlfClient.getGroupFolderId();
			if (folderId==null) return null;
			return mcAlfClient.getProperties(folderId);
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}

	public HashMap getBaskets() throws CCException {
		try {
			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(null);
			return mcAlfClient.getBaskets();
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}

	public HashMap getGroupFolders() throws CCException {
		try {
			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(null);
			return mcAlfClient.getGroupFolders();
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}

	public String newBasket(String _basketName) throws CCSessionExpiredException, Exception {
		HttpServletRequest req = this.getThreadLocalRequest();
		logger.info("RemoteAddress:" + req.getRemoteAddr() + " RemoteHost:" + req.getRemoteHost() + " RemoteUser:" + req.getRemoteUser());
		try {

			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(null);
			return mcAlfClient.newBasket(_basketName);
		} catch (Throwable e){
			this.errorHandling(e);
			return null;
		}
	}

	public boolean removeBasket(String basketID) throws CCSessionExpiredException, Exception {
		HttpServletRequest req = this.getThreadLocalRequest();
		logger.info("RemoteAddress:" + req.getRemoteAddr() + " RemoteHost:" + req.getRemoteHost() + " RemoteUser:" + req.getRemoteUser());
		try {
			MCAlfrescoClient mcAlfClient = getMCAlfrescoBaseClient(null);
			return mcAlfClient.removeBasket(basketID);
		} catch (Throwable e) {
			this.errorHandling(e);
			return false;
		}
	}

	/**
	 * Drop an Home Node to Basket
	 */
	public boolean dropToBasket(String basketID, String nodeID) throws CCSessionExpiredException, CCException,
			Exception {
		HttpServletRequest req = this.getThreadLocalRequest();
		logger.info("RemoteAddress:" + req.getRemoteAddr() + " RemoteHost:" + req.getRemoteHost() + " RemoteUser:" + req.getRemoteUser());
		try {
			
			MCAlfrescoBaseClient mcAlfBaseClient = getMCAlfrescoBaseClient(null);

			boolean result = mcAlfBaseClient.createChildAssociation(basketID, nodeID);

			/**
			 * alfresco Problem
			 * http://forums.alfresco.com/en/viewtopic.php?f=4&t
			 * =20335&p=66165&hilit=permission+problem#p66165
			 */
			if (result) {

				// check if it's the users favorite folder
				String favFolderId = mcAlfBaseClient.getFavoritesFolder();
				HashMap<String, Object> basketProps = mcAlfBaseClient.getProperties(basketID);
				HashMap<String, HashMap> parents = mcAlfBaseClient.getParents(basketID, true);

				if (parents.keySet() != null && parents.keySet().contains(favFolderId)) {

					ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
					AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
					HashMap adminAuthInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());

					MCAlfrescoBaseClient mcAlfClientAdmin = (MCAlfrescoBaseClient) RepoFactory.getInstance(homeRep.getAppId(), adminAuthInfo);

					org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(homeRep.getAppId());
					
					ACL permissions = mcAlfClientAdmin.getPermissions(nodeID);
					if (permissions != null) {
						HashMap<String,String> authenticationInfo = RepoFactory.getAuthenticationToolInstance(null).validateAuthentication(this.perThreadRequest.get().getSession());
						permissionService.setPermissions(nodeID, authenticationInfo.get(CCConstants.AUTH_USERNAME),
								new String[] { CCConstants.PERMISSION_CC_REMOVEFROMBASKET }, new Boolean(permissions.isInherited()));
					}
				}
			}
			return result;	
		} 
		catch (org.apache.axis.AxisFault e) {
			if (e.getFaultReason().contains("org.apache.ws.security.WSSecurityException")) {
				throw new CCSessionExpiredException(CCConstants.CC_SESSION_EXPIRED_ID);
			} else {
				logger.error("Exception", e);
			}
			return false;
			// for API mode other exceptions can occur
		} catch (CyclicChildRelationshipException e) {
			throw new CCException(CCException.CYCLE_CHILDRELATION, CCConstants.CC_EXCEPTION_CYCLE_CHILDRELATION);
		} catch (DuplicateChildNodeNameException e) {
			throw new CCException(CCException.DUPLICATE_CHILD, CCConstants.CC_EXCEPTION_DUPLICATE_CHILD);
		} catch (Throwable e) {
			this.errorHandling(e);
			return false;
		}
	}

	/**
	 * Drop an remote Node to basket
	 * 
	 * @return Node Id of RemoteObject
	 */
	public String dropToBasketRemoteNode(String basketId, HashMap<String, String> params)
			throws CCSessionExpiredException, CCException, Exception {

		String result = null;
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
			result = mcAlfrescoBaseClient.dropToBasketRemoteNode(basketId, params);
		} 
		catch (org.apache.axis.AxisFault e) {

			if (e.getFaultReason().contains("org.apache.ws.security.WSSecurityException")) {
				throw new CCSessionExpiredException(CCConstants.CC_SESSION_EXPIRED_ID);
			} else {
				logger.error("Exception", e);
			}

		} catch (Throwable e) {
			errorHandling(e);
		}
		return result;
	}

	public boolean removeFromBasket(String basketID, String nodeID) throws CCException {
		HttpServletRequest req = this.getThreadLocalRequest();
		logger.info("RemoteAddress:" + req.getRemoteAddr() + " RemoteHost:" + req.getRemoteHost() + " RemoteUser:" + req.getRemoteUser());
		try {
			
			try {
				MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
				mcAlfrescoBaseClient.removeNodeAndRelations(nodeID, basketID);
				return true;

			} catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
				logger.info("fallback to proxy removing API because of " + e.getMessage());
				// alfresco bug
				// http://forums.alfresco.com/en/viewtopic.php?f=4&t=20335&p=66165&hilit=permission+problem#p66165
				// return removeFromBasketWithAdmin(basketID, nodeID, authenticationInfo);
				return removeNodeAdmin(nodeID, basketID, ApplicationInfoList.getHomeRepository().getAppId());
			}

		} catch (Throwable e) {
			errorHandling(e);
			logger.info("returning false");
			return false;
		}
	}

	public void createRelation(String parentID, String sourceID, String targetID, String name) throws CCException {
		try {
			
			// name is not importend for relation objekt but it has to be unique
			// cause its parent uses cm:contains which is duplicate=false so
			// take name+"_"+System.currentTimeMillis()

			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
			HashMap props = new HashMap();
			props.put(CCConstants.CM_NAME, name + "_" + System.currentTimeMillis());
			String relNodeID = mcAlfrescoBaseClient.createNode(parentID, CCConstants.CCM_TYPE_MAPRELATION, props);
			mcAlfrescoBaseClient.createAssociation(relNodeID, sourceID, CCConstants.CCM_ASSOC_RELSOURCE);
			mcAlfrescoBaseClient.createAssociation(relNodeID, targetID, CCConstants.CCM_ASSOC_RELTARGET);

		} catch (org.apache.axis.AxisFault e) {

			if (e.getFaultReason().contains("org.apache.ws.security.WSSecurityException")) {
				throw new CCSessionExpiredException(CCConstants.CC_SESSION_EXPIRED_ID);
			} else {
				logger.error("Exception", e);
			}

		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	public void createNode(String parentID, String nodeType, String repositoryId, HashMap properties, String childAssociation) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repositoryId);
			if (childAssociation == null) {
				mcAlfrescoBaseClient.createNode(parentID, nodeType, properties);
			} else {
				mcAlfrescoBaseClient.createNode(parentID, nodeType, childAssociation, properties);
			}
		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	public void createAssociation(String fromID, String toID, String association) throws CCSessionExpiredException,
			Exception {
		try {

			getMCAlfrescoBaseClient(null).createAssociation(fromID, toID, association);
		} catch (org.apache.axis.AxisFault e) {

			if (e.getFaultReason().contains("org.apache.ws.security.WSSecurityException")) {
				throw new CCSessionExpiredException(CCConstants.CC_SESSION_EXPIRED_ID);
			} else {
				logger.error("Exception", e);
			}

		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	public HashMap getAssociationNodes(String nodeID, String repositoryId, String association) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repositoryId) ;
			return mcAlfrescoBaseClient.getAssocNode(nodeID, association);
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}
	
	/**
	 * get MCAlfrescoBaseClient instance
	 * the instance is initialized with validated authinfo of the current session
	 * 
	 * @param repositoryId
	 * @return
	 * @throws Throwable
	 */
	private MCAlfrescoBaseClient getMCAlfrescoBaseClient(String repositoryId) throws Throwable{
		return (MCAlfrescoBaseClient)RepoFactory.getInstance(repositoryId,  this.perThreadRequest.get().getSession());
	}
	
	private MCBaseClient getMCBaseClient(String repositoryId) throws Throwable{
		return (MCBaseClient)RepoFactory.getInstance(repositoryId,  this.perThreadRequest.get().getSession());
	}
	
	/**
	 * get validated authinfo of current session
	 * 
	 * @return
	 * @throws Throwable
	 */
	public HashMap<String,String> getValidatedAuthInfo(String repositoryId) throws Throwable{
		return RepoFactory.getAuthenticationToolInstance(repositoryId).validateAuthentication(this.perThreadRequest.get().getSession());
	}
	
	/**
	 * return authinfo of current session without validating
	 * 
	 * @param repId
	 * @return
	 * @throws Throwable
	 */
	public HashMap<String,String> getAuthInfo(String repId) throws Throwable{
		return RepoFactory.getAuthenticationToolInstance(repId).getAuthentication(this.perThreadRequest.get().getSession());
	}
	
	public boolean removeNodes(String[] nodeIDs, String fromID, String repositoryId)
			throws CCSessionExpiredException, Exception {
		return this.removeNodes(nodeIDs, fromID, repositoryId, true);
	}

	/**
	 * also checks for relations to other nodes and removes those
	 */
	public boolean removeNodes(String[] nodeIDs, String fromID, String repositoryId, boolean recycle)
			throws CCSessionExpiredException, Exception {
		
		logger.info("nodeIDs:" + nodeIDs + " fromID:" + fromID + " repositoryId:" + repositoryId);
		
		try {

			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repositoryId);
			HashMap<String,String> authenticationInfo = RepoFactory.getAuthenticationToolInstance(repositoryId).getAuthentication(this.perThreadRequest.get().getSession());
			
			//Helper
			HashMap<String,String> nodeIdTypeMap = new HashMap<String,String>();
			if (nodeIDs != null) {
				for (String nodeId : nodeIDs) {
					logger.info("to delete: " + nodeId);
					String nodeType = mcAlfrescoBaseClient.getNodeType(nodeId);
					nodeIdTypeMap.put(nodeId, nodeType);
				}
			}

			for (String nodeId : nodeIDs) {
				try {
					mcAlfrescoBaseClient.removeNodeAndRelations(nodeId, fromID, recycle);
				} 
				catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
					// http://forums.alfresco.com/en/viewtopic.php?f=4&t=20335&p=66165&hilit=permission+problem#p66165
					boolean success = removeNodeAdmin(nodeId, fromID, repositoryId);
					if (!success) {
						throw new CCException(CCException.NO_PERMISSIONS_TO_DELETE);
					}
				}
			}
			
			//helper
			for(Map.Entry<String,String> entry : nodeIdTypeMap.entrySet()){
				
				String deletedNodeId = entry.getKey();
				String deletedNodeTyp = entry.getValue();
				
				HashMap<String,Object> params = new HashMap<String,Object>();
				params.put(CCConstants.PARENTID, fromID);
				params.put(CCConstants.REPOSITORY_ID, repositoryId);
				HashMap<String,ArrayList<String>> helperPostDelete =  (HashMap<String,ArrayList<String>>)ApplicationContextFactory.getApplicationContext().getBean(ApplicationContextFactory.BEAN_ID_HELPER_POST_DELETE);
				
				ArrayList<String> helperClassnames = helperPostDelete.get(deletedNodeTyp);
				if(helperClassnames != null){
					for(String className : helperClassnames){
						Class clazz = Class.forName(className);
						Helper helper = (Helper)clazz.getConstructor(new Class[] { }).newInstance(new Object[] {});
						helper.execute(params, authenticationInfo);
					}
				}
			}
			return true;

		} catch (Throwable e) {
			errorHandling(e);
			return false;
		}
	}

	private boolean removeNodeAdmin(String nodeId, String fromId, String repositoryId) throws Throwable {
		
		logger.info("Alfresco Permission bug. nodeId: "
						+ nodeId
						+ " parentId:"
						+ fromId
						+ " When user gots PERMISSION_DELETE_CHILDREN on parent Node and read right on subnode and it's no primary parent subnode will be removed by admin");

		boolean hasFolderPermission = this.hasPermissions(fromId, new String[] { CCConstants.PERMISSION_DELETE_CHILDREN }, repositoryId);
		boolean hasChildPermission = this.hasPermissions(nodeId, new String[] { CCConstants.PERMISSION_READ }, repositoryId);
		
		if(!hasFolderPermission){
			return false;
		}
		
		if(!hasChildPermission){
			return false;
		}
		
		HashMap<String, HashMap<String, Object>> children = this.getChildren(fromId, repositoryId);
		try {
			for (Map.Entry<String, HashMap<String, Object>> entry : children.entrySet()) {
				if (entry.getKey().equals(nodeId)) {
					boolean isPrimaryParent = entry.getValue().get(CCConstants.CCM_PROP_PRIMARY_PARENT).equals("false") ? false : true;
					if (hasFolderPermission && hasChildPermission && !isPrimaryParent) {
						logger.info("will remove node:" + nodeId + " from:" + fromId + " by admin");
						ApplicationInfo applicationInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
						AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(applicationInfo.getAppId());
						HashMap adminAuthInfo = authTool.createNewSession(applicationInfo.getUsername(), applicationInfo.getPassword());
						MCAlfrescoBaseClient baseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(repositoryId, adminAuthInfo);

						baseClient.removeNodeAndRelations(nodeId, fromId);
					}
				}
			}
		} catch (Throwable e) {
			throw e;
		}
		return true;

	}

	public void removeRelations(String parentID) throws CCSessionExpiredException, Exception {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
			mcAlfrescoBaseClient.removeRelations(parentID);
		} catch (Throwable e) {
			errorHandling(e);
		}
	}
	
	public void removeChildrenSequentially(String parentId) throws CCException{
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
			HashMap<String, Object> parentProps = mcAlfrescoBaseClient.getProperties(parentId);
			HashMap children = (HashMap)mcAlfrescoBaseClient.getChildren(parentId);
			if (children != null) {
				for(Object setKey : children.keySet()){
					String name = (String)((HashMap)children.get(setKey)).get(CCConstants.CM_NAME);
					String type = (String)((HashMap)children.get(setKey)).get(CCConstants.NODETYPE);
					String title = (String)((HashMap)children.get(setKey)).get(CCConstants.CM_PROP_C_TITLE);
					String level1NodeId = (String)((HashMap)children.get(setKey)).get(CCConstants.SYS_PROP_NODE_UID);
					logger.info("will remove Object:"+name+" title:"+title+" type");
					if(type != null && (type.equals(CCConstants.CM_TYPE_FOLDER) || type.equals(CCConstants.CCM_TYPE_MAP))){
						logger.info("type is a folder removing childs sequentially");
						HashMap level2Objects = (HashMap)mcAlfrescoBaseClient.getChildren((String)setKey);
						for(Object level2Id : level2Objects.keySet()){
							HashMap level2ObjectProps = (HashMap)level2Objects.get(level2Id);
							String level2ObjectName = (String)level2ObjectProps.get(CCConstants.CM_NAME);
							logger.debug("  removing level2 Object:"+ level2ObjectName +" parent: "+name);
							mcAlfrescoBaseClient.removeNode((String)level2Id, (String)setKey);
						}
					}
					logger.info("remove Object:"+name+" title:"+title+" type");
					mcAlfrescoBaseClient.removeNode(level1NodeId, (String)parentProps.get(CCConstants.SYS_PROP_NODE_UID));
				}
			} else {
				logger.info("parentId:"+parentId+" has no children");
			}
		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	public boolean updateNode(String nodeID, HashMap properties, String repositoryId)
			throws CCSessionExpiredException, Exception {
		logger.info("repositoryId:" + repositoryId + " nodeID:" + nodeID);
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repositoryId);
			mcAlfrescoBaseClient.updateNode(nodeID, properties);
			return true;
		} catch (Throwable e) {
			errorHandling(e);
			return false;
		}
	}

	public void createChildAssociation(String parentID, String childID) throws CCSessionExpiredException,
			CCException, Exception {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
			mcAlfrescoBaseClient.createChildAssociation(parentID, childID);
		}  catch (org.apache.axis.AxisFault e) {
			if (e.getFaultReason().contains("org.apache.ws.security.WSSecurityException")) {
				throw new CCSessionExpiredException(CCConstants.CC_SESSION_EXPIRED_ID);
			} else {
				logger.error("Exception", e);
			}
			// for API mode other exceptions can occur
		} catch (CyclicChildRelationshipException e) {
			throw new CCException(CCException.CYCLE_CHILDRELATION, CCConstants.CC_EXCEPTION_CYCLE_CHILDRELATION);
		} catch (DuplicateChildNodeNameException e) {
			throw new CCException(CCException.DUPLICATE_CHILD, CCConstants.CC_EXCEPTION_DUPLICATE_CHILD);
		} catch (Throwable e) {
			this.errorHandling(e);
		}
	}

	public void createChildOfFavoritesFolder(String nodeID) throws CCSessionExpiredException, CCException,
			Exception {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
			String favFolder = mcAlfrescoBaseClient.getFavoritesFolder();
			mcAlfrescoBaseClient.createChildAssociation(favFolder, nodeID);
		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	/**
	 * @return list of property filename(key) and caption (value)
	 */
	public RepositoryInfo getRepositoryiesInfo(String metadataSetName) throws CCException {
		RepositoryInfo result = null;
		try {
			
			result = MCAlfrescoServiceImplExt.getRepositoryiesInfo(metadataSetName);
			
			List<String> reposToRemove = new ArrayList<String>(); 
			for(Map.Entry<String,HashMap<String,String>> repo : result.getRepInfoMap().entrySet()){
				if(ApplicationInfo.REPOSITORY_TYPE_YOUTUBE.equals(repo.getValue().get(ApplicationInfo.KEY_REPOSITORY_TYPE)) 
						&& !this.hasToolPermissions(null, CCConstants.CCM_VALUE_TOOLPERMISSION_UNCHECKEDCONTENT)){
					
					Map.Entry<String,HashMap<String,String>> repoTest = repo;					
					reposToRemove.add(repo.getKey());
				}
			}
			
			for(String removeRepoID:reposToRemove){
				logger.info("removing repo cause of youthprotection:"+removeRepoID);
				result.getRepInfoMap().remove(removeRepoID);
			}
			
			String tempdir = System.getProperty("java.io.tmpdir");
		    if (! (tempdir.endsWith("/") || tempdir.endsWith("\\"))) {
		      tempdir = tempdir + System.getProperty("file.separator");
		    }
		    
			String fileName = "repinfo_encoded.txt";
			fileName = tempdir + fileName;
			File file = new File(fileName);
			logger.info("file exsists:"+ file.getAbsolutePath()+" "+file.exists());
			if(!file.exists()){
				
				try {
					
					Class[] params = new Class[] {String.class};
					Method method = MCAlfrescoService.class.getMethod("getRepositoryiesInfo",params);
					String encodedResponse = RPC.encodeResponseForSuccess(method, result);
					//escapeBackslash
					encodedResponse = encodedResponse.replaceAll("\\\\","\\\\\\\\" );
					//escapeSingleQuotes
					encodedResponse = encodedResponse.replaceAll("'","\\\\'" );
					
					
					BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
					out.write(encodedResponse);
					out.close();
					
				}
				catch (IOException e)
				{
					errorHandling(e);
				}
			}
			
		} catch (Throwable e) {
			errorHandling(e);
		}
		return result;
	}

	public HashMap getRepositoryHome() {
		HashMap result = new HashMap();
		result.put(ApplicationInfoList.getHomeRepository().getAppId(), ApplicationInfoList.getHomeRepository().getAppCaption());
		return result;
	}

	public HashMap getISO8601DateFormat(HashMap dates) {
		Iterator iter = dates.keySet().iterator();
		HashMap returnVal = new HashMap();
		while (iter.hasNext()) {
			Object key = iter.next();
			returnVal.put(key, ISO8601DateFormat.format((Date) dates.get(key)));
		}
		return returnVal;
	}
	
	public Date getDateFromISOString(String isoDate){
		return ISO8601DateFormat.parse(isoDate);
	}
	
	public String getUserAgent() {
		return this.getThreadLocalRequest().getHeader("User-Agent");
	}

	public HashMap<String,String> authenticate(String userName, String password) throws CCException {
		
		HashMap<String,String> result = null;
		try{
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(null);
			result = authTool.createNewSession(userName, password);	
			
			//save ticket in session
			HttpSession session = this.perThreadRequest.get().getSession();
			authTool.storeAuthInfoInSession(userName, result.get(CCConstants.AUTH_TICKET), CCConstants.AUTH_TYPE_DEFAULT,session);
			
		}catch(Throwable e){
			errorHandling(e);
		}
		return result;
	}
	
	public HashMap<String,String> authenticateByGuest() throws CCException {
		
		ApplicationInfo repHomeInfo = ApplicationInfoList.getRepositoryInfo(CCConstants.REPOSITORY_FILE_HOME);
		String guestUn = repHomeInfo.getGuest_username();
		String guestPw = repHomeInfo.getGuest_password();
		
		if(guestUn == null || guestPw == null){
			throw new CCException("guest login not allowed");
		}
		
		HashMap<String,String> result = null;
		try{
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(null);
			result = authTool.createNewSession(guestUn, guestPw);
			
			//save ticket in session
			HttpSession session = this.perThreadRequest.get().getSession();
			authTool.storeAuthInfoInSession(guestUn, result.get(CCConstants.AUTH_TICKET), CCConstants.AUTH_TYPE_DEFAULT, session);
			
		}catch(Throwable e){
			errorHandling(e);
		}
		return result;
	}
	
	/**
	 * find ticket in session, authenticate with this info at repository and return userInfo
	 * 
	 * @return
	 * @throws CCException
	 */
	public HashMap<String,String> getUserInfo() throws CCException {
		try {
			String ticket = (String)this.perThreadRequest.get().getSession().getAttribute(CCConstants.AUTH_TICKET);
			if(ticket != null){
				AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(null);
				return authTool.getUserInfo(null, ticket);
			}
		} catch (Throwable e) {
			errorHandling(e);
		}
		return null;
	}
	
	/***
	 * this method should only be called once (Performance) cause it also checks
	 * and creates the linked public folder
	 */
	public String getRootNodeId() throws CCException {
		String result = null;
		try {

			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
			// get Root Node Id
			result = mcAlfrescoBaseClient.getRootNodeId();
			// check if public folder is linked
			if (result != null && !result.trim().equals("")) {
				//link public folder
				mcAlfrescoBaseClient.checkAndLinkPublicFolder(result);
			}

		} catch (Throwable e) {
			errorHandling(e);
		}
		return result;
	}

	/**
	 * @param foldername
	 * @param repId
	 * @param permissions for folder to create
	 * @param authInfo
	 * @return
	 * @throws CCException
	 */
	public String checkSystemFolderAndReturn(String foldername, String repId) throws CCException {
		HashMap<String,String> authInfo = null;
		try {
			authInfo = getValidatedAuthInfo(repId);
		} catch(Throwable e){
			this.errorHandling(e);
		}
		if (!new CheckAuthentication().isAdmin(repId, authInfo)) {
			throw new CCException(CCException.UNKNOWNEXCEPTION, "You are not an admin");
		} else {
			try {
				MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
				return mcAlfrescoBaseClient.checkSystemFolderAndReturn(foldername);
			} catch (Throwable e) {
				this.errorHandling(e);
			}
		}
		return null;
	}

	public EnvInfo getEnvInfo() throws CCException {
		try{	
			//authenticate
			this.getValidatedAuthInfo(null);
			EnvironmentService envService = (EnvironmentService)ApplicationContextFactory.getApplicationContext().getBean("environmentService");
			return envService.getEntInfo(null);
		} catch(Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}

	public GetPermissions getPermissions(String nodeId) throws CCSessionExpiredException, CCException {
		return this.getPermissions(nodeId, ApplicationInfoList.getHomeRepository().getAppId());
	}

	public GetPermissions getPermissions(String nodeId, String repositoryId) throws CCException {
		try {
			MCAlfrescoBaseClient baseClient = getMCAlfrescoBaseClient(repositoryId);
			return this.getPermissions(nodeId,repositoryId, baseClient);
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}

	private GetPermissions getPermissions(String nodeId, String repositoryId, MCAlfrescoBaseClient mcAlfrescoBaseClient) throws CCException {
		GetPermissions result = null;
		try {
			ACL permissions = mcAlfrescoBaseClient.getPermissions(nodeId);
			List<AssignedLicense> assignedLicensesList = new AssignedLicenseService().getAssignedLicenses(repositoryId, nodeId);
			AssignedLicense[] assignedLicense = assignedLicensesList.toArray(new AssignedLicense[assignedLicensesList.size()]);
			result = new GetPermissions(permissions, assignedLicense);
		} catch (Throwable e) {
			logger.error(e);
			errorHandling(e);
		}
		return result;
	}

	private void errorHandling(Throwable e) throws CCException {
		
		logger.error("Ther is an Error" + e.getClass());
		e.printStackTrace();
		
		if (e instanceof org.apache.axis.AxisFault) {

			org.apache.axis.AxisFault axisFault = (org.apache.axis.AxisFault) e;

			// ((org.apache.axis.AxisFault)e).g
			logger.info("e is Axis Fault cause:" + e.getCause());
			logger.info("e is Axis Fault string:" + ((org.apache.axis.AxisFault) e).getFaultString());
			logger.info("e is Axis Fault reason:" + axisFault.getFaultReason());
			if (axisFault.getFaultReason().contains("org.apache.ws.security.WSSecurityException")) {
				throw new CCSessionExpiredException("Ticket nicht mehr gültig. " + CCConstants.CC_SESSION_EXPIRED_ID);
			}
			for (org.w3c.dom.Element ele : axisFault.getFaultDetails()) {
				logger.info(ele.getNodeName());
				logger.info("  " + ele.getNodeValue());

				if (ele.getNodeName().equals("ns1:RepositoryFault")) {
					errorEle(ele, "");
					org.w3c.dom.Node nodeMessage = getW3cChildNode(ele, "ns1:message");
					logger.info("nodeMessage:" + nodeMessage);
					if (nodeMessage != null) {
						org.w3c.dom.Node textNode = getW3cChildNode(nodeMessage, "#text");
						logger.info("textNode:" + textNode);
						if (textNode != null) {
							e.printStackTrace();
							logger.info("I'll throw an new CCEXCEPTION!!!!");
							throw new CCException(CCException.UNKNOWNEXCEPTION, textNode.getNodeValue());
						}
					}
				}

				if (ele.getNodeName().equals("faultData")) {
					e.printStackTrace();
					throw new CCException(CCException.UNKNOWNEXCEPTION, ele.getTextContent());
				}

			}
			e.printStackTrace();
			throw new CCException(CCException.UNKNOWNEXCEPTION, axisFault.dumpToString());
		}

		// for the Reflection stuff
		if (e instanceof java.lang.reflect.InvocationTargetException) {
			logger.info("e is InvocationTargetException cause" + e.getCause().getClass().getName());
			if (e.getCause() != null) {
				errorHandling((Throwable) e.getCause());
			}
		}

		if (e instanceof org.alfresco.repo.security.authentication.AuthenticationException) {
			if (e.getMessage() != null && e.getMessage().contains("Missing ticket for TICKET")) {
				throw new CCSessionExpiredException("Ticket nicht mehr gültig. " + e.getMessage());
			}else if(e.getMessage() != null && e.getMessage().contains("Bad credentials presented")){
				throw new CCException(CCException.AUTHENTIFICATIONFAILED, "Login fehlgeschlagen");
			}
			else {
				throw new CCException(CCException.AUTHENTIFICATIONEXCEPTION, e.getMessage());
			}
		}
		
		if (e instanceof org.alfresco.repo.security.permissions.AccessDeniedException) {
			throw new CCException(CCException.ACCESS_DENIED_EXCEPTION, e.getMessage());
		}
		
		if (e instanceof QueryValidationFailedException) {
			
			QueryValidationFailedException queryValidationFailedException = (QueryValidationFailedException)e;
			
			String id = "validation failed";
			
			if(queryValidationFailedException.getValidator() != null){
				id = queryValidationFailedException.getValidator().getMessageId();
			}
			
			CCException clientException = new CCException(id);
			String locale = (String)this.getThreadLocalRequest().getSession().getAttribute(CCConstants.AUTH_LOCALE);
			if(locale == null) locale="default";
			if(queryValidationFailedException.getMdsqp() != null && queryValidationFailedException.getMdsqp().getLabel() != null){
				clientException.setMessageParam("label", queryValidationFailedException.getMdsqp().getLabel().getValue(locale));
				logger.info("queryValidationFailedException.getMdsqp().getLabel(): "+queryValidationFailedException.getMdsqp().getLabel().getKey() );
			}
			
			clientException.setMessageParam("value",queryValidationFailedException.getValue());
			throw clientException;
		}
		
		/**
		 * Share Service Exceptions
		 */
		
		if (e instanceof EMailSendFailedException) {
			logger.error(e.getMessage(),e);
			throw new CCException(CCException.SHARE_SERVICE_EMAILSENDFAILED);
		}
		
		if (e instanceof EMailValidationException) {
			throw new CCException(CCException.SHARE_SERVICE_EMAILVALIDATIONFAILED);
		}
		
		if (e instanceof ExpiryDateValidationException) {
			throw new CCException(CCException.SHARE_SERVICE_EXPIRYDATETOOLD);
		}
		
		if (e instanceof NodeDoesNotExsistException) {
			throw new CCException(CCException.SHARE_SERVICE_NODEDOESNOTEXSIST);
		}
		
		if (e instanceof PermissionFailedException) {
			throw new CCException(CCException.SHARE_SERVICE_NOPERMISSIONS);
		}
		
		if (e instanceof org.apache.lucene.search.BooleanQuery.TooManyClauses) {
			throw new CCException(CCException.LUCENE_TO_MANY_CLAUSES);
		}
		
		if (e instanceof IllegalStateException) {
			if (e.getMessage() != null && e.getMessage().contains("Can not delete from this acl in a node context SHARED")) {
				throw new CCException(CCException.REMOVE_PERMISSION_INHERIT);
			}
		}

		if (e.getCause() != null && e.getCause() instanceof CCException) {
			throw (CCException) e.getCause();
		}
		
		if (e instanceof CCException) {
			throw (CCException)e;
		}

		// e.printStackTrace();
		logger.error(e.getMessage(), e);
		throw new CCException(CCException.UNKNOWNEXCEPTION, e.getMessage());
	}

	private org.w3c.dom.Node getW3cChildNode(org.w3c.dom.Node node, String childName) {
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			if (node.getChildNodes().item(i).getNodeName().equals(childName)) {
				return node.getChildNodes().item(i);
			}
		}
		return null;
	}

	private void errorEle(org.w3c.dom.Node node, String space) {
		
		space = space + " ";
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			logger.info(space + "childNode:" + node.getChildNodes().item(i).getNodeName());
			logger.info(space + "childNodeVal:" + node.getChildNodes().item(i).getNodeValue());

			errorEle(node.getChildNodes().item(i), space);
		}
	}

	/**
	 * this method is used to set and remove Permissions for a set of
	 * authorities
	 * 
	 * @param ioNodeId
	 * @param ArrayList <PermissionContainer> permissionContainers
	 * @param authenticationInfo
	 */
	public void setPermissions(SetPermissions setPermissions) throws CCException {

		HashMap<String,String> authenticationInfo = null;
		try {
			authenticationInfo = getValidatedAuthInfo(null);
			
		} catch (Throwable e) {
			this.errorHandling(e);
			return;
		}
		
		// for all users
		logger.debug("permContainer:" + setPermissions.getPermissionContainers());
		for (PermissionContainer permCont : setPermissions.getPermissionContainers()) {
			logger.debug("permCont.getAuthorityType():" + permCont.getAuthorityType());
			if (permCont.getAuthorityType().equals(CCConstants.PERM_AUTHORITY_TYPE_USER)) {
				
				//cause the search only delivers local users(even shadow ones) we don't need to check and create remote users here
				//HashMap<String, String> userProps = checkAndCreateShadowUser(permCont.getAuthorityProps(), authenticationInfo);
				HashMap<String, String> userProps  = null;
				try{
					MCAlfrescoBaseClient baseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(null, authenticationInfo);
					userProps = baseClient.getUserInfo((String)permCont.getAuthorityProps().get(CCConstants.PROP_USERNAME));
				}catch(Throwable e){
					this.errorHandling(e);
					return;
				}
				
				logger.debug("userProps:" + userProps);
				if (userProps != null && userProps.get(CCConstants.PROP_USERNAME) != null
						&& userProps.get(CCConstants.PROP_USERNAME).equals(permCont.getAuthorityName())) {
					logger.info("AUTHORITY:" + permCont.getAuthorityName());
					logger.info("   SET Permissions:");

					// first remove than set this is necessary for Permissionpanel that 
					// removes all old permissions and add's the new one
					logger.info("   REMOVE Permissions:" + permCont.getPermissionsToRemove());
					for (String perm : permCont.getPermissionsToRemove()) {
						logger.info("     " + perm);
					}
					removePermissions(setPermissions.getNodeId(), permCont.getAuthorityName(), permCont.getPermissionsToRemove());
					for (String perm : permCont.getPermissionsToSet()) {
						logger.info("     " + perm);
					}
					setPermissions(setPermissions.getNodeId(), permCont.getAuthorityName(), permCont.getPermissionsToSet(), setPermissions.getInherit());

				} else {
					removePermissions(setPermissions.getNodeId(), permCont.getAuthorityName(), permCont.getPermissionsToRemove());
				}
			} else {
				// first remove than set the new
				removePermissions(setPermissions.getNodeId(), permCont.getAuthorityName(), permCont.getPermissionsToRemove());
				setPermissions(setPermissions.getNodeId(), permCont.getAuthorityName(), permCont.getPermissionsToSet(), setPermissions.getInherit());
			}
		}

		// set licenses
		setLicenses(setPermissions.getNodeId(), setPermissions.getAssignedLicenses(), authenticationInfo);
		/**
		 * @TODO make this hole permission stuff run inside an transaction
		 */
		try{
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			permissionService.createNotifyObject(setPermissions.getNodeId(), authenticationInfo.get(CCConstants.AUTH_USERNAME), CCConstants.CCM_VALUE_NOTIFY_EVENT_PERMISSION, CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_CHANGE);
		}catch(Throwable e){
			this.errorHandling(e);
		}
	}
	
	public void setPermissions(String repositoryid, String nodeId, ACE[] aces) throws CCException {
		try{
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(repositoryid);
			permissionService.setPermissions(nodeId, Arrays.asList(aces));
			
			HashMap<String,String> authenticationInfo = null;
			try{
				authenticationInfo = getValidatedAuthInfo(null);
			}catch(Throwable e){
				this.errorHandling(e);
				return;
			}

			HashMap<String,AssignedLicense> alicenses = new HashMap<String,AssignedLicense>();
			for(ACE ace : aces){
				AssignedLicense licenses = alicenses.get(ace.getAuthority());
				if (licenses == null) {
					String[] license =  CCConstants.PERMISSION_CC_PUBLISH.equals(ace.getPermission()) ? new String[]{CCConstants.COMMON_LICENSE_EDU_NC} : new String[]{CCConstants.COMMON_LICENSE_EDU_P_NR};
					licenses = new AssignedLicense(nodeId, ace.getAuthority(), license);
				} else {
					licenses.setLicenses(CCConstants.PERMISSION_CC_PUBLISH.equals(ace.getPermission()) ? new String[]{CCConstants.COMMON_LICENSE_EDU_NC} : new String[]{CCConstants.COMMON_LICENSE_EDU_P_NR});
				}
				alicenses.put(ace.getAuthority(), licenses);
			}
			
			// set licenses
			setLicenses(nodeId, alicenses.values().toArray(new AssignedLicense[alicenses.values().size()]), authenticationInfo);
			/**
			 * @TODO make this hole permission stuff run inside an transaction
			 */
			permissionService.createNotifyObject(nodeId, authenticationInfo.get(CCConstants.AUTH_USERNAME), CCConstants.CCM_VALUE_NOTIFY_EVENT_PERMISSION, CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_CHANGE);
			
		} catch(Throwable e){
			this.errorHandling(e);
		}
		
	}
	
	public void setPermissionsInherit(String nodeId, boolean inheritPermission) throws CCSessionExpiredException, CCException {
		this.setPermissions(nodeId, null, null, inheritPermission);
		/**
		 * @TODO make this hole permission stuff run inside an transaction
		 */
		try {
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			permissionService.createNotifyObject(nodeId, getValidatedAuthInfo(null).get(CCConstants.AUTH_USERNAME), CCConstants.CCM_VALUE_NOTIFY_EVENT_PERMISSION, CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_CHANGE_INHERIT);
		} catch (Throwable e) {
			this.errorHandling(e);
		}
	}

	private void setPermissions(String nodeId, String _authority, String[] permissions, boolean inheritPermission) throws CCSessionExpiredException, CCException {
		try {
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			permissionService.setPermissions(nodeId, _authority, permissions, new Boolean(inheritPermission));
		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	public void removePermissions(String nodeId, String _authority, String[] permissions)
			throws CCSessionExpiredException, CCException {
		try {
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			permissionService.removePermissions(nodeId, _authority, permissions);
		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	private void setLicenses(String nodeId, AssignedLicense[] assignedLicenses, HashMap<String, String> authenticationInfo) throws CCException {
		try {
			
			//TODO: authenticate - change to interface AuthenticationTool when implementing invite and license setting in remote repositories
			new AuthenticationToolAPI().getUserInfo(authenticationInfo.get(CCConstants.AUTH_USERNAME), authenticationInfo.get(CCConstants.AUTH_TICKET));
			
			//only allow licenses to be assigned to io's 
			if (assignedLicenses != null) {
				String nodeType = new MCAlfrescoAPIClient().getType(nodeId);
				if (CCConstants.CCM_TYPE_IO.equals(nodeType)) {
					new AssignedLicenseService().setAssignedLicenses(null, nodeId,Arrays.asList(assignedLicenses));
				} else {
					logger.error("not allowed to assign licenses to:"+nodeType);
				}
			}
			
		} catch(Throwable e) {
			this.errorHandling(e);
		}
	}
	
	public void requestForPublishPermission(String repositoryId, String nodeId, String message) throws CCException{
		try {
			
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repositoryId);
			HashMap<String,Object> props = mcAlfrescoBaseClient.getProperties(nodeId);
			String questionsAllowed = (String)props.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED);
			String creator = (String)props.get(CCConstants.CM_PROP_C_CREATOR);
			String title = (String)props.get(CCConstants.LOM_PROP_GENERAL_TITLE);
			if(title == null || title.trim().equals("")) title = (String)props.get(CCConstants.CM_NAME);
			
			HashMap<String, String> creatorProps = mcAlfrescoBaseClient.getUserInfo(creator);
			if(!new Boolean(questionsAllowed)){
				throw new CCException(null,"No requests allowed!");
			}
			
			if(creatorProps == null){
				logger.error("no props found for creator:"+creator+" of nodeId:"+nodeId);
				throw new CCException(null,"can not find out creator");
			}
			
			String receiver = creatorProps.get(CCConstants.CM_PROP_PERSON_EMAIL);
			
			EmailValidator mailValidator = EmailValidator.getInstance();
			if(!mailValidator.isValid(receiver)){
				logger.error("no valid email found for creator:"+creator+" of nodeId:"+nodeId);
				throw new CCException(null,"no valid email found for creator");
			}
			
			HashMap<String,String> authenticationInfo = getValidatedAuthInfo(null);
			HashMap<String,String> senderProps = mcAlfrescoBaseClient.getUserInfo(authenticationInfo.get(CCConstants.AUTH_USERNAME));
			String senderSN = senderProps.get(CCConstants.CM_PROP_PERSON_LASTNAME);
			String senderGN = senderProps.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
			String senderEmail = senderProps.get(CCConstants.CM_PROP_PERSON_EMAIL);
			
			String currentLocale = (String)this.perThreadRequest.get().getSession().getAttribute(CCConstants.AUTH_LOCALE);
			if(currentLocale == null || currentLocale.trim().equals("")) currentLocale = "en_EN";
			String mailText = I18nServer.getTranslationDefaultResourcebundle("dialog_request_publish_permission", currentLocale);
			mailText = mailText.replace("{user}", senderGN + " "+senderSN+ "("+senderEmail+")");
			mailText = mailText.replace("{title}", title);
			
			repositoryId = (repositoryId == null) ? ApplicationInfoList.getHomeRepository().getAppId() : repositoryId;
			mailText = mailText.replace("{link}", URLTool.getUploadFormLink(repositoryId,nodeId));
			
			if(message != null && !message.trim().equals("")){
				String messageText =  I18nServer.getTranslationDefaultResourcebundle("dialog_inviteusers_mailtext_usermessage", currentLocale);
				messageText = messageText.replace("{user}", senderGN + " "+senderSN);
				mailText+= "\n\n" + messageText;
				mailText+= "\n\n" + message;
			}
			
			String mailSubject = I18nServer.getTranslationDefaultResourcebundle("dialog_request_publish_permission_subject", currentLocale);
			mailSubject = mailSubject.replace("{title}", title);
			
			Mail mail = new Mail();
			mail.sendMail(receiver, mailSubject, mailText);
			
		} catch(Throwable e) {
			this.errorHandling(e);
		}
	}

	public HashMap<String, Object> getNode(String nodeId) throws CCSessionExpiredException, CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(null);
			return mcAlfrescoBaseClient.getProperties(nodeId);
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}

	public HashMap<String, Object> getNode(String nodeId, String repositoryId) throws CCException {
		logger.info("nodeId:" + nodeId + " repositoryId:" + repositoryId);
		try {
			MCBaseClient mcAlfrescoBaseClient = getMCBaseClient(repositoryId);
			return mcAlfrescoBaseClient.getProperties(nodeId);
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}
	
	public HashMap<String, Object> getNode(String nodeId, String repositoryId, String version) throws CCException {
		logger.info("nodeId:" + nodeId + " repositoryId:" + repositoryId+" version:"+version);
		try {
			
			MCBaseClient mcAlfrescoBaseClient = getMCBaseClient(repositoryId);
			if (!(mcAlfrescoBaseClient instanceof MCAlfrescoBaseClient)) {
				return null;
			}
			if (version != null && !version.trim().equals("")) {
				
				HashMap<String, HashMap<String,Object>> versionHistory = ((MCAlfrescoBaseClient)mcAlfrescoBaseClient).getVersionHistory(nodeId);
				
				if (versionHistory != null) {
					for (Map.Entry<String, HashMap<String,Object>> map : versionHistory.entrySet()) {
						String v = (String)map.getValue().get(CCConstants.CM_PROP_VERSIONABLELABEL);
						if (version.equals(v)) {
							return map.getValue();
						}
					}
				}
				
			}
			
			return mcAlfrescoBaseClient.getProperties(nodeId);
						
		} catch (Throwable e) {
			errorHandling(e);
			return null;
		}
	}
	
	public HashMap<String, HashMap<String,Object>> getNodes(String[] nodeIds, String repositoryId) throws CCException {
		HashMap<String, HashMap<String,Object>> result = new HashMap<String, HashMap<String,Object>>();
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repositoryId);
			for(String nodeId:nodeIds){
				result.put(nodeId, mcAlfrescoBaseClient.getProperties(nodeId));
			}
		} catch (Throwable e) {
			errorHandling(e);
		}
		return result;
	}

	public void move(String fromParentId, String parentId, String nodeId, String repositoryId) throws CCSessionExpiredException,
			CCException {

		logger.info("fromParentId:"+fromParentId+" parentId:"+parentId+" repositoryId:"+repositoryId);
		if(fromParentId == null){
			String error = "fromParentId is null, can not determine if it's a linked object or not. will not move!";
			logger.error(error);
			throw new CCException(null,error);
		}
		try {

			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repositoryId);
			
			HashMap<String, HashMap> parentNodes = mcAlfrescoBaseClient.getParents(nodeId, true);
			HashMap primaryParent = parentNodes.get(parentNodes.keySet().iterator().next());
			if(primaryParent.get(CCConstants.SYS_PROP_NODE_UID).equals(fromParentId)){
				logger.info("its a object move operation");
				mcAlfrescoBaseClient.moveNode(parentId, CCConstants.CM_ASSOC_FOLDER_CONTAINS, nodeId);
			}else{
				logger.info("its a link move operation");
				mcAlfrescoBaseClient.removeNode(nodeId, fromParentId);
				mcAlfrescoBaseClient.createChildAssociation(parentId, nodeId);
			}
			
		} catch (Throwable e) {
			errorHandling(e);
		}
	}
	
	public void copy(String repositoryId, String nodeId, String toNodeId) throws CCException{
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repositoryId);
			String nodeType = mcAlfrescoBaseClient.getNodeType(nodeId);
			boolean copyChildren = false;
			if(nodeType.equals(CCConstants.CM_TYPE_FOLDER) || nodeType.equals(CCConstants.CCM_TYPE_MAP)){
				copyChildren = true;
			}
			mcAlfrescoBaseClient.copyNode(nodeId, toNodeId, copyChildren);
		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	public Group getEduGroupContextOfNode(String nodeId) throws CCException {
		try {				
			// get base client instance
			MCAlfrescoBaseClient baseClient = getMCAlfrescoBaseClient(null);
			return baseClient.getEduGroupContextOfNode(nodeId);
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}		
	}
	
	public Result<List<User>> findUsers(String query, List<String> searchFields, boolean globalContext, int from, int nrOfResults) throws CCException {
		try{
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			return permissionService.findUsers(query, searchFields, globalContext, from, nrOfResults);
		}catch(Throwable e){
			this.errorHandling(e);
		}
		return null;
	}
	
	public  Result<List<Group>> findGroups(String _toSearch, boolean globalContext, int from, int nrOfResults) throws CCException {
		try{
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			return permissionService.findGroups(_toSearch, globalContext, from, nrOfResults);
		}catch(Throwable e){
			this.errorHandling(e);
		}
		return null;
	}
	
	public  Result<List<Authority>> findAuthorities(String _toSearch, boolean globalContext, int from, int nrOfResults) throws CCException {
		try{
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
			return permissionService.findAuthorities(_toSearch, globalContext, from, nrOfResults);
		}catch(Throwable e){
			this.errorHandling(e);
		}
		return null;
	}
	
	public HashMap<String, HashMap<String, Object>> processEduGroupCommand(String propName) throws CCException {
		try {
			MCAlfrescoBaseClient baseClient = getMCAlfrescoBaseClient(null);
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(null);
			if(baseClient.isAdmin(authTool.getAuthentication(this.perThreadRequest.get().getSession()).get(CCConstants.AUTH_USERNAME))){
				EduGroupTool.processEduGroupMicroCommand(propName);
			}else throw new Exception("no admin");
		} catch (Throwable e) {
			this.errorHandling(e);
		}		
		return new HashMap<String, HashMap<String, Object>>();
	}	

	public HashMap<String, HashMap<String,Object>> getVersionHistory(String nodeId, String repId) throws CCException {
		HashMap<String, HashMap<String,Object>> result = null;
		try {
			MCAlfrescoBaseClient baseClient = getMCAlfrescoBaseClient(repId);
			result = baseClient.getVersionHistory(nodeId);
		} catch (Throwable e) {
			errorHandling(e);
		}
		return result;
	}

	/**
	 * @param nodeId
	 * @param versLbl
	 * @param repId
	 * @throws CCException
	 */
	public void revertVersion(String nodeId, String versLbl, String repId) throws CCException {
		try {
			MCAlfrescoBaseClient baseClient = getMCAlfrescoBaseClient(repId);
			baseClient.revertVersion(nodeId, versLbl);
		} catch (Throwable e) {
			errorHandling(e);
		}
	}

	public ArrayList<HashMap<String, Object>> getVCardAsMap(String vcard) throws CCException {
		try {
			return VCardConverter.vcardToHashMap(vcard);
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}

	private static ArrayList<String> allowedPropFiles = null;

	ArrayList<String> getAllowedPropFiles() {
		if (allowedPropFiles == null) {
			allowedPropFiles = new ArrayList<String>();
			allowedPropFiles.add("ddc.properties");
		}
		return allowedPropFiles;
	}

	public HashMap<String, String> getPropFileContent(String propFile) {

		String teststr = getAllowedPropFiles().get(0).trim();
		propFile = propFile.trim();

		boolean test = teststr.equals(propFile);

		if (!getAllowedPropFiles().contains(propFile))
			return null;

		HashMap<String, String> result = new HashMap<String, String>();
		try {
			Properties props = PropertiesHelper.getProperties(propFile, PropertiesHelper.TEXT);
			for (Object key : props.keySet()) {
				result.put((String) key, (String) props.get(key));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	public void removeUserDefinedPreview(String nodeId, String repId) throws CCException {
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repId);
		if (appInfo != null) {
			try {
				MCAlfrescoBaseClient baseClient = getMCAlfrescoBaseClient(repId);
				baseClient.removeUserDefinedPreview(nodeId);
			} catch (Throwable e) {
				errorHandling(e);
			}
		}
	}

	public boolean hasPermissions(String nodeId, String[] permissions, String repId) throws CCException {
		ArrayList<String> alfPerm = new ArrayList<String>();
		try {
			
			MCBaseClient mcAlfrescoBaseClient = RepoFactory.getInstance(repId, getValidatedAuthInfo(ApplicationInfoList.getHomeRepository().getAppId()));
			Boolean result = mcAlfrescoBaseClient.hasPermissions(nodeId, permissions);

			return result;

		} catch (Throwable e) {
			e.printStackTrace();
			errorHandling(e);
		}

		return false;
	}

	public HashMap<String, Boolean> hasAllPermissions(String nodeId, String[] permissions, String repId) throws CCException {
		if (nodeId == null) return null;
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			HashMap<String, Boolean> result = mcAlfrescoBaseClient.hasAllPermissions(nodeId, permissions);
			return result;
		} catch (Throwable e) {
			errorHandling(e);
		}
		return null;
	}

	public HashMap<String, Boolean> hasAllPermissions(String nodeId, String authority, String[] permissions, String repId) throws CCException {
		logger.info("starting nodeId:" + nodeId + " authority:" + authority + " repId:" + repId);
		if (nodeId == null) return null;
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			HashMap<String, Boolean> result = mcAlfrescoBaseClient.hasAllPermissions(nodeId, authority, permissions);
			return result;
		} catch (Throwable e) {
			errorHandling(e);
		}
		return null;
	}

	@Override
	public ArrayList<Authority> getAllAuthoritiesHavingPermission(
			final String nodeId,
			final String permission, 
			final String repId) throws CCException {
		
		if (! this.hasPermissions(
				nodeId, 
				new String[] { CCConstants.PERMISSION_CHANGEPERMISSIONS }, 
				repId)) {
			
			return null;
		}
		
		if (! this.hasToolPermissions(
				repId, 
				CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE)) {
			
			return null;
		}
		
		Set<Authority> authorities = new HashSet<Authority>();
		
		for (ACE ace : this.getPermissions(nodeId, repId).getPermissions().getAces()) {
			
			if (this.hasAllPermissions(nodeId, ace.getAuthority(), new String[] {permission}, repId)
					.get(permission)) {
				
				if ("USER".equals(ace.getAuthorityType())) {
					
					authorities.add(ace.getUser());
					
				} else if ("GROUP".equals(ace.getAuthorityType())) {

					authorities.add(ace.getGroup());

				} else if ("OWNER".equals(ace.getAuthorityType())) {

					authorities.add(new Owner());

				} else if ("EVERYONE".equals(ace.getAuthorityType())) {

					authorities.add(new Everyone());

				} 				
			}
		}

		ArrayList<Authority> result = new ArrayList<Authority>();
		result.addAll(authorities);
		
		return result;
	}

	
	public HashMap<String, HashMap<String, Object>> getUsages(String nodeId, String repId) throws CCException {
		HashMap<String, HashMap<String, Object>> result = null;

		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			result = mcAlfrescoBaseClient.getChildrenByType(nodeId, CCConstants.CCM_TYPE_USAGE);

		} catch (Throwable e) {
			errorHandling(e);
		}
		return result;
	}
	
	public Boolean isUsed(String nodeId) throws CCException{
		
		try {
			MCAlfrescoBaseClient client = getMCAlfrescoBaseClient(null);
			String[] aspects = (String[])client.getAspects(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), nodeId);
			
			HashMap<String, HashMap<String, Object>> usages = this.getUsages(nodeId, null);
			if(usages != null && usages.size() >  0){
				return true;
			}
			
			if(!CCConstants.CCM_TYPE_IO.equals(client.getNodeType(nodeId)) 
					|| Arrays.asList(aspects).contains(CCConstants.CCM_ASPECT_COLLECTION) 
					|| Arrays.asList(aspects).contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)){
				return false;
			}
			
			HashMap<String, HashMap<String, Object>> refs = client.search("@ccm\\:original:\""+nodeId +"\"");
			if(refs.size() > 1){
				return true;
			}
		} catch (Throwable e) {
			errorHandling(e);
		}
		
		return false;
	}

	public boolean isAdmin(String repId) throws CCException {
		try {
			return new CheckAuthentication().isAdmin(repId, getValidatedAuthInfo(repId));
		} catch(Throwable e) {
			this.errorHandling(e);
			return false;
		}
	}

	public boolean isGuest(String repId) throws CCException {
		try {
			
			ApplicationInfo appInfo = (repId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(repId);
		
			String guest = appInfo.getGuest_username();
			if (guest != null && guest.equals(getValidatedAuthInfo(repId).get(CCConstants.AUTH_USERNAME))) {
				return true;
			}
			
		} catch (Throwable e) {
			errorHandling(e);
		}
		return false;
	}
	
	static public void main(String[] args) {

		Locale list[] = SimpleDateFormat.getAvailableLocales();
		List localeList = Arrays.asList(list);

		Comparator<Locale> comp = new Comparator<Locale>() {

			public int compare(Locale o1, Locale o2) {
				if (o1 == null && o2 == null) {
					return 0;
				}
				if (o1 == null) {
					return 1;
				}
				if (o2 == null) {
					return -1;
				}
				return o1.toString().compareTo(o2.toString());
			}
		};
		Collections.sort(localeList, comp);
		
		for (Object objLocale : localeList) {
			Locale locale = (Locale) objLocale;
		}
	}

	public void refreshApplicationInfo() throws CCException {
		try {
			HashMap<String,String> authInfo = getValidatedAuthInfo(null);
			if (new CheckAuthentication().isAdmin(null, authInfo)) {
				ApplicationInfoList.refresh();
				RepoFactory.refresh();
			} else {
				throw new Exception("That was not an admin");
			}
		} catch (Throwable e) {
			this.errorHandling(e);
		}
	}
	
	public void runOAIPMHLOMImporter(ArrayList<String> setsParam, String oaiBaseUrl, String metadataSetId, String metadataPrefix, String importerClassName) throws CCException {
		try{
			
			HashMap authInfo = getValidatedAuthInfo(null);
			
			if(!new CheckAuthentication().isAdmin(null, authInfo)){
				throw new CCException(null,"you are not an admin");
			}
						
			HashMap<String,Object> paramsMap = new HashMap<String,Object>();
			paramsMap.put(JobHandler.AUTH_INFO_KEY, authInfo);
			paramsMap.put("sets", setsParam);
			if(oaiBaseUrl != null && !oaiBaseUrl.trim().equals("")){
				paramsMap.put(OAIConst.PARAM_OAI_BASE_URL, oaiBaseUrl);
			}
			if(metadataSetId != null && !metadataSetId.trim().equals("")){
				paramsMap.put(OAIConst.PARAM_METADATASET_ID, metadataSetId);
			}
			
			//metadataPrefix
			if(metadataPrefix != null && !metadataPrefix.trim().equals("")){
				paramsMap.put(OAIConst.PARAM_OAI_METADATA_PREFIX,metadataPrefix);
			}
			
			Class importerClass = null;
			for(JobConfig jobConfig : JobHandler.getInstance().getJobConfigList()){
				if(jobConfig.getJobClass().getName().equals(importerClassName)){
					importerClass = jobConfig.getJobClass();
				}
			}
			
			if(importerClass == null){
				throw new Exception("no Importer Jobclass found for" + importerClassName);
			}
			
			ImmediateJobListener jobListener = JobHandler.getInstance().startJob(importerClass,paramsMap);
			if(jobListener.isVetoed()){
				throw new Exception("job was vetoed by "+jobListener.getVetoBy());
			}
			
		} catch(Throwable e) {
			this.errorHandling(e);
		}
	}
	
	public ArrayList<String> getImporterJobList() throws CCException {
		ArrayList<String> result = new ArrayList<String>();
		try{
			if(!new CheckAuthentication().isAdmin(null, getValidatedAuthInfo(null))){
				throw new CCException(null,"you are not an admin");
			}
			List<JobConfig> jcl = JobHandler.getInstance().getJobConfigList();
			Class importerBaseClass = org.edu_sharing.repository.server.jobs.quartz.ImporterJob.class;
			for(JobConfig jc: jcl){
				
				if(jc.getJobClass().equals(importerBaseClass) || jc.getJobClass().getSuperclass().equals(importerBaseClass)){
					if(!result.contains(jc.getJobClass().getName())) result.add(jc.getJobClass().getName());
				}
			}
		}catch(Throwable e){
			errorHandling(e);
		}
		return result;
	}
	
	public void removeOAIImportedObjects() throws CCException {
		try {
			
			HashMap authInfo = getValidatedAuthInfo(null);
			if (!new CheckAuthentication().isAdmin(null, authInfo)) {
				throw new CCException(null,"you are not an admin");
			}
			
			HashMap<String,Object> paramsMap = new HashMap<String,Object>();
			paramsMap.put(JobHandler.AUTH_INFO_KEY, authInfo);
			ImmediateJobListener jobListener = JobHandler.getInstance().startJob(org.edu_sharing.repository.server.jobs.quartz.RemoveImportedObjectsJob.class,paramsMap);
			if (jobListener.isVetoed()) {
				throw new Exception("job was vetoed by "+jobListener.getVetoBy());
			}
			
		} catch (Throwable e) {
			this.errorHandling(e);
		}
	}
	
	Scheduler quartzScheduler = null;
	
	public void startCacheRefreshingJob(String[] params) throws CCException {
		
			try {
				
				HashMap authInfo = getValidatedAuthInfo(null);
				if (!new CheckAuthentication().isAdmin(null, authInfo)) {
					throw new CCException(null,"you are not an admin");
				}
				
				HashMap<String,Object> paramsMap = new HashMap<String,Object>();
				if (params != null && params.length > 0) {
					paramsMap.put("rootFolderId", params[0]);
					paramsMap.put("sticky", params[1]);
				}
				paramsMap.put(JobHandler.AUTH_INFO_KEY, authInfo);
				ImmediateJobListener jobListener = JobHandler.getInstance().startJob(org.edu_sharing.repository.server.jobs.quartz.RefreshCacheJob.class, paramsMap);
				
				if (jobListener.isVetoed()) {
					throw new Exception("job was vetoed by "+jobListener.getVetoBy());
				}
				
			} catch(Throwable e) {
				this.errorHandling(e);
			}
	}
	
	public void removeDeletedImports(String oaiBaseUrl, String cataloges, String oaiMetadataPrefix) throws CCException {
		try {
			
			HashMap authInfo = getValidatedAuthInfo(null);
			if (!new CheckAuthentication().isAdmin(null, authInfo)) {
				throw new CCException(null,"you are not an admin");
			}
			
			HashMap<String,Object> paramsMap = new HashMap<String,Object>();
			paramsMap.put(JobHandler.AUTH_INFO_KEY, authInfo);
			paramsMap.put(OAIConst.PARAM_OAI_BASE_URL, oaiBaseUrl);
			paramsMap.put(OAIConst.PARAM_OAI_CATALOG_IDS, cataloges);
			paramsMap.put(OAIConst.PARAM_OAI_METADATA_PREFIX, oaiMetadataPrefix);
			
			ImmediateJobListener jobListener = JobHandler.getInstance().startJob(org.edu_sharing.repository.server.jobs.quartz.RemoveDeletedImportsJob.class, paramsMap);
			
			if (jobListener.isVetoed()) {
				throw new Exception("job was vetoed by "+jobListener.getVetoBy());
			}
			
		} catch(Throwable e) {
			this.errorHandling(e);
		}
	}

	public ArrayList<String> getAllValuesFor(String property) throws CCException{
		try {
			HashMap<String,String> authInfo = getValidatedAuthInfo(null);
			if(!new CheckAuthentication().isAdmin(null,authInfo )){
				throw new CCException(null,"you are not an admin");
			}
			return AdminServiceFactory.getInstance().getAllValuesFor(property, authInfo);

		} catch (Throwable e) {
			errorHandling(e);
			return new ArrayList<String>();
		}
	}
	
	public void writePublisherToMDSXml(String vcardProp) throws CCException{
		try{
			HashMap authInfo = getValidatedAuthInfo(null);
			if(!new CheckAuthentication().isAdmin(null, authInfo)){
				throw new CCException(null,"you are not an admin");
			}
			HashMap<String,Object> paramsMap = new HashMap<String,Object>();
			paramsMap.put(JobHandler.AUTH_INFO_KEY, authInfo);
			paramsMap.put(RefreshPublisherListJob.CONFIG_VCARD_PROPS, vcardProp);
			paramsMap.put(RefreshPublisherListJob.CONFIG_IGNORE_ENTRIES_KEY, "");
			paramsMap.put(RefreshPublisherListJob.CONFIG_VALUESPACE_PROP, vcardProp.split(",")[0]);
			paramsMap.put(RefreshPublisherListJob.CONFIG_FILEPATH, "tomcat/shared/classes/org/edu_sharing/metadataset/valuespace_learnline2_0_contributer.xml");
		
			ImmediateJobListener jobListener = JobHandler.getInstance().startJob(RefreshPublisherListJob.class, paramsMap);
			
			if(jobListener.isVetoed()){
				throw new Exception("job was vetoed by "+jobListener.getVetoBy());
			}
		} catch(Throwable e) {
			this.errorHandling(e);
		}
	}
	
	@Override
	protected void checkPermutationStrongName() throws SecurityException {
		//http://code.google.com/p/gwteventservice/issues/detail?id=30
		//or http://code.google.com/p/gwt-examples/wiki/gwtTomcat#Security_Failure/Error_%28RPC_Failure_500%29
		try{
			super.checkPermutationStrongName();
		}catch(Throwable e){
			logger.error("Maybe the evil 500 bug?");
			e.printStackTrace();
		}
		return;
	}
	
	public void setLocaleInSession(String localeStr){
		this.getThreadLocalRequest().getSession().setAttribute(CCConstants.AUTH_LOCALE,localeStr);
	}
	
	public HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> getRecommendObjectsQuery(String repositoryId, String metadataSetId){
		logger.info("repositoryId:"+repositoryId);
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
		String ro_query = appInfo.getRecommend_objects_query();
		String value = "";
		if(ro_query != null && !ro_query.trim().equals("")){
			value = "dummyvalue";
			SearchMetadataHelper smdHelper = new SearchMetadataHelper();
			final HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> metadataSetSearchData = smdHelper.createSearchData("recommendobjectsproperty", ro_query, new String[]{value});
			MetadataSetQueryProperty mdsqp = metadataSetSearchData.get(metadataSetSearchData.keySet().iterator().next()).keySet().iterator().next();
			mdsqp.setEscape("false");
			Integer propertyId = new MetadataReader().createPropertyId(repositoryId, metadataSetId, MetadataSetQuery.class.getSimpleName(),0, "recommendobjectsproperty", 0);
			mdsqp.setId(propertyId);
			MetadataCache.add(mdsqp);
			return metadataSetSearchData;
		}
		return null;
	}
	
	public String getStylesheetByAppId(String appId) throws CCException {
		if (appId != null) {
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			if(appInfo != null){
				
				String url = appInfo.getCustomCss();
				logger.info("custom css:"+url);
				return url;
			}
		}
		return null;
	}
	
	/**
	 * @param parentId
	 * @param currentNodeId: can be null when checking for create process. set it for update.
	 * @param property
	 * @param value
	 * @param repositoryId
	 * @param authInfo
	 * @return
	 * @throws CCException
	 */
	public CheckForDuplicatesResult checkForDuplicates(String parentId, String currentNodeId, String property, String value, String repositoryId) throws CCException {
		
		CheckForDuplicatesResult result = new CheckForDuplicatesResult();
		if (property == null) {
			logger.error("property can't be null");
			throw new CCException(null, "property can't be null");
		}
		if (value == null) {
			logger.error("value can't be null");
			throw new CCException(null, "value can't be null");
		}
		try {
			//HashMap<String,String> authInfo = getValidatedAuthInfo(repositoryId);
			MCBaseClient mcBaseClient = this.getMCAlfrescoBaseClient(repositoryId);//RepoFactory.getInstance(repositoryId, authInfo);
	
			logger.info("parentId:"+parentId+" is null?"+(parentId == null));
			
			//parentId calculation/ quickfix for IE parentId can have the value 'null'
			if (parentId == null || parentId.trim().equals("") || parentId.trim().equals("null")) {
				parentId = new UserEnvironmentTool(repositoryId, new AuthenticationToolAPI().getAuthentication(this.perThreadRequest.get().getSession())).getDefaultUserDataFolder();
			}
			
			//do this as admin org.alfresco.repo.security.permissions.AccessDeniedException when editing an IO in the invited treetrunk where the current user dont got the permissions on parentID
			HashMap currentLevelObjects = null;
			if (mcBaseClient instanceof MCAlfrescoAPIClient) {
				currentLevelObjects = ((MCAlfrescoAPIClient)mcBaseClient).getChildrenRunAs(parentId, ApplicationInfoList.getHomeRepository().getUsername());
			} else {
				currentLevelObjects = ((MCAlfrescoBaseClient)mcBaseClient).getChildren(parentId);
			}
			
			//recommendValue
			String newValue = null;
			if (currentNodeId == null) {
				newValue = new DuplicateFinder().getUniqueValue(currentLevelObjects, property, value);
			} else {
				newValue = new DuplicateFinder().getUniqueValue(currentLevelObjects, currentNodeId, property, value);
			}
			
			if (newValue.equals(value)) {
				result.setNodeExists(false);
			} else {
				result.setNodeExists(true);
				result.setRecommendValue(newValue);
			}
			
		} catch(Throwable e) {
			errorHandling(e);
		}
		
		return result;
	}
	
	public String getHTMLTitleForURL(String url) throws CCException{
		
		HashMap<String,String> umlautMap = new HashMap<String,String>();
		umlautMap.put("&uuml;", "ü");
		umlautMap.put("&Uuml;", "Ü");
		umlautMap.put("&auml;", "ä");
		umlautMap.put("&Auml;", "Ä");
		umlautMap.put("&ouml;", "ö");
		umlautMap.put("&Ouml;", "Ö");
		umlautMap.put("&szlig;", "ß");
		
		try{
			//ValidateUrl
			new URL(url);
			HttpQueryTool httpQuery = new HttpQueryTool();
			String result = httpQuery.query(url);
			if(result == null) return null;
			Parser parser = new Parser(new Lexer(result));
		
			NodeClassFilter filter = new NodeClassFilter (TitleTag.class);
			NodeList list = parser.parse(filter);
			for(int i = 0; i < list.size(); i++){
				TitleTag titleTag = (TitleTag)list.elementAt(i);
				String rawTagName = titleTag.getRawTagName().toLowerCase();
				if(rawTagName != null){
					
					String title = titleTag.getTitle();
					
					if(title != null){
						for(Map.Entry<String, String> entry: umlautMap.entrySet()){
							title = title.replace(entry.getKey(),entry.getValue());
						}
					}
					
					return title;
				}
			}
			
		} catch(Throwable e) {
			this.errorHandling(e);
		}
		return null;
	}
	
	public ArrayList<ServerUpdateInfo> getServerUpdateInfos() throws CCException {
		ArrayList<ServerUpdateInfo> result = new ArrayList<ServerUpdateInfo>();
		try {
			if (!new CheckAuthentication().isAdmin(null, getValidatedAuthInfo(null))) {
				throw new CCException(CCException.UNKNOWNEXCEPTION, "You are not an admin");
			} else {		
				result.add(new ServerUpdateInfo(Licenses1.ID,Licenses1.description));
				result.add(new ServerUpdateInfo(Licenses2.ID,Licenses2.description));
				result.add(new ServerUpdateInfo(ClassificationKWToGeneralKW.ID,ClassificationKWToGeneralKW.description));
				result.add(new ServerUpdateInfo(SystemFolderNameToDisplayName.ID,SystemFolderNameToDisplayName.description));
				result.add(new ServerUpdateInfo(Release_1_6_SystemFolderNameRename.ID, Release_1_6_SystemFolderNameRename.description));
				result.add(new ServerUpdateInfo(Release_1_7_SubObjectsToFlatObjects.ID, Release_1_7_SubObjectsToFlatObjects.description));
				result.add(new ServerUpdateInfo(Release_1_7_UnmountGroupFolders.ID, Release_1_7_UnmountGroupFolders.description));
				result.add(new ServerUpdateInfo(Edu_SharingAuthoritiesUpdate.ID, Edu_SharingAuthoritiesUpdate.description));
				result.add(new ServerUpdateInfo(RefreshMimetypPreview.ID,RefreshMimetypPreview.description));
				result.add(new ServerUpdateInfo(FixMissingUserstoreNode.ID,FixMissingUserstoreNode.description));
				result.add(new ServerUpdateInfo(KeyGenerator.ID,KeyGenerator.description));
				result.add(new ServerUpdateInfo(FolderToMap.ID,FolderToMap.description));
				result.add(new ServerUpdateInfo(Edu_SharingPersonEsuidUpdate.ID,Edu_SharingPersonEsuidUpdate.description));
				result.add(new ServerUpdateInfo(Release_3_2_FillOriginalId.ID,Release_3_2_FillOriginalId.description));
				result.add(new ServerUpdateInfo(Release_3_2_DefaultScope.ID,Release_3_2_DefaultScope.description));
			}
		} catch (Throwable e) {
			this.errorHandling(e);
		}
		return result;
	}
	
	public String getDetailsHtmlSnippet(String repositoryId, String nodeId) throws CCException {
		try {
			MCBaseClient baseClient = RepoFactory.getInstance(repositoryId, getValidatedAuthInfo(repositoryId));
			return baseClient.getDetailsHtmlSnippet(nodeId);
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}

	public void track(ACTIVITY activity, CONTEXT_ITEM[] context, PLACE place) {
		try {
			TrackingService.track(activity, context, place, getValidatedAuthInfo(null));
		} catch(Throwable e) {
			logger.error(e.getMessage(),e);
		}	
	}
	
	public ArrayList<String> findPathToParent(String parentId, String nodeId, String repId) throws CCException {
		
		logger.info("nodeId:"+nodeId +" parentId:"+parentId);
		try{
			
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			ArrayList<String> reverseResult = new ArrayList<String>(); 
			findPath(parentId, nodeId, mcAlfrescoBaseClient, reverseResult, 0);
			
			ArrayList<String> result = new ArrayList<String>();
			String pathInfo = "";
			for(int i = (reverseResult.size()-1); i >= 0; i--){
				
				String currEle = reverseResult.get(i);
				pathInfo = pathInfo+"/"+currEle;
				result.add(currEle);
			}
			logger.info("PATH returned:"+pathInfo);
			return result;
			
		} catch(Throwable e) {
			this.errorHandling(e);
		}
		return null;
	}
	
	/**
	 * fills path to parent in reverse order, this means the parent is at the last index
	 * @param parentIdToFind
	 * @param nodeId
	 * @param mcAlfrescoBaseClient
	 * @param result
	 * @param level
	 * @throws Throwable
	 */
	private void findPath(String parentIdToFind, String nodeId, MCAlfrescoBaseClient mcAlfrescoBaseClient, ArrayList<String> result, int level) throws Throwable{
		
		HashMap<String, HashMap> parents = mcAlfrescoBaseClient.getParents(nodeId, false);
		for (String currentParentId : parents.keySet()) {
			
			//break
			if(result.contains(parentIdToFind)) {
				return;
			}
			
			int index = level;
			
			//clear to the current level
			while(((result.size() - 1) >= index) && index >= 0 ) {
				result.remove(index);
			}
			
			result.add(currentParentId);
			
			String pathInfo = "";
			for(int i = (result.size()-1); i >= 0; i--){
				pathInfo = pathInfo+"/"+mcAlfrescoBaseClient.getProperties(result.get(i)).get(CCConstants.CM_NAME);
			}
			logger.info("PATH current:"+pathInfo +" level:"+level);
			
			if (currentParentId.equals(parentIdToFind)) {
				logger.info("PATH returning:"+pathInfo);
				//everything is OK found a path to parent
				return;
			} else {
				findPath(parentIdToFind,currentParentId,mcAlfrescoBaseClient,result,(level+1));
			}
			
		}
		
	}
	
	public HashMap<String,String> addApplication(String appMetadataUrl) throws CCException{
		
		//cause standard properties class does not save the values sorted
		class SortedProperties extends Properties {
			
			public SortedProperties() {
				super();
			}
			
			public SortedProperties(Properties initWith) {
				for(Map.Entry entry : initWith.entrySet()){
					this.setProperty((String)entry.getKey(), (String)entry.getValue());
				}
			}
			
			//for sorted XML storing
			@Override
			public Set<Object> keySet() {
				 return new TreeSet<Object>(super.keySet());
			}

		}
		
		try{
			
			boolean isAdmin = new CheckAuthentication().isAdmin(ApplicationInfoList.getHomeRepository().getAppId(), getValidatedAuthInfo(null));
			if (!isAdmin) {
				throw new CCException(null,"you are not an admin");
			}
			
			HttpQueryTool httpQuery = new HttpQueryTool();
			String httpQueryResult = httpQuery.query(appMetadataUrl);
			if (httpQueryResult == null) {
				throw new CCException(null,"something went wrong. got no result for metadata url:"+appMetadataUrl);
			}
			
			String catalinaBase = System.getProperty("catalina.base");
			logger.info("catalinaBase:"+catalinaBase);
			
			Properties props = new SortedProperties();
			InputStream is = new ByteArrayInputStream(httpQueryResult.getBytes("UTF-8"));
			props.loadFromXML(is);
			String appId = props.getProperty(ApplicationInfo.KEY_APPID);
			
			if (appId == null || appId.trim().equals("")) {
				throw new Exception("no appId found");
			}
			
			String filename = "app-"+appId+".properties.xml";
			
			//check if appID already exists
			if (ApplicationInfoList.getApplicationInfos().keySet().contains(appId)) {
				throw new Exception("appId is already in registry");
			}
			
			//check for mandatory Property type
			String type = props.getProperty(ApplicationInfo.KEY_TYPE);
			if (type == null || type.trim().equals("")) {
				throw new Exception("missing type");
			}
			
			if (type.equals(ApplicationInfo.TYPE_RENDERSERVICE)) {
				String contentUrl = props.getProperty("contenturl");
				if(contentUrl == null || contentUrl.trim().equals("")){
					throw new Exception("a renderservice must have an contenturl");
				}
			}
			
			if (catalinaBase == null || catalinaBase.trim().equals("")) {
				throw new Exception("could not find catalina base in System Properties"); 
			}
			if(catalinaBase.contains("\\")) {
				catalinaBase = catalinaBase.replace("\\","/");
			}
			logger.info("catalinaBase:"+catalinaBase);
			
			File appFile = new File(catalinaBase+"/shared/classes/"+filename);
			if (!appFile.exists()) {
				props.storeToXML(new FileOutputStream(appFile), "");
			} else {
				throw new Exception("File "+appFile.getPath() + " already exsists");
			}
		
			String appRegistryfileName = "ccapp-registry.properties.xml";
			
			Properties propsAppRegistry = PropertiesHelper.getProperties(appRegistryfileName, PropertiesHelper.XML);
			
			String pathAppRegistry = catalinaBase+"/shared/classes/"+appRegistryfileName;
			
			//backup
			propsAppRegistry.storeToXML(new FileOutputStream(new File(pathAppRegistry+System.currentTimeMillis()+".bak")), " backup of registry");
			
			String applicationFilesValue = propsAppRegistry.getProperty("applicationfiles");
			propsAppRegistry.setProperty("applicationfiles", applicationFilesValue+","+filename);
			
			//overwrite
			propsAppRegistry.storeToXML(new FileOutputStream(new File(pathAppRegistry)), new Date()+" added file:"+filename);
			
			if (type.equals(ApplicationInfo.TYPE_RENDERSERVICE)) {
				String contentUrl = props.getProperty("contenturl");
				
				String homeAppFileName = "homeApplication.properties.xml";
				Properties homeAppProps = PropertiesHelper.getProperties(homeAppFileName, PropertiesHelper.XML);
				homeAppProps = new SortedProperties(homeAppProps);
				
				String homeAppPath = catalinaBase+"/shared/classes/"+homeAppFileName;
				//backup
				homeAppProps.storeToXML(new FileOutputStream(new File(homeAppPath+System.currentTimeMillis()+".bak")), " backup of homeApplication.properties.xml");
				
				homeAppProps.setProperty(ApplicationInfo.KEY_CONTENTURL, contentUrl);
				
				//overwrite
				homeAppProps.storeToXML(new FileOutputStream(new File(homeAppPath)), " added contenturl and preview url");
			}
			
			
			ApplicationInfoList.refresh();
			RepoFactory.refresh();
			
			HashMap<String,String> result = new HashMap<String,String>();
			for(Object key : props.keySet()){
				result.put((String)key,props.getProperty((String)key));
			}
			return result;
			
		}catch(Throwable e){
			this.errorHandling(e);
		}
		
		return null;
		
	}

	public GetPreviewResult getPreviewUrl(String nodeId, String repId) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			return mcAlfrescoBaseClient.getPreviewUrl(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), nodeId);
		} catch(Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}
	
	public void createShare(String repId, String nodeId, String[] emails, long expiryDate) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			mcAlfrescoBaseClient.createShare(nodeId, emails, expiryDate);
		} catch(Throwable e) {
			this.errorHandling(e);
		}
	}
	
	public Share[] getShares(String repId, String nodeId) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			return mcAlfrescoBaseClient.getShares(nodeId);
		} catch(Throwable e) {
			this.errorHandling(e);
			return null;
		}
	}
	
	public boolean isOwner(String repId, String nodeId) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			return mcAlfrescoBaseClient.isOwner(nodeId, mcAlfrescoBaseClient.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME));
		} catch(Throwable e) {
			this.errorHandling(e);
			return false;
		}
	}
	
	public HashMap<String,Boolean> hasAllToolPermissions(String repId, String[] toolPermissions) throws CCException {
		
		HashMap<String,Boolean> result = new HashMap<String,Boolean> (); 
		
		try {
			
			ToolPermissionService tps = ToolPermissionServiceFactory.getInstance();
			for(String toolPermission : toolPermissions){
				boolean	hasToolPerm = tps.hasToolPermission(toolPermission);	
				result.put(toolPermission, hasToolPerm);
			}
			
		} catch(Throwable e) {
			this.errorHandling(e);
		}
		return result;
	}
	
	public boolean hasToolPermissions(String repId, String toolPermission) throws CCException {
		try {
			ToolPermissionService tps = ToolPermissionServiceFactory.getInstance();
			return tps.hasToolPermission(toolPermission);	
		} catch (Throwable e) {
			this.errorHandling(e);
		}
		return false;
	}
	
	public void setGlobal(String nodeId,String repId) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			mcAlfrescoBaseClient.addAspect(nodeId, CCConstants.CCM_ASPECT_SCOPE);
		} catch (Throwable e) {
			this.errorHandling(e);
		}
	}
	
	public void removeGlobal(String nodeId,String repId) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			mcAlfrescoBaseClient.removeGlobalAspectFromGroup(nodeId);
		} catch(Throwable e) {
			this.errorHandling(e);
		}
	}
	
	public ArrayList<Group> getGlobalGroups(String repId) throws CCException {
		ArrayList<Group> result = new ArrayList<Group>();
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			HashMap<String, HashMap<String, Object>> raw = mcAlfrescoBaseClient.search("TYPE:cm\\:authorityContainer AND @ccm\\:scopetype:\"global\"");
			
			for (Map.Entry<String, HashMap<String,Object>> entry : raw.entrySet()) {
				Group group = new Group();
				group.setName((String)entry.getValue().get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
				group.setDisplayName((String)entry.getValue().get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
				group.setRepositoryId(repId);
				group.setNodeId((String)entry.getValue().get(CCConstants.SYS_PROP_NODE_UID));
				group.setAuthorityType(AuthorityType.getAuthorityType(group.getName()).name());
				group.setScope((String)entry.getValue().get(CCConstants.CCM_PROP_SCOPE_TYPE));
				result.add(group);
			}
	
		} catch(Throwable e) {
			this.errorHandling(e);
		}
		return result;
	}
	
	public ArrayList<Notify> getNotifyList(String repId, String nodeId) throws CCException {
		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = getMCAlfrescoBaseClient(repId);
			return new ArrayList<Notify>(mcAlfrescoBaseClient.getNotifyList(nodeId));	
		} catch (Throwable e) {
			this.errorHandling(e);
			return null;
		}	
	}
	
	public CacheInfo getCacheInfo(String name) throws CCException {
		if (!isAdmin(null)) {
			throw new CCException("Access Denied");
		}
		try {
			return CacheManagerFactory.getCacheInfo(name);
		} catch(Throwable e) {
			errorHandling(e);
			return null;
		}
	}
	
	public void refreshEduGroupCache() throws CCException {
		if(!isAdmin(null)){
			throw new CCException("Access Denied");
		}
		EduGroupCache.refresh();
	}
	
	@Override
	public void setPermissionsAndMail(SetPermissionsAndMail setPermAndMail) throws CCSessionExpiredException, CCException {
		// TODO Auto-generated method stub
		
	}
	
}
