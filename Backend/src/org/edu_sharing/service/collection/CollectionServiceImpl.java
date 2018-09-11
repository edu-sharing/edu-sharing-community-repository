package org.edu_sharing.service.collection;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.edu_sharing.repository.server.tools.NodeTool;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.edu_sharing.restservices.CollectionDao;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.CollectionDao.Scope;
import org.edu_sharing.restservices.CollectionDao.SearchScope;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.springframework.context.ApplicationContext;


public class CollectionServiceImpl implements CollectionService{
	
	Logger logger = Logger.getLogger(CollectionServiceImpl.class);
	
	String pattern;
	
	String path;
	
	final Lock lock = new ReentrantLock();
	
	Map<String, String> cache = new HashMap<String, String>();
	
	private final String SEPARATOR = "/";
	
	ApplicationInfo appInfo = null;
	
	MCAlfrescoAPIClient client = null;
	
	AuthenticationTool authTool = null;
	
	HashMap<String,String> authInfo;
	
	SearchService searchService;
	NodeService nodeService;
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	TransactionService transactionService = serviceRegistry.getTransactionService();
	
	ToolPermissionService  toolPermissionService;
	
	org.edu_sharing.service.permission.PermissionService permissionService;
	
	public CollectionServiceImpl(String appId, String pattern, String path) {
		try{
			this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			
			this.authTool = RepoFactory.getAuthenticationToolInstance(appId);
			
			//fix for running in runas user mode
			if((AuthenticationUtil.isRunAsUserTheSystemUser() || "admin".equals(AuthenticationUtil.getRunAsUser())) ) {
				logger.debug("starting in runas user mode");
				this.authInfo = new HashMap<String,String>();
				this.authInfo.put(CCConstants.AUTH_USERNAME, AuthenticationUtil.getRunAsUser());
			}else {
				this.authInfo = this.authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
			}
			try{
				this.client = new MCAlfrescoAPIClient();
			}catch(net.sf.acegisecurity.AuthenticationCredentialsNotFoundException e){
				//when remote auth
				logger.warn(e.getMessage());
			}
			this.searchService = SearchServiceFactory.getSearchService(appId);
			this.nodeService = NodeServiceFactory.getNodeService(appId);
			this.pattern = pattern;
			this.path = path;
			this.toolPermissionService = ToolPermissionServiceFactory.getInstance();
			this.permissionService = PermissionServiceFactory.getPermissionService(appId);
			
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String addToCollection(String collectionId, String originalNodeId) throws Throwable {
		
		try{
			List<String> aspects = Arrays.asList(client.getAspects(originalNodeId));
			
			/**
			 * use original
			 */
			String nodeId=originalNodeId;
			if(aspects.contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)){
				originalNodeId = client.getProperty(Constants.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), originalNodeId, CCConstants.CCM_PROP_IO_ORIGINAL);
			}
			
			String locale = (Context.getCurrentInstance() != null) ? Context.getCurrentInstance().getLocale() : "de_DE";

			// user must have CC_PUBLISH on either the original or a reference object
			if(!client.hasPermissions(originalNodeId, new String[]{CCConstants.PERMISSION_CC_PUBLISH})
					&& !client.hasPermissions(nodeId, new String[]{CCConstants.PERMISSION_CC_PUBLISH})){
				String message = I18nServer.getTranslationDefaultResourcebundle("collection_no_publish_permission", locale);
				throw new Exception(message);
			}
			
			if(!toolPermissionService.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES) 
					&& !client.isOwner(collectionId, AuthenticationUtil.getFullyAuthenticatedUser())){
				throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES);
			}
			
			String nodeType = client.getNodeType(originalNodeId);
			if(!nodeType.equals(CCConstants.CCM_TYPE_IO)){
				throw new Exception("Only Files are allowed to be added!");
			}
			
			for(String node : client.getChildren(collectionId).keySet()){
				// TODO: Maybe we can find a faster way to determine it?
				String nodeRef = client.getProperty(Constants.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), node, CCConstants.CCM_PROP_IO_ORIGINAL);
				if(originalNodeId.equals(nodeRef)){
					String message = I18nServer.getTranslationDefaultResourcebundle("collection_already_in", locale);
					
					throw new DuplicateNodeException(message);
				}
			}

			HashMap<String,Object> props = client.getProperties(originalNodeId);
			String versLabel = (String)props.get(CCConstants.CM_PROP_VERSIONABLELABEL);
			
			/**
			 * make a copy of the original. 
			 * OnCopyCollectionRefPolicy cares about
			 * - not duplicating the content
			 * - ignore childs: usage and license data
			 * - the preview child will be copied
			 */
			String refId = client.copyNode(originalNodeId, collectionId, true);
			
			client.setProperty(refId, CCConstants.CCM_PROP_IO_ORIGINAL, originalNodeId);
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
				@Override
				public Void doWork() throws Exception {
					permissionService.setPermissions(refId, null, true);
					return null;
				}
			});
			
			client.addAspect(refId, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE);
			client.addAspect(refId, CCConstants.CCM_ASPECT_POSITIONABLE);
			
			
			/**
			 * write content, so that the index tracking will be triggered
			 * the overwritten NodeContentGet class checks if it' s an collection ref object
			 * and switches the nodeId to original node, which is used for indexing
			 */
			client.writeContent(refId, new String("1").getBytes(), (String)props.get(CCConstants.ALFRESCO_MIMETYPE) , "utf-8", CCConstants.CM_PROP_CONTENT);
			
			//set to original size
			client.setProperty(refId, CCConstants.LOM_PROP_TECHNICAL_SIZE, client.getProperty(Constants.storeRef, originalNodeId, CCConstants.LOM_PROP_TECHNICAL_SIZE));
			
			new Usage2Service().setUsage(appInfo.getAppId(), 
					authInfo.get(CCConstants.AUTH_USERNAME), 
					appInfo.getAppId(), 
					collectionId, 
					originalNodeId, null, null, null, -1, versLabel, refId, null);
			
			return refId;
		
		}catch(Throwable e){
			throw e;
		}
	}
	
	@Override
	public String[] addToCollection(String collectionId, String[] originalNodeIds) throws Throwable {
		List<String> refIds = new ArrayList<String>();
		for(String orgId : originalNodeIds){
			refIds.add(addToCollection(collectionId, orgId));
		}
		return refIds.toArray(new String[refIds.size()]);
	}
	
	@Override
	public Collection create(String parentId, Collection collection) throws Throwable {
	
		String currentUsername = null;
		
		if(Context.getCurrentInstance() != null) {
			currentUsername = authTool.validateAuthentication(Context.getCurrentInstance().getRequest().getSession()).get(CCConstants.AUTH_USERNAME);
		}else {
			if(AuthenticationUtil.getRunAsUser() != null) {
				currentUsername = AuthenticationUtil.getRunAsUser();
			}
		}
		
		final String fcurrentUsername = currentUsername;
		
		if(fcurrentUsername != null) {
			return AuthenticationUtil.runAsSystem(new RunAsWork<Collection>() {

				@Override
				public Collection doWork() throws Exception {
					String parentIdLocal=parentId;
					if(parentIdLocal == null){
						
						collection.setLevel0(true);
						
						parentIdLocal = getContainerId(client);
					}
					
					HashMap<String,Object> props = asProps(collection);
					try {
						new DuplicateFinder().transformToSafeName(client.getChildren(parentIdLocal), props);
					} catch (Throwable e) {
						throw new Exception(e);
					}
					
					String collectionId = client.createNode(parentIdLocal, CCConstants.CCM_TYPE_MAP, props);
					client.addAspect(collectionId, CCConstants.CCM_ASPECT_COLLECTION);
					client.addAspect(collectionId, CCConstants.CCM_ASPECT_POSITIONABLE);

					client.setOwner(collectionId, fcurrentUsername);
					collection.setNodeId(collectionId);
					return collection;
				}
			});
		}else {
			throw new Exception("not authenticated");
		}
			
	}
	
	@Override
	public Collection createAndSetScope(String parentId, Collection collection) throws Throwable{
		Collection col = create(parentId,collection);
	    setScope(col);
		return col;
	}
	
	private String getContainerId(MCAlfrescoBaseClient client){
		String result = null;
		try{
			
			// request node	
			HashMap<String, HashMap<String, Object>> search = client.search("PATH:\"" + path + "\"", CCConstants.CM_TYPE_FOLDER);
			String rootId = null;
			if (search.size() != 1) {
				if(search.size() > 1) throw new IllegalArgumentException("The path must reference a unique node.");
				
				
				String startAt = client.getCompanyHomeNodeId();
				String collectionPath = new String(path);
				String pathCompanyHome = "/app:company_home/";
				
				if(collectionPath.startsWith(pathCompanyHome)){
					collectionPath = collectionPath.replace(pathCompanyHome, "");
				}
				
				collectionPath = collectionPath.replaceAll("[a-zA-Z]*:", "");
				collectionPath = (collectionPath.startsWith("/"))? collectionPath.replaceFirst("/", "") : collectionPath;
				rootId = new NodeTool().createOrGetNodeByName(client,startAt , collectionPath.split("/"));
			}else{
				rootId = search.keySet().iterator().next();	
			}
			
			
			String[] patterns = pattern.split(SEPARATOR); 
			
			DateFormat[] formatter = new DateFormat[patterns.length];
			for (int i = 0, c = patterns.length; i<c; ++i) {
				formatter[i] = new SimpleDateFormat(patterns[i]);
			}
			
			String[] items = new String[formatter.length];
			StringBuilder path = new StringBuilder();
			
			Date date = new Date();
			
			for (int i = 0, c = formatter.length; i < c; ++i) {
				items[i] = formatter[i].format(date);
				
				if (i > 0) {
					path.append(SEPARATOR);
				}
				path.append(items[i]);
			}
			
			try{
				lock.lock();
				String key = path.toString();
				result = cache.get(key);
				if(result == null){
					result = new NodeTool().createOrGetNodeByName(client, rootId, items);
					cache.put(key, result);
				}
			}finally{
				lock.unlock();
			}
			
		}catch (Throwable e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
		return result;
	}
	
	
	/**
	 * @TODO 
	 * not ready yet, set usage again, refId must be safed by addToCollection
	 * set level0 to false when moving an root collection to an sub one
	 * set level0 to true when moving an childcollection to root
	 */
	@Override
	public void move(String toCollection, String toMove) {
		try{
			
			if(CCConstants.CCM_TYPE_IO.equals(client.getNodeType(toMove))){
				String parent = client.getParents(toMove, true).keySet().iterator().next();
				client.moveNode(toCollection, CCConstants.CM_ASSOC_FOLDER_CONTAINS, toMove);
				
				HashMap<String,HashMap> assocNode = client.getAssocNode(toMove, CCConstants.CM_ASSOC_ORIGINAL);
				String originalNodeId = (String)assocNode.entrySet().iterator().next().getValue().get(CCConstants.SYS_PROP_NODE_UID);
				
				/**
				 * set the usage for the new collection
				 */
				Usage2Service usageService = new Usage2Service();
				Usage usage = usageService.getUsage(this.appInfo.getAppId(), parent, originalNodeId, toMove);
				client.removeNode(usage.getNodeId(), originalNodeId);
				usageService.setUsage(appInfo.getAppId(), 
						authInfo.get(CCConstants.AUTH_USERNAME), 
						appInfo.getAppId(), 
						toCollection, 
						originalNodeId, null, null, null, -1, usage.getUsageVersion(), toMove, null);
			
			}else{
				client.moveNode(toCollection, CCConstants.CM_ASSOC_FOLDER_CONTAINS, toMove);
				/**
				 * handle level0
				 */
				HashMap<String, Object> properties = client.getProperties(toMove);
				if(new Boolean((String)properties.get(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0))){
					if(Arrays.asList(client.getAspects(toCollection)).contains(CCConstants.CCM_ASPECT_COLLECTION)){
						client.setProperty(toMove, CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0, new Boolean(false).toString());
					}
				}else{
					if(!Arrays.asList(client.getAspects(toCollection)).contains(CCConstants.CCM_ASPECT_COLLECTION)){
						client.setProperty(toMove, CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0, new Boolean(true).toString());
					}
				}
			}
			
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
		}
		
	}
	
	@Override
	public void remove(String collectionId) {
		
		try{
			/**
			 * first remove the children so that the usages from the original are also removed
			 */
			HashMap<String, HashMap<String, Object>> refObjects = this.getChildren(collectionId, null);
			for(Map.Entry<String, HashMap<String, Object>> entry : refObjects.entrySet()){
				if(entry.getValue().get(CCConstants.NODETYPE).equals(CCConstants.CCM_TYPE_MAP) ){
					remove(entry.getKey());
				}
				if(entry.getValue().get(CCConstants.NODETYPE).equals(CCConstants.CCM_TYPE_IO) ){
					removeFromCollection(collectionId, entry.getKey());
				}
			}
			/**
			 * remove the collection
			 */
			String parent = client.getParents(collectionId, true).keySet().iterator().next();
			client.removeNode(collectionId, parent);
			
		}catch(Throwable e){
			throw new RuntimeException(e.getMessage());
		}	
	}
	
	@Override
	public void removeFromCollection(String collectionId, String nodeId) {
		try{
						
			List<String> assocNodes = client.getAssociationNodeIds(nodeId, CCConstants.CM_ASSOC_ORIGINAL);
			
			String originalNodeId = null;
			if(assocNodes != null && assocNodes.size() > 0){
				originalNodeId = (String)assocNodes.get(0);
			}
							
			client.removeNode(nodeId, collectionId);
			
			if(originalNodeId == null){
				logger.warn("reference object "+nodeId + " has no originId, can not remove usage");
				return;
			}
			
			Usage2Service usageService = new Usage2Service();
			
			usageService.deleteUsage(appInfo.getAppId(), 
					authInfo.get(CCConstants.AUTH_USERNAME), 
					this.appInfo.getAppId(), 
					collectionId, 
					originalNodeId, 
					nodeId);
			try{
				new RepositoryCache().remove(originalNodeId);
			}catch(Throwable t){
				// may fail if original has no access, however, this is not an issue
			}
			
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public void update(Collection collection) {
		HashMap<String,Object> props = asProps(collection);
		props.remove(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0);
		client.updateNode(collection.getNodeId(), props);
	}
	
	@Override
	public void updateAndSetScope(Collection collection) throws Exception {
		update(collection);
		setScope(collection);		
	}
	
	public HashMap<String,Object> asProps(Collection collection){
		HashMap<String,Object> props = new HashMap<String,Object>();
		if(collection.getNodeId() != null){
			props.put(CCConstants.SYS_PROP_NODE_UID, collection.getNodeId());
		}
		props.put(CCConstants.CM_PROP_TITLE, collection.getTitle());
		props.put(CCConstants.CM_NAME, NodeServiceHelper.cleanupCmName(collection.getTitle()));
		props.put(CCConstants.CM_PROP_DESCRIPTION, collection.getDescription());
		props.put(CCConstants.CCM_PROP_MAP_X, collection.getX());
		props.put(CCConstants.CCM_PROP_MAP_Y, collection.getY());
		props.put(CCConstants.CCM_PROP_MAP_Z, collection.getZ());
		props.put(CCConstants.CCM_PROP_MAP_COLLECTIONSCOPE,collection.getScope());

		props.put(CCConstants.CCM_PROP_MAP_COLLECTIONCOLOR, collection.getColor());
		props.put(CCConstants.CCM_PROP_MAP_COLLECTIONTYPE, collection.getType());
		props.put(CCConstants.CCM_PROP_MAP_COLLECTIONVIEWTYPE, collection.getViewtype());
		props.put(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0, collection.isLevel0());
		return props;
	}
	
	public Collection asCollection(HashMap<String,Object> props){
		Collection collection = new Collection();
		collection.setNodeId((String)props.get(CCConstants.SYS_PROP_NODE_UID));
		
		collection.setTitle((String)props.get(CCConstants.CM_PROP_TITLE));
		collection.setDescription((String)props.get(CCConstants.CM_PROP_DESCRIPTION));
		
		String x = (String)props.get(CCConstants.CCM_PROP_MAP_X);
		if(x != null) collection.setX(Integer.parseInt(x));
		
		String y = (String)props.get(CCConstants.CCM_PROP_MAP_Y);
		if(y != null) collection.setY(Integer.parseInt(y));
		
		String z = (String)props.get(CCConstants.CCM_PROP_MAP_Z);
		if(z != null) collection.setZ(Integer.parseInt(z));
		
		collection.setColor((String)props.get(CCConstants.CCM_PROP_MAP_COLLECTIONCOLOR));
		collection.setType((String)props.get(CCConstants.CCM_PROP_MAP_COLLECTIONTYPE));
		collection.setViewtype((String)props.get(CCConstants.CCM_PROP_MAP_COLLECTIONVIEWTYPE));		
		collection.setScope((String)props.get(CCConstants.CCM_PROP_MAP_COLLECTIONSCOPE));		
		collection.setOrderMode((String)props.get(CCConstants.CCM_PROP_MAP_COLLECTION_ORDER_MODE));		
		if(props.containsKey(CCConstants.CCM_PROP_COLLECTION_PINNED_STATUS))
			collection.setPinned(new Boolean((String)props.get(CCConstants.CCM_PROP_COLLECTION_PINNED_STATUS)));
		
		return collection;
	}
	
	@Override
	public Collection get(String storeId,String storeProtocol,String collectionId) {
		try{
			HashMap<String,Object> props = client.getProperties(storeProtocol,storeId,collectionId);
			throwIfNotACollection(storeProtocol,storeId,collectionId);
			
			Collection collection = asCollection(props);			
			collection.setChildReferencesCount(client.getChildAssociationByType(storeProtocol,storeId,collectionId, CCConstants.CCM_TYPE_IO).size());
			collection.setChildCollectionsCount(client.getChildAssociationByType(storeProtocol,storeId,collectionId, CCConstants.CCM_TYPE_MAP).size());
						
			User owner = client.getOwner(storeId,storeProtocol,collectionId);
			
			String currentUser = client.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME);
			if(!currentUser.equals(owner.getUsername()) && !client.isAdmin(currentUser)){
				//leave out username
				owner.setUsername(null);
				owner.setEmail(null);
				owner.setNodeId(null);
			}
			collection.setFromUser(currentUser.equals(owner.getUsername()));
			
			collection.setOwner(owner);
			
			String parentId = (String)props.get(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID);
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

				@Override
				public Void doWork() throws Exception {
					if(Constants.storeRef.equals(new StoreRef(storeProtocol, storeId))){
						if(Arrays.asList(client.getAspects(storeProtocol,storeId,parentId)).contains(CCConstants.CCM_ASPECT_COLLECTION)){
							collection.setLevel0(false);
						}else{
							collection.setLevel0(true);
						}
					}
					return null;
				}				
			});
			detectAndSetCollectionScope(collectionId,collection);
			return collection;
			
		} catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
		}
	}

	private void detectAndSetCollectionScope(String collectionId,Collection collection) {
		if(!CollectionDao.Scope.CUSTOM.name().equals(collection.getScope())){
			return;
		}
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				ACL permissions = permissionService.getPermissions(collectionId);
				for(ACE acl : permissions.getAces()){
					if(acl.getAuthority().equals(CCConstants.AUTHORITY_GROUP_EVERYONE)){
						collection.setScope(CollectionDao.Scope.CUSTOM_PUBLIC.name());
						break;
					}
				}
				return null;
			}
		});
	}

	@Override
	public HashMap<String,HashMap<String,Object>> getChildren(String parentId, String scope){
		
		try{
			if(parentId == null){
				
				/**
				 * @TODO owner + inherit off -> node will be found even if search is done in edu-group context 
				 * level 0 nodes -> maybe cache level 0 with an node property
				 */
				String queryString = "ASPECT:\"" + CCConstants.CCM_ASPECT_COLLECTION + "\"" + " AND @ccm\\:collectionlevel0:true";
				boolean eduGroupScope = false;
				if(SearchScope.EDU_GROUPS.name().equals(scope)){
					eduGroupScope = true;
				}
				if(SearchScope.MY.name().equals(scope)){
					queryString += " AND OWNER:\"" + authInfo.get(CCConstants.AUTH_USERNAME)+"\"";
				}
				HashMap<String,HashMap<String,Object>> returnVal = new HashMap<String,HashMap<String,Object>>();
				Set<Entry<String, HashMap<String, Object>>> searchResult = client.search(queryString,eduGroupScope).entrySet();
				for(Map.Entry<String, HashMap<String,Object>> entry : searchResult){
					String parent = (String)entry.getValue().get(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID);
					if(Arrays.asList(client.getAspects(parent)).contains(CCConstants.CCM_ASPECT_COLLECTION)){
						continue;
					}
					returnVal.put(entry.getKey(), entry.getValue());
				}
				return returnVal;
			}else{
				return client.getChildren(parentId);
			}
		} catch(Throwable e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public List<NodeRef> getChildReferences(String parentId, String scope) {
		try{
			if(parentId == null){
				
				/**
				 * @TODO owner + inherit off -> node will be found even if search is done in edu-group context 
				 * level 0 nodes -> maybe cache level 0 with an node property
				 */
				String queryString = "ASPECT:\"" + CCConstants.CCM_ASPECT_COLLECTION + "\"" + " AND @ccm\\:collectionlevel0:true";
				boolean eduGroupScope = false;
				if(Scope.EDU_GROUPS.name().equals(scope)){
					eduGroupScope = true;
				}
				
				if(Scope.MY.name().equals(scope)){
					queryString += " AND OWNER:\"" + authInfo.get(CCConstants.AUTH_USERNAME)+"\"";
				}
				
				if(SearchScope.TYPE_EDITORIAL.name().equals(scope)){
					queryString += " AND @ccm\\:collectiontype:\"" + CCConstants.COLLECTIONTYPE_EDITORIAL + "\"";
				}
				List<NodeRef> returnVal = new ArrayList<>();
				List<NodeRef> nodeRefs = client.searchNodeRefs(queryString,eduGroupScope);
				for(NodeRef nodeRef : nodeRefs){
					if(isSubCollection(nodeRef)){
						continue;
					}
					returnVal.add(nodeRef);
				}
				return returnVal;
			}else{
				List<ChildAssociationRef> children =  client.getChildrenChildAssociationRef(parentId);
				List<NodeRef> returnVal = new ArrayList<NodeRef>();
				for(ChildAssociationRef child : children){
					returnVal.add(child.getChildRef());
				}
				return returnVal;
			}
		}catch(Throwable e){
			throw new RuntimeException(e);
		}
	}

	private boolean isSubCollection(NodeRef nodeRef) {
		return AuthenticationUtil.runAsSystem(() ->{
			String parent = client.getParent(nodeRef).getParentRef().getId();
			return Arrays.asList(client.getAspects(parent)).contains(CCConstants.CCM_ASPECT_COLLECTION);
		});
	}

	public void setScope(Collection collection) throws Exception {
		String collectionId = collection.getNodeId();
		String scope = collection.getScope();
		boolean custom=(scope==null || scope.equals(Scope.CUSTOM.name()));
		org.edu_sharing.repository.client.rpc.ACL acl=new org.edu_sharing.repository.client.rpc.ACL();

		List<org.edu_sharing.repository.client.rpc.ACE> aces=new ArrayList<>();
		if(acl.getAces()!=null)
			aces.addAll(Arrays.asList(acl.getAces()));

		if(custom){
			
			if(!collection.isLevel0()) { // TODO: don't allow inherition on root level -> this variable seems to be inverted?!
				permissionService.setPermissionInherit(collectionId, false);
				return;
			}

		}
		else{
			acl.setInherited(false);
			if(scope.equals(Scope.MY.name())){
			
			}
			else if(scope.equals(Scope.EDU_ALL.name())){
				org.edu_sharing.repository.client.rpc.ACE ace2=new org.edu_sharing.repository.client.rpc.ACE();
				ace2.setAuthority(CCConstants.AUTHORITY_GROUP_EVERYONE);
				ace2.setAuthorityType(Authority.Type.EVERYONE.name());
				ace2.setPermission(CCConstants.PERMISSION_CONSUMER);
				aces.add(ace2);
			}
			else if(scope.equals(Scope.EDU_GROUPS.name())){
				
				List<EduGroup> groups = searchService.getAllOrganizations(false).getData();
				for(EduGroup group : groups){
					org.edu_sharing.repository.client.rpc.ACE ace2=new org.edu_sharing.repository.client.rpc.ACE();
					ace2=new org.edu_sharing.repository.client.rpc.ACE();
					ace2.setAuthority(group.getGroupname());
					ace2.setAuthorityType(Authority.Type.GROUP.name());
					ace2.setPermission(CCConstants.PERMISSION_CONSUMER);
					aces.add(ace2);
	
				}
			}
		}
		
		final ACL aclFinal = acl;
		if(scope.equals(Scope.MY.name())){
			// We need to set inherition
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
				@Override
				public Void doWork() throws Exception {
					permissionService.setPermissions(collectionId, aces,aclFinal.isInherited());
					return null;
				}
			});
		}
	}

	/**
	 * unpin all current collections and set the list of collection ids pinned
	 */
	@Override
	public void setPinned(String[] collections) {
		SearchToken searchToken = new SearchToken();
		searchToken.setContentType(ContentType.COLLECTIONS);
		searchToken.setLuceneString("ASPECT:"+QueryParser.escape(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION_PINNED)));
		searchToken.setMaxResult(Integer.MAX_VALUE);
		List<org.edu_sharing.service.model.NodeRef> currentPinned = searchService.search(searchToken).getData();
		for(org.edu_sharing.service.model.NodeRef pinned : currentPinned) {
			nodeService.removeAspect(pinned.getNodeId(), CCConstants.CCM_ASPECT_COLLECTION_PINNED);
			nodeService.removeProperty(pinned.getStoreProtocol(), pinned.getStoreId(), pinned.getNodeId(), CCConstants.CCM_PROP_COLLECTION_PINNED_STATUS);
			nodeService.removeProperty(pinned.getStoreProtocol(), pinned.getStoreId(), pinned.getNodeId(), CCConstants.CCM_PROP_COLLECTION_PINNED_ORDER);
		}
		int order=0;
		for(String collection : collections) {
			throwIfNotACollection(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),collection);
			nodeService.addAspect(collection, CCConstants.CCM_ASPECT_COLLECTION_PINNED);
			
			HashMap<String,Object> props = new HashMap<String,Object>();
			props.put(CCConstants.CCM_PROP_COLLECTION_PINNED_STATUS, true);
			props.put(CCConstants.CCM_PROP_COLLECTION_PINNED_ORDER, order);
			nodeService.updateNodeNative(collection, props);
			
			order++;
		}
	}

	private void throwIfNotACollection(String storeProtocol,String storeId,String collection) {
		String[] aspects=nodeService.getAspects(storeProtocol,storeId, collection);
		if(!Arrays.asList(aspects).contains(CCConstants.CCM_ASPECT_COLLECTION)) {
			throw new IllegalArgumentException("Node "+collection+" is not a collection (Aspect "+CCConstants.CCM_ASPECT_COLLECTION+" not found)");
		}
		
	}

	@Override
	public void writePreviewImage(String collectionId,InputStream is, String mimeType) throws Exception {
		//new ImageMagickContentTransformerWorker()
		is=ImageTool.autoRotateImage(is,ImageTool.MAX_THUMB_SIZE);
		client.writeContent(MCAlfrescoAPIClient.storeRef, collectionId, is, mimeType,null, CCConstants.CCM_PROP_MAP_ICON);
		ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		ThumbnailService thumbnailService = serviceRegistry.getThumbnailService();
		org.alfresco.service.cmr.repository.NodeRef ref=new org.alfresco.service.cmr.repository.NodeRef(MCAlfrescoAPIClient.storeRef,collectionId);
		PreviewCache.purgeCache(collectionId);
	}

	@Override
	public void setOrder(String parentId, String[] nodes) {
		List<NodeRef> refs=getChildReferences(parentId, null);
		int order=0;
		
		String mode=CCConstants.COLLECTION_ORDER_MODE_CUSTOM;
		if(nodes==null || nodes.length==0)
			mode=null;
		
		HashMap<String,Object> collectionProps=new HashMap<>();
		collectionProps.put(CCConstants.CCM_PROP_MAP_COLLECTION_ORDER_MODE, mode);
		nodeService.updateNodeNative(parentId, collectionProps);
		
		if(nodes==null || nodes.length==0)
			return;
		for(String node : nodes) {
			NodeRef ref=new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,node);
			if(!refs.contains(ref))
				throw new IllegalArgumentException("Node id "+node+" is not a children of the collection "+parentId);
			
			nodeService.addAspect(node, CCConstants.CCM_ASPECT_COLLECTION_ORDERED);
			HashMap<String,Object> props=new HashMap<>();
			props.put(CCConstants.CCM_PROP_COLLECTION_ORDERED_POSITION, order);
			nodeService.updateNodeNative(node, props);
			order++;
		}
	}
}
