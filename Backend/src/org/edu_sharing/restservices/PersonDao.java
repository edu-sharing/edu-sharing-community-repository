package org.edu_sharing.restservices;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.cache.PersonCache;
import org.edu_sharing.restservices.iam.v1.model.GroupEntries;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpSession;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public class PersonDao {

	Logger logger = Logger.getLogger(PersonDao.class);

	public static final String ME = "-me-";
	
	public static PersonDao getPerson(RepositoryDao repoDao, String userName) throws DAOException {
		
		try {
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
			
			if (ME.equals(userName)) {
	
				userName = currentUser;
			}
	
			/*
			if (   !currentUser.equals(userName)
				&& !repoDao.getBaseClient().isAdmin(currentUser)
				&& !AuthenticationUtil.isRunAsUserTheSystemUser()
					) {
								
				throw new AccessDeniedException(currentUser);
			}
			*/

			return new PersonDao(repoDao, userName);
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	private boolean isCurrentUserOrAdmin() {
		try {
		String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
		if (   !currentUser.equals(getUserName())
				&& !repoDao.getBaseClient().isAdmin(currentUser)
				&& !AuthenticationUtil.isRunAsUserTheSystemUser()
					) {

				return false;
			}
		}catch(Exception e) {
			return false;
		}
		return true;
	}
	public static PersonDao createPerson(RepositoryDao repoDao, String userName,String password, UserProfile profile) throws DAOException {
		
		try {

			try {
				
				repoDao.getBaseClient().getUserInfo(userName);

				throw new DAOValidationException(
						new IllegalArgumentException("Username already exists."));
				
			} catch (NoSuchPersonException e) {
				
				HashMap<String, String> userInfo = new HashMap<String, String>();
				userInfo.put(CCConstants.PROP_USERNAME, userName);
				userInfo.put(CCConstants.PROP_USER_FIRSTNAME, profile.getFirstName());
				userInfo.put(CCConstants.PROP_USER_LASTNAME, profile.getLastName());
				userInfo.put(CCConstants.PROP_USER_EMAIL, profile.getEmail());
				
				((MCAlfrescoAPIClient)repoDao.getBaseClient()).createOrUpdateUser(userInfo);
				PersonDao result=new PersonDao(repoDao, userName);
				if(password!=null)
					result.changePassword(null,password);
				return result;
			}			
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	
	private final MCAlfrescoBaseClient baseClient;

	private final RepositoryDao repoDao;
	
	private final Map<String, Serializable> userInfo;
	private final String homeFolderId;
	private final List<String> sharedFolderIds = new ArrayList<String>();

	private NodeService nodeService;
	private SearchService searchService;

	private AuthorityService authorityService;


	public PersonDao(RepositoryDao repoDao, String userName) throws DAOException  {

		try {
			
			this.baseClient = repoDao.getBaseClient();
			this.nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
			this.searchService = SearchServiceFactory.getSearchService(repoDao.getId());
			this.authorityService = AuthorityServiceFactory.getAuthorityService(repoDao.getId());

			this.repoDao = repoDao;

			this.userInfo = authorityService.getUserInfo(userName);
			this.homeFolderId = baseClient.getHomeFolderID(userName);
			try{
				
				boolean getGroupFolder = true;
				//don't run into access denied wrapped by Transaction commit failed
				if(!AuthenticationUtil.isRunAsUserTheSystemUser() 
						&& !AuthenticationUtil.getRunAsUser().equals(ApplicationInfoList.getHomeRepository().getUsername())
						&& !AuthenticationUtil.getRunAsUser().equals(userName)) {
					getGroupFolder = false;
				}
				if(getGroupFolder && userName!=null) {
					String groupFolderId = ((MCAlfrescoAPIClient)baseClient).getGroupFolderId(userName);
					if (groupFolderId != null) {
						
						HashMap<String, HashMap<String, Object>> children = baseClient.getChildren(groupFolderId);
						
						for (Object key : children.keySet()) {
		
							sharedFolderIds.add(key.toString());
						}				
					}
				}
			}catch(InvalidNodeRefException e){
				
			}
			catch(AccessDeniedException e){
			
			}
			
		} catch (Throwable t) {
			t.printStackTrace();
			throw DAOException.mapping(t);
		}
	}
	
	public void changeProfile(UserProfile profile) throws DAOException {
		
		try {

			HashMap<String, Serializable> newUserInfo = new HashMap<String, Serializable>();
			
			newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());
			
			newUserInfo.put(CCConstants.PROP_USER_FIRSTNAME, profile.getFirstName());
			newUserInfo.put(CCConstants.PROP_USER_LASTNAME, profile.getLastName());
			newUserInfo.put(CCConstants.PROP_USER_EMAIL, profile.getEmail());
			newUserInfo.put(CCConstants.CM_PROP_PERSON_ABOUT, profile.getAbout());
			newUserInfo.put(CCConstants.CM_PROP_PERSON_SKILLS, profile.getSkills());

			authorityService.createOrUpdateUser(newUserInfo);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}

	}
	public GroupEntries getMemberships(String pattern, int skipCount, int maxItems, SortDefinition sort) throws DAOException{
		if (!AuthenticationUtil.getFullyAuthenticatedUser().equals(getAuthorityName()) && !AuthorityServiceFactory.getLocalService().isGlobalAdmin()) {
			throw new NotAnAdminException();
		}
    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).searchPersonGroups(
    					getAuthorityName(),
    					pattern,
    					skipCount,
    					maxItems,
    					sort

    			);
		List<Group> result = new ArrayList<>();
    	for (String member: search.getData()) {
    		result.add(new GroupDao(repoDao,member).asGroup());
    	}
    	GroupEntries response = new GroupEntries();
    	response.setList(result);
    	response.setPagination(new Pagination(search));
    	return response;
	}

	public void changePassword(String oldPassword, String newPassword) throws DAOException {
		
		try {
			
			if (oldPassword == null) {
			
				((MCAlfrescoAPIClient)this.baseClient).setUserPassword(getUserName(), newPassword);
				
			} else {

				((MCAlfrescoAPIClient)this.baseClient).updateUserPassword(getUserName(), oldPassword, newPassword);
				
			}
				
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}

	}
	
	public void delete() throws DAOException {
		
		try {

			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
			
			if (currentUser.equals(getUserName())) {
								
				throw new DAOValidationException(
						new IllegalArgumentException("Session user can not be deleted."));
			}

			((MCAlfrescoAPIClient)this.baseClient).deleteUser(getUserName());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	private String getNodeId() {
		return (String) this.userInfo.get(CCConstants.SYS_PROP_NODE_UID);
	}
	public User asPerson() {
		
    	User data = new User();
    	
    	data.setAuthorityName(getAuthorityName());
    	data.setAuthorityType(Authority.Type.USER);
    	
    	data.setUserName(getUserName());
    	

    	data.setProfile(getProfile());
    	data.setStats(getStats());

    	if(isCurrentUserOrAdmin()) {
	    	NodeRef homeDir = new NodeRef();
	    	homeDir.setRepo(repoDao.getId());
	    	homeDir.setId(getHomeFolder());
	    	data.setHomeFolder(homeDir);

	    	List<NodeRef> sharedFolderRefs = new ArrayList<NodeRef>();
	    	for (String sharedFolderId : sharedFolderIds) {

	        	NodeRef sharedFolderRef = new NodeRef();
	        	sharedFolderRef.setRepo(repoDao.getId());
	        	sharedFolderRef.setId(sharedFolderId);

	        	sharedFolderRefs.add(sharedFolderRef);
	    	}
	    	data.setSharedFolders(sharedFolderRefs);
    	}
    	return data;
	}
	private UserProfile getProfile() {
		UserProfile profile = new UserProfile();
    	profile.setFirstName(getFirstName());
    	profile.setLastName(getLastName());
    	profile.setEmail(getEmail());
    	profile.setAvatar(getAvatar());
    	profile.setAbout(getAbout());
    	profile.setSkills(getSkills());
    	profile.setType(getType());
    	return profile;
	}
	private UserStats getStats() {
		UserStats stats = new UserStats();
		// run as admin so solr counts all materials and collections
		return AuthenticationUtil.runAsSystem(new RunAsWork<UserStats>() {

			@Override
			public UserStats doWork() throws Exception {
				String luceneUser = "@cm\\:creator:\""+QueryParser.escape(getAuthorityName())+"\"";
				SearchToken token=new SearchToken();
				token.setMaxResult(0);
				token.setLuceneString(luceneUser);
				token.setContentType(SearchService.ContentType.FILES);
		    	SearchResultNodeRef result = searchService.search(token);
		    	stats.setNodeCount(result.getNodeCount());

		    	token.setLuceneString(luceneUser+" AND @ccm\\:commonlicense_key:\"CC_*\"");
		    	result = searchService.search(token);
		    	stats.setNodeCountCC(result.getNodeCount());

		    	token.setLuceneString(luceneUser);
		    	token.setContentType(SearchService.ContentType.COLLECTIONS);
		    	result = searchService.search(token);
		    	stats.setCollectionCount(result.getNodeCount());
				return stats;
			}
		});
	}

	private org.alfresco.service.cmr.repository.NodeRef getAvatarNode() {
		List<ChildAssociationRef> refs = this.nodeService.getChildrenChildAssociationRef(getNodeId());
		for(ChildAssociationRef ref : refs) {
			if(ref.getTypeQName().equals(QName.createQName(CCConstants.ASSOC_USER_PREFERENCEIMAGE))){
				return ref.getChildRef();
			}
		}
		return null;
	}
	private String getAvatar() {
		org.alfresco.service.cmr.repository.NodeRef avatar=getAvatarNode();
		if(avatar==null)
			return null;
		return NodeServiceHelper.getPreview(avatar).getUrl();
	}
	public void removeAvatar() throws DAOException {
		try {
			org.alfresco.service.cmr.repository.NodeRef currentAvatar = getAvatarNode();
			if(currentAvatar!=null) {
				this.nodeService.removeNode(currentAvatar.getId(), getNodeId(), false);
			}
		}catch(Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	public void changeAvatar(InputStream is) throws DAOException {
		try {
		org.alfresco.service.cmr.repository.NodeRef currentAvatar = getAvatarNode();
		String nodeId=null;
		if(currentAvatar==null) {
			nodeId = this.nodeService.createNode(getNodeId(), CCConstants.CCM_TYPE_IO, new HashMap<>(), CCConstants.ASSOC_USER_PREFERENCEIMAGE);
			this.baseClient.createAssociation(getNodeId(), nodeId, CCConstants.ASSOC_USER_AVATAR);
		}
		else {
			nodeId=currentAvatar.getId();
		}
		NodeServiceHelper.setCreateVersion(nodeId,false);
		nodeService.writeContent(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, is, "image", null, CCConstants.CM_PROP_CONTENT);
		this.nodeService.setPermissions(nodeId, CCConstants.AUTHORITY_GROUP_EVERYONE,new String[]{CCConstants.PERMISSION_CONSUMER},true);
		}catch(Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	public UserSimple asPersonSimple() {
		UserSimple data = new UserSimple();    	
    	data.setAuthorityName(getAuthorityName());
    	data.setAuthorityType(Authority.Type.USER);    	
    	data.setUserName(getUserName());    	
    	data.setProfile(getProfile());
    	if(isCurrentUserOrAdmin()) {
	    	NodeRef homeDir = new NodeRef();
	    	homeDir.setRepo(repoDao.getId());
	    	homeDir.setId(getHomeFolder());
    	}
    	return data;
	}
	public String getId() {
		return getNodeId();
	}
	
	public String getAuthorityName() {
		
		return getUserName();
	}
	
	public String getUserName() {
		
		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_USERNAME);
	}
	
	public String getFirstName() {
		
		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
	}

	public String[] getType() {
		return AuthenticationUtil.runAsSystem(new RunAsWork<String[]>() {
			@Override
			public String[] doWork() throws Exception {
				 PersonCache.get(getAuthorityName(),PersonCache.TYPE);
				if(PersonCache.contains(getAuthorityName(),PersonCache.TYPE)) {
					logger.info("using person cache for "+getAuthorityName());
					return (String[]) PersonCache.get(getAuthorityName(),PersonCache.TYPE);
				}
				Set<String> types=new HashSet<>();
				Set<String> groups = authorityService.getMemberships(getAuthorityName());
				for(String group : groups) {
					try {
						String type=GroupDao.getGroup(repoDao, group).getGroupType();
						if(type!=null)
							types.add(type);

					}catch(Throwable t) {}
				}
				String[] typesArray = types.toArray(new String[0]);
				PersonCache.put(getAuthorityName(),PersonCache.TYPE, typesArray);
				return typesArray;
			}
		});
	}
	
	public String getLastName() {
		
		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
	}
	
	public String getEmail() {
		
		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);
	}
	public String getAbout() {
		return (String)this.userInfo.get(CCConstants.CM_PROP_PERSON_ABOUT);
	}
	public String[] getSkills() {
		return (String[])this.userInfo.get(CCConstants.CM_PROP_PERSON_SKILLS);
	}
	public String getHomeFolder() {
		
		return this.homeFolderId;
	}

	public String getPreferences() {
		return (String)this.userInfo.get(CCConstants.CCM_PROP_PERSON_PREFERENCES);
	}
	public void setPreferences(String preferences) throws Exception{
		// validate json
		new JSONObject(preferences);
		HashMap<String, String> newUserInfo = new HashMap<String, String>();
		newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());		
		newUserInfo.put(CCConstants.CCM_PROP_PERSON_PREFERENCES, preferences);		
		((MCAlfrescoAPIClient)this.baseClient).updateUser(newUserInfo);
	}
	public void addNodeList(String list,String nodeId) throws Exception {
		// Simply check if node is valid
		NodeDao node=NodeDao.getNode(repoDao, nodeId);
		if(node.isDirectory())
			throw new IllegalArgumentException("The node "+nodeId+" is a directory. Only files are allowed for this list");
		String data=getCurrentNodeListJson();
		JSONObject json=new JSONObject();
		if(data!=null)
			json=new JSONObject(data);
		
		JSONArray array=null;
		if(json.has(list))
			array=json.getJSONArray(list);
		List<JSONObject> nodes=new ArrayList<>();
		if(array!=null){
			for(int i=0;i<array.length();i++){
				if(array.getJSONObject(i).getString("id").equals(nodeId))
					throw new IllegalAccessException("Node is already in list: "+nodeId);
				nodes.add(array.getJSONObject(i));
			}
		}
		JSONObject object=new JSONObject();
		object.put("id",nodeId);
		object.put("dateAdded",System.currentTimeMillis());
		nodes.add(object);
		json.put(list,new JSONArray(nodes));
		updateNodeList(json);
	}
	private String getCurrentNodeListJson(){
		org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		String data;
		if(service.isGuest()){
			data=(String) session.getAttribute(CCConstants.CCM_PROP_PERSON_NODE_LISTS);
		}
		else{
			data=(String) this.userInfo.get(CCConstants.CCM_PROP_PERSON_NODE_LISTS);
		}
		return data;
	}
	public List<NodeRef> getNodeList(String list) throws Exception {
		String data=getCurrentNodeListJson();
		if(data==null)
			return null;
		JSONObject json=new JSONObject(data);
		if(!json.has(list))
			return null;
		JSONArray array=json.getJSONArray(list);
		List<NodeRef> result=new ArrayList<>();
		for(int i=0;i<array.length();i++){
			String nodeId=array.getJSONObject(i).getString("id");
			try{
				// causes invalid nodes to fire throwable -> delete them
				NodeDao.getNode(repoDao, nodeId);
				result.add(new NodeRef(repoDao.getId(), nodeId));
			}
			catch(Throwable t){
				removeNodeList(list,nodeId);
			}
		}
		return result;			
	}

	public void removeNodeList(String list, String nodeId) throws Exception {
		String data=getCurrentNodeListJson();
		if(data==null)
			throw new IllegalArgumentException("Node list not found: "+list);
		JSONObject json=new JSONObject(data);
		if(!json.has(list))
			throw new IllegalArgumentException("Node list not found: "+list);
		JSONArray array=json.getJSONArray(list);
		boolean found=false;
		List<JSONObject> result=new ArrayList<>();
		for(int i=0;i<array.length();i++){
			if(array.getJSONObject(i).getString("id").equals(nodeId)){
				found=true;
			}
			else
				result.add(array.getJSONObject(i));
		}
		if(!found)
			throw new IllegalArgumentException("Node not found in list: "+nodeId);
		json.put(list, new JSONArray(result));
		updateNodeList(json);
}

	private void updateNodeList(JSONObject json) throws Exception {
		org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		if(service.isGuest()){
			session.setAttribute(CCConstants.CCM_PROP_PERSON_NODE_LISTS,json.toString());
		}
		else{
			HashMap<String, String> newUserInfo = new HashMap<String, String>();
			newUserInfo.put(CCConstants.PROP_USERNAME, getUserName());
			newUserInfo.put(CCConstants.CCM_PROP_PERSON_NODE_LISTS, json.toString());
			((MCAlfrescoAPIClient)this.baseClient).updateUser(newUserInfo);
		}
	}
}
