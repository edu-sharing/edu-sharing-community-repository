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
package org.edu_sharing.repository.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.edu_sharing.repository.client.auth.CCSessionExpiredException;
import org.edu_sharing.repository.client.exception.CCException;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.Authority;
import org.edu_sharing.repository.client.rpc.CheckForDuplicatesResult;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.GetPermissions;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
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
import org.edu_sharing.repository.client.tracking.TrackingEvent.ACTIVITY;
import org.edu_sharing.repository.client.tracking.TrackingEvent.CONTEXT_ITEM;
import org.edu_sharing.repository.client.tracking.TrackingEvent.PLACE;

import com.google.gwt.user.client.rpc.RemoteService;

public interface MCAlfrescoService extends RemoteService {

	public void setLocaleInSession(String locale);

	public String getUserAgent();
	
	public HashMap<String,String> authenticate(String userName, String password) throws CCException;
	
	public HashMap<String,String> authenticateByGuest() throws CCException;
	
	public HashMap<String,String> getUserInfo() throws CCException;

	public SearchResult search(SearchToken seachToken) throws CCException;
	
	public SearchResult searchRecommendObjects(String repositoryId, int startIdx, int nrOfResults) throws CCException;
	
	public HashMap<String, HashMap<String, Object>> searchByParentId(SearchCriterias searchCriterias, String parentId) throws CCException;
	
	public HashMap<String, HashMap<String, Object>> searchInvited(SearchToken searchToken) throws CCException;

	public HashMap<String, HashMap<String, Object>> search(String searchWord, String type) throws CCException;
	
	public String getNodeIdAuthorityContainer() throws CCException;

	public HashMap<String,HashMap<String,Object>> getChildren(String parentID, String repositoryId) throws CCException;
	
	public HashMap<String,java.util.HashMap<String,Object>> getChildrenForGroups(String parentID, String repositoryId, HashMap<String, String> authenticationInfo) throws CCException;

	public HashMap getParents(String nodeId, boolean primary) throws CCException;

	public HashMap getAssociationNodes(String nodeID, String repositoryId, String association) throws CCException;

	public HashMap getBaskets() throws CCException;
	
	public String getBasketId() throws CCException;
	
	public HashMap getGroupFolders() throws CCException;
	
	public HashMap getGroupFolder() throws CCException;

	public String getGroupFolderId() throws CCException;

	public boolean removeBasket(String basketID) throws CCSessionExpiredException, Exception;
	
	public void removeChildrenSequentially(String parentId) throws CCException;

	public boolean dropToBasket(String basketID, String nodeID) throws CCSessionExpiredException, CCException,
			Exception;

	public String dropToBasketRemoteNode(String basketId, HashMap<String, String> params)
			throws CCSessionExpiredException, CCException, Exception;

	public boolean removeFromBasket(String basketID, String nodeID) throws CCException;

	public String newBasket(String basketName) throws CCSessionExpiredException, Exception;

	// public String createNode(String type, String parentID, String name,
	// HashMap properties, HashMap<String, String> authenticationInfo) throws
	// CCSessionExpiredException, Exception	<import resource="classpath:org/edu_sharing/spring/helper-registry.xml" />;

	public void removeRelations(String parentID) throws CCSessionExpiredException, Exception;

	public boolean updateNode(String nodeID, HashMap properties, String repositoryId)
			throws CCSessionExpiredException, Exception;

	public void createRelation(String parentID, String sourceID, String targetID, String name) throws CCException;

	public void createAssociation(String fromID, String toID, String association) throws CCSessionExpiredException,
			Exception;

	public void createChildAssociation(String parentID, String childID) throws CCSessionExpiredException, Exception;

	public void createChildOfFavoritesFolder(String nodeID) throws CCSessionExpiredException, CCException,
			Exception;

	public void createNode(String parentID, String nodeType, String repositoryId, HashMap properties, String childAssociation) throws CCException;

	public HashMap getISO8601DateFormat(HashMap dates);
	
	public Date getDateFromISOString(String isoDate);

	public GetPermissions getPermissions(String nodeId) throws CCSessionExpiredException, CCException;

	public GetPermissions getPermissions(String nodeId, String repositoryId) throws CCException;

	public void move(String fromParentId, String parentId, String nodeId, String repositoryId) throws CCSessionExpiredException,
			CCException;
	
	public void copy(String repositoryId, String nodeId, String toNodeId) throws CCException;

	public void removePermissions(String nodeId, String _authority, String[] permission)
			throws CCSessionExpiredException, CCException;


	public HashMap<String, Object> getNode(String nodeId) throws CCSessionExpiredException, CCException;

	public HashMap<String, Object> getNode(String nodeId, String repositoryId) throws CCException;
	
	public HashMap<String, Object> getNode(String nodeId, String repositoryId, String version) throws CCException;

	public HashMap<String, HashMap<String,Object>> getNodes(String[] nodeIds, String repositoryId) throws CCException;

	public ArrayList<HashMap<String, Object>> getVCardAsMap(String vcard) throws CCException;

	public HashMap<String, HashMap<String, Object>> getChildrenByType(String nodeId, String type)
			throws CCException;

	public HashMap<String, HashMap<String, Object>> getChildrenByType(String nodeId, String type, String repositoryId) throws CCException;

	public HashMap<String, HashMap<String, Object>> getChildrenByAssociation(String store, String nodeId, String association, String repositoryId) throws CCException;

	public HashMap<String, String> getPropFileContent(String propFile);

	public void removeUserDefinedPreview(String nodeId, String repId) throws CCException;

	public boolean hasPermissions(String nodeId, String[] permissions, String repId) throws CCException;

	public HashMap<String, Boolean> hasAllPermissions(String nodeId, String[] permissions, String repId) throws CCException;

	public HashMap<String, Boolean> hasAllPermissions(String nodeId, String authority, String[] permissions, String repId) throws CCException;

	public ArrayList<Authority> getAllAuthoritiesHavingPermission(String nodeId, String permissions, String repId) throws CCException ;

	public HashMap<String, HashMap<String, Object>> getUsages(String nodeId, String repId) throws CCException;

	public boolean isAdmin(String repId) throws CCException;
	
	public boolean isGuest(String repId) throws CCException;

	public String checkSystemFolderAndReturn(String foldername, String repId) throws CCException;

	public ArrayList<String> getAllValuesFor(String property) throws CCException;

	public ArrayList<String> getImporterJobList() throws CCException;
	
	public HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> getRecommendObjectsQuery(String repositoryId, String metadataSetId);

	public String getStylesheetByAppId(String appId) throws CCException;
	
	public CheckForDuplicatesResult checkForDuplicates(String parentId, String currentNodeId, String property, String value, String repositoryId) throws CCException;

	public String getHTMLTitleForURL(String url) throws CCException;
	
	public ArrayList<ServerUpdateInfo> getServerUpdateInfos() throws CCException;
	
	public String getDetailsHtmlSnippet(String repositoryId, String nodeId) throws CCException;

	/**
	 * Track an action.
	 * 
	 * @param activity what
	 * @param context with
	 * @param place where
	 * @param authInfo who
	 */
	public void track(ACTIVITY activity, CONTEXT_ITEM[] context, PLACE place);	
	
	public ArrayList<String> findPathToParent(String parentId, String nodeId, String repId) throws CCException;

	public HashMap<String,String> addApplication(String appMetadataUrl) throws CCException;
	
	public HashMap<String, HashMap<String, Object>> processEduGroupCommand(String propName) throws CCException;
	
	public HashMap<String, HashMap<String,Object>> getVersionHistory(String nodeId, String repId) throws CCException;
	
	public Group getEduGroupContextOfNode(String nodeId) throws CCException;

	public Result<List<User>> findUsers(String query, List<String> searchFields, boolean globalContext, int from, int nrOfResults) throws CCException;
	
	public Result<List<Group>> findGroups(String _toSearch, boolean globalContext, int from, int nrOfResults) throws CCException;
	
	public Result<List<Authority>> findAuthorities(String _toSearch, boolean globalContext, int from, int nrOfResults) throws CCException;
	
	public void refreshApplicationInfo() throws CCException;
	
	public void writePublisherToMDSXml(String vcardProp) throws CCException;
	
	public void runOAIPMHLOMImporter(ArrayList<String> setsParam, String oaiBaseUrl, String metadataSetId, String metadataPrefix, String importerClassName) throws CCException;
	
	public void startCacheRefreshingJob(String[] params) throws CCException;
	
	public void removeDeletedImports(String oaiBaseUrl, String cataloges, String oaiMetadataPrefix) throws CCException;
	
	public void removeOAIImportedObjects() throws CCException;
	
	public void revertVersion(String nodeId, String versLbl, String repId) throws CCException;

	public ArrayList<EduGroup> getEduGroups() throws CCException;
	
	public GetPreviewResult getPreviewUrl(String nodeId, String repId) throws CCException;
	
	public void createShare(String repId, String nodeId, String[] emails, long expiryDate) throws CCException;
	
	public Share[] getShares(String repId, String nodeId) throws CCException;
	
	public boolean isOwner(String repId, String nodeId) throws CCException;
	
	public HashMap<String,Boolean> hasAllToolPermissions(String repId, String[] toolPermissions) throws CCException;
	
	public boolean hasToolPermissions(String repId, String toolPermission) throws CCException;
	
	public void requestForPublishPermission(String repositoryId, String nodeId, String message) throws CCException;
	
	public HashMap<String,HashMap<String,Object>> getChildren(String parentID, String[] permissions, String repositoryId) throws CCException;
	
	public void setGlobal(String nodeId,String repId) throws CCException;
	
	public void removeGlobal(String nodeId,String repId) throws CCException;
	
	public ArrayList<Group> getGlobalGroups(String repId) throws CCException;
	
	public CacheInfo getCacheInfo(String name) throws CCException;
	
	public void refreshEduGroupCache() throws CCException;
	
	public HashMap<String, HashMap<String, Object>> searchWorkflowTasks(String searchWord) throws CCException;
	
	public Boolean isUsed(String nodeId) throws CCException;
}
