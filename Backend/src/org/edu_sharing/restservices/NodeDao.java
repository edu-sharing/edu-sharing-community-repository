package org.edu_sharing.restservices;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.edu_sharing.repository.server.tools.NameSpaceTool;
import org.edu_sharing.repository.server.tools.XApiTool;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.node.v1.model.NodeShare;
import org.edu_sharing.restservices.node.v1.model.NotifyEntry;
import org.edu_sharing.restservices.node.v1.model.WorkflowHistory;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.shared.NodeSearch.Facette;
import org.edu_sharing.restservices.shared.NodeSearch.Facette.Value;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.*;
import org.edu_sharing.service.notification.NotificationServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.remote.RemoteObjectService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import io.swagger.util.Json;

public class NodeDao {
	private static Logger logger = Logger.getLogger(NodeDao.class);
	private static final StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	private static final String[] DAO_PERMISSIONS = new String[]{
			org.alfresco.service.cmr.security.PermissionService.ADD_CHILDREN,
			org.alfresco.service.cmr.security.PermissionService.CHANGE_PERMISSIONS,
			org.alfresco.service.cmr.security.PermissionService.WRITE,
			org.alfresco.service.cmr.security.PermissionService.DELETE,
			CCConstants.PERMISSION_CC_PUBLISH,
			CCConstants.PERMISSION_READ_ALL
	};
	public static NodeDao getNode(RepositoryDao repoDao, String nodeId)
			throws DAOException {
		return getNode(repoDao, nodeId, new Filter());
	}
	
	/** get node via shared link **/
	public static NodeDao getNode(RepositoryDao repoDao, String nodeId,String token)
			throws DAOException {
		return AuthenticationUtil.runAsSystem(new RunAsWork<NodeDao>() {			
			@Override
			public NodeDao doWork() throws Exception {		
				ShareServiceImpl service=new ShareServiceImpl();
				Share share=service.getShare(nodeId, token);
				if(share==null){
					throw new Exception("No share found for nodeId and token");
				}
				if (share.getExpiryDate() != ShareService.EXPIRY_DATE_UNLIMITED) {
					if (new Date(System.currentTimeMillis()).after(new Date(share.getExpiryDate()))) {
						throw new Exception("Share expired");
					}
				}
				share.setDownloadCount((share.getDownloadCount() + 1) );
				service.updateShare(share);
				
				return getNode(repoDao, nodeId, new Filter());
			}
		});
	}
	
	public static NodeDao getNode(RepositoryDao repoDao, String nodeId, Filter filter)
			throws DAOException {

		try {
			if(filter == null) filter = new Filter();
			return new NodeDao(repoDao, nodeId, filter);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	
	public static NodeDao getNode(RepositoryDao repoDao, String storeProtocol, String storeId,  String nodeId, Filter filter)
			throws DAOException {
		try {
			
			return new NodeDao(repoDao,storeProtocol,storeId, nodeId, filter);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public static NodeSearch search(RepositoryDao repoDao,
			org.edu_sharing.service.search.model.SearchToken searchToken) throws DAOException {
		SearchService searchService=SearchServiceFactory.getSearchService(repoDao.getId());
		return transform(repoDao,searchService.search(searchToken));
	}
	
	public static NodeSearch searchV2(RepositoryDao repoDao,MdsDaoV2 mdsDao,
			String query, List<MdsQueryCriteria> criterias,SearchToken token, Filter filter) throws DAOException {
		SearchService searchService=SearchServiceFactory.getSearchService(repoDao.getId());
		Map<String,String[]> criteriasMap=new HashMap<>();
		for(MdsQueryCriteria criteria : criterias){
			criteriasMap.put(criteria.getProperty(),criteria.getValues().toArray(new String[0]));
		}
		try {
			NodeSearch result = transform(repoDao,searchService.searchV2(mdsDao.getMds(),query,criteriasMap,token),filter);
			if(result.getCount()==0) {
				// try to search for ignorable properties to be null
				List<String> removed=slackCriteriasMap(criteriasMap,mdsDao.getMds().findQuery(query));
				result=transform(repoDao,searchService.searchV2(mdsDao.getMds(),query,criteriasMap,token),filter);
				result.setIgnored(removed);
				return result;
			}
			return result;
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}
	
	private static List<String> slackCriteriasMap(Map<String, String[]> criteriasMap, MetadataQuery metadataQuery) {
		List<String> removed=new ArrayList<>();
		for(MetadataQueryParameter param : metadataQuery.getParameters()){
			if(param.getIgnorable()>0 && criteriasMap.containsKey(param.getName())) {
				criteriasMap.put(param.getName(),null);
				removed.add(param.getName());
			}
		}
		return removed;
	}

	public static NodeSearch search(RepositoryDao repoDao,MdsDao mdsDao,
			String query, List<MdsQueryCriteria> criterias,SearchToken token, Filter filter) throws DAOException {
		SearchService searchService=SearchServiceFactory.getSearchService(repoDao.getId());
		try {
			return transform(repoDao,searchService.search(mdsDao,query,criterias,token),filter);
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}

	public static NodeSearch searchFingerprint(RepositoryDao repoDao, String nodeId, Filter filter) throws DAOException {
		SearchService searchService=SearchServiceFactory.getSearchService(repoDao.getId());
		try {
			return transform(repoDao,searchService.searchFingerPrint(nodeId),filter);
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}

	public static NodeSearch search(RepositoryDao repoDao, String query,
			int startIdx, int nrOfresults, List<String> facettes,
			int facettesMinCount, int facettesLimit) throws DAOException {

		try {
			
			SearchResultNodeRef search = ((MCAlfrescoAPIClient)repoDao.getBaseClient()).searchSolrNodeRef(query,
					startIdx, nrOfresults, facettes, facettesMinCount,
					facettesLimit);
	
			return transform(repoDao, search);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public static NodeSearch transform(RepositoryDao repoDao, SearchResultNodeRef search){
		return transform(repoDao,search,null);
	}
	
	public static NodeSearch transform(RepositoryDao repoDao, SearchResultNodeRef search, Filter filter) {

		NodeSearch result = new NodeSearch();

		List<NodeRef> data = new ArrayList<NodeRef>();
		List<Node> nodes = new ArrayList<Node>();
		result.setResult(data);
		result.setNodes(nodes);
		
		if(search==null || search.getData()==null)
			return result;
		for (org.edu_sharing.service.model.NodeRef nodeRef : search.getData()) {
			NodeRef ref = new NodeRef();
			ref.setRepo(repoDao.getId());
			ref.setId(nodeRef.getNodeId());
			
			String storeProtocol = nodeRef.getStoreProtocol();
			if(Constants.archiveStoreRef.getProtocol().equals(storeProtocol)){
				ref.setArchived(true);
			}
			
			data.add(ref);
			
			if(nodeRef.getProperties() != null){
				
				try{
					nodes.add(new NodeDao(repoDao, nodeRef, filter).asNode());
				}catch(DAOException e){
					
				}
			}
		}
		result.setCount(search.getNodeCount());
		result.setSkip(search.getStartIDX());
	
		Map<String, Map<String, Integer>> countedProps = search
				.getCountedProps();
		if (countedProps != null) {
			List<Facette> resultFacettes = new ArrayList<Facette>();
			for (Entry<String, Map<String, Integer>> entry : countedProps
					.entrySet()) {

				Facette facette = new Facette();
				facette.setProperty(entry.getKey());

				List<Value> values = new ArrayList<Value>();
				for (Entry<String, Integer> entryValue : entry.getValue()
						.entrySet()) {

					Value value = new Value();
					value.setValue(entryValue.getKey());
					value.setCount(entryValue.getValue());

					values.add(value);
				}

				Collections.sort(values,new Comparator<Value>(){

					@Override
					public int compare(Value o1, Value o2) {
						return o2.getCount().compareTo(o1.getCount());
					}

				});
				facette.setValues(values);

				resultFacettes.add(facette);
			}
			result.setFacettes(resultFacettes);
		}

		return result;
	}
	
	public static void delete(String protocol, String store, String nodeId) {
		NodeService nodeService = NodeServiceFactory.getNodeService(null);
		nodeService.removeNode(protocol, store, nodeId);
	}

	private final RepositoryDao repoDao;
	private final String nodeId;

	private final HashMap<String, Object> nodeProps;
	private HashMap<String, HashMap<String, Object>> nodeHistory;

	private HashMap<String, Boolean> hasPermissions;

	private final String type;
	private final List<String> aspects;

	private final String storeProtocol;
	
	private final String storeId;
	
	NodeService nodeService;
	CommentService commentService;

	Filter filter;

	private org.edu_sharing.service.permission.PermissionService permissionService;
	
	public static final String defaultStoreProtocol = "workspace";
	public static final String defaultStoreId = "SpacesStore";
	
	public static final String archiveStoreProtocol = "archive";
	public static final String archiveStoreId = "SpacesStore";
	
	private NodeDao(RepositoryDao repoDao, String nodeId) throws Throwable {
		this(repoDao,nodeId,new Filter());
	}
	/**
	 * return a node by a given name inside a parent folder
	 * @param repoDao
	 * @param parentId the folder to search
	 * @param type the NodeType to find
	 * @param name the CM_NAME to find
	 * @return
	 * @throws Throwable
	 */
	public static NodeDao getByParent(RepositoryDao repoDao, String parentId,String type,String name) throws Throwable {
		NodeService nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
		org.alfresco.service.cmr.repository.NodeRef ref = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId,type,CCConstants.CM_NAME, name);
		return new NodeDao(repoDao,ref.getId());
	}
	private NodeDao(RepositoryDao repoDao, String nodeId, Filter filter) throws Throwable {

		this(repoDao,defaultStoreProtocol,defaultStoreId,nodeId,filter);
	}
	
	private NodeDao(RepositoryDao repoDao, String storeProtocol, String storeId, String nodeId, Filter filter) throws DAOException {
		this(repoDao,new org.edu_sharing.service.model.NodeRefImpl(repoDao.getId(),storeProtocol,storeId,nodeId),filter);
	}

	public static String mapNodeConstants(RepositoryDao repoDao,String node) throws DAOException {
		try {
			if ("-userhome-".equals(node)) {
				node = repoDao.getUserHome();
			}
			if ("-inbox-".equals(node)) {
				node = repoDao.getUserInbox();
			}
			if ("-saved_search-".equals(node)) {
				node = repoDao.getUserSavedSearch();
			}
			return node;
		}catch (Exception e){
			throw DAOException.mapping(e);
		}
	}

    private int getCommentCount(){
		if(nodeProps.containsKey(CCConstants.VIRT_PROP_COMMENTCOUNT)){
			return (int) nodeProps.get(CCConstants.VIRT_PROP_COMMENTCOUNT);
		}
		return 0;
	}

	private NodeDao(RepositoryDao repoDao, org.edu_sharing.service.model.NodeRef nodeRef, Filter filter) throws DAOException {
		try{
	
			this.repoDao = repoDao;
			this.nodeId = nodeRef.getNodeId();
			
			this.storeProtocol = nodeRef.getStoreProtocol();
			this.storeId = nodeRef.getStoreId();
			
			this.nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
			this.permissionService = PermissionServiceFactory.getPermissionService(repoDao.getId());
			
			/**
			 * call getProperties on demand
			 */
			if(nodeRef.getProperties() == null || nodeRef.getProperties().size() == 0){
				this.nodeProps = this.nodeService.getProperties(this.storeProtocol, this.storeId, this.nodeId);
				
			}else{
				this.nodeProps = nodeRef.getProperties();
			}
	
			refreshPermissions();
			
			if(nodeProps.containsKey(CCConstants.NODETYPE)){
				this.type = (String) nodeProps.get(CCConstants.NODETYPE);
			}
			else{
				this.type = CCConstants.CCM_TYPE_IO;
			}
			
			String[] aspects = nodeService.getAspects(this.storeProtocol,this.storeId, nodeId);
			
			this.aspects = (aspects != null) ? Arrays.asList(aspects) : new ArrayList<String>();
			
			this.filter = filter;
			
		}catch(Throwable t){
			logger.warn(t.getMessage(),t);
			throw DAOException.mapping(t,nodeRef.getNodeId());
		}
	}

	public void refreshPermissions() {
        this.hasPermissions = permissionService.hasAllPermissions(storeProtocol, storeId, nodeId,DAO_PERMISSIONS);
	}

    public static NodeEntries convertToRest(RepositoryDao repoDao,Filter propFilter,List<NodeRef> children, Integer skipCount, Integer maxItems) throws DAOException {
        NodeEntries result=new NodeEntries();
        List<Node> nodes=new ArrayList<>();
        for(int i=skipCount;i<Math.min(children.size(),skipCount+maxItems);i++){
            nodes.add(NodeDao.getNode(repoDao,children.get(i).getId(),propFilter).asNode());
        }

        Pagination pagination=new Pagination();
        pagination.setFrom(skipCount);
        pagination.setCount(nodes.size());
        pagination.setTotal(children.size());
        result.setPagination(pagination);
        result.setNodes(nodes);
        return result;
    }

    public static List<NodeRef> sortApiNodeRefs(RepositoryDao repoDao, List<NodeRef> refList, List<String> filter, SortDefinition sortDefinition) {
        return NodeDao.convertAlfrescoNodeRef(repoDao,NodeDao.sortAlfrescoRefs(NodeDao.convertApiNodeRef(refList), filter, sortDefinition));
    }

	public NodeDao createChild(String type, List<String> aspects,
			HashMap<String, String[]> properties,boolean renameIfExists) throws DAOException {
		return this.createChild(type, aspects, properties, renameIfExists, null);
	}
	
	public NodeDao createChild(String type, List<String> aspects,
			HashMap<String, String[]> properties,boolean renameIfExists, String childAssoc) throws DAOException {

		try {
			NameSpaceTool<String> nameSpaceTool = new NameSpaceTool<String>();
			type = nameSpaceTool.transformToLongQName(type);
			if(childAssoc!=null)
				childAssoc=CCConstants.getValidGlobalName(childAssoc);
			HashMap<String, String[]> props = transformProperties(properties);
			String childId;
			
			String originalNameArr[] = props.get(CCConstants.CM_NAME);
			String originalName= (originalNameArr != null && originalNameArr.length > 0) ? originalNameArr[0] : null;
			
			if(originalName == null) throw new Exception("missing name");
			
			int i=2;
			while(true){
				try{
					childId = this.nodeService.createNode(nodeId, type, props, childAssoc);
					break;
				}catch(DuplicateChildNodeNameException e){
					if(renameIfExists){
						props.put(CCConstants.CM_NAME, new String[]{NodeServiceHelper.renameNode(originalName,i)});
						i++;
						continue;
					}
					throw e;
				}
			}
	
			if (aspects != null) {
				for (String aspect : aspects) {
					aspect = NameSpaceTool.transformToLongQName(aspect);
					nodeService.addAspect(childId, aspect);
				}
			}
	
			return new NodeDao(repoDao, childId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
		
	}

	public NodeDao createChildByMove(String sourceId) throws DAOException {

		try {
			
			nodeService.moveNode(nodeId, CCConstants.CM_ASSOC_FOLDER_CONTAINS,
					sourceId);
	
			return new NodeDao(repoDao, sourceId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	public NodeDao createChildByCopy(String sourceId, boolean withChildren)
			throws DAOException {

		try {
			org.alfresco.service.cmr.repository.NodeRef newNode = nodeService.copyNode(sourceId, nodeId, withChildren);
			permissionService.createNotifyObject(newNode.getId(), new AuthenticationToolAPI().getCurrentUser(), CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD);
			return new NodeDao(repoDao, sourceId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	public List<NodeRef> getChildren() throws DAOException {
		return getChildren(null,null,new SortDefinition());
	}
	public List<NodeRef> getChildren(String assocName, List<String> filter, SortDefinition sortDefinition) throws DAOException {

		try {
			List<NodeRef> result = new ArrayList<NodeRef>();
	
			
			if(assocName!=null && !assocName.isEmpty()){
				assocName=CCConstants.getValidGlobalName(assocName);
			}
			for (ChildAssociationRef childRef : nodeService.getChildrenChildAssociationRefAssoc(getId(),assocName,filter,sortDefinition)) {
	
				NodeRef ref = new NodeRef();
				ref.setRepo(this.repoDao.getId());
				ref.setHomeRepo(this.repoDao.isHomeRepo());
				ref.setId(childRef.getChildRef().getId());
				
				String storeProtocol = childRef.getChildRef().getStoreRef().getProtocol();
				if(Constants.archiveStoreRef.getProtocol().equals(storeProtocol)){
					ref.setArchived(true);
				}
			
				result.add(ref);
			}
	
			return result;
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	public List<NodeRef> getAssocs(AssocInfo assoc,List<String> filter,SortDefinition sortDefinition) throws DAOException {

		try {
			List<NodeRef> result = new ArrayList<NodeRef>();
			assoc.setAssocName(CCConstants.getValidGlobalName(assoc.getAssocName()));
            List<AssociationRef> assocs = nodeService.getNodesByAssoc(getId(), assoc);
            assocs=((NodeServiceImpl)NodeServiceFactory.getLocalService()).sortNodeRefList(assocs,filter,sortDefinition);
			for (AssociationRef childRef : assocs) {

				NodeRef ref = new NodeRef();
				ref.setRepo(this.repoDao.getId());
				ref.setHomeRepo(this.repoDao.isHomeRepo());
				ref.setId(childRef.getTargetRef().getId());

				String storeProtocol = childRef.getTargetRef().getStoreRef().getProtocol();
				if(Constants.archiveStoreRef.getProtocol().equals(storeProtocol)){
					ref.setArchived(true);
				}

				result.add(ref);
			}

			return result;

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
	}
	public NodeDao changeProperties(HashMap<String,String[]> properties)
			throws DAOException {

		try {
			
			this.nodeService.updateNode(nodeId, transformProperties(properties));
	
			return new NodeDao(repoDao, nodeId, Filter.createShowAllFilter());
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	public NodeDao changePropertiesWithVersioning(
			HashMap<String,String[]> properties, String comment) throws DAOException {

		try { 
			mergeVersionComment(properties, comment);
	
			// 1. update
			this.nodeService.updateNode(nodeId,transformProperties(properties));
	
			// 2. versioning
			this.nodeService.createVersion(nodeId,
					transformProperties(properties));
	
			return new NodeDao(repoDao, nodeId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public NodeDao changePreview(InputStream is,String mimetype) throws DAOException {

		try {
			is=ImageTool.autoRotateImage(is,ImageTool.MAX_THUMB_SIZE);
			nodeService.writeContent(storeRef, nodeId, is, mimetype, null,
					isDirectory() ? CCConstants.CCM_PROP_MAP_ICON : CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW);
			PreviewCache.purgeCache(nodeId);
			return new NodeDao(repoDao, nodeId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	public NodeDao deletePreview() throws DAOException {

		try {
			nodeService.removeProperty(storeProtocol,storeId,nodeId,isDirectory() ? CCConstants.CCM_PROP_MAP_ICON : CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW);
			PreviewCache.purgeCache(nodeId);
			return new NodeDao(repoDao, nodeId);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public NodeDao changeContent(InputStream is, String mimetype,
			String versionComment) throws DAOException {

		try {
			HashMap<String,String[]> props = new HashMap<>();
	
			boolean version=versionComment!=null && !versionComment.isEmpty();
			// 1. update
			if(version){
				props.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, new String[]{versionComment});
				//mergeVersionComment(props, versionComment);
			}
			props.put(CCConstants.CCM_PROP_IO_CREATE_VERSION,new String[]{new Boolean(version).toString()});
			nodeService.updateNode(nodeId, props);
			
	
			// 2. change content (automatic versioning)
			nodeService.writeContent(storeRef, nodeId, is, mimetype, null,
					CCConstants.CM_PROP_CONTENT);
	
			return new NodeDao(repoDao, nodeId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	private void mergeVersionComment(HashMap<String,String[]> properties,
			String versionComment) {
		
		properties.remove(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_VERSION_COMMENT));

		properties.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, new String[]{versionComment});
	}
	
	public void delete(boolean recycle) throws DAOException {
		try{
			nodeService.removeNode(nodeId, getParentId(),recycle);
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}

	public List<NodeVersion> getHistory() throws DAOException {

		List<NodeVersion> history = new ArrayList<NodeVersion>();
		for (Entry<String, HashMap<String, Object>> version : getNodeHistory().entrySet()) {
			NodeVersion nodeVersion=convertVersionProps(version.getKey(),version.getValue());
			history.add(nodeVersion);
		}
		// Sort by version
		Collections.sort(history, (o1, o2) -> {
			if(o1.getVersion().getMajor()==o2.getVersion().getMajor())
				return o1.getVersion().getMinor()>o2.getVersion().getMinor() ? 1 : -1;
			return o1.getVersion().getMajor()>o2.getVersion().getMajor() ? 1 : -1;
		});

		return history;
	}

	public NodeDao revertHistory(int major, int minor) throws DAOException {

		try {
			
			String versionLabel = getVersionLabel(major, minor);
	
			HashMap<String, Object> versionProps = getNodeHistory().get(versionLabel);
	
			if (versionProps == null) {
				return null;
			}
	
			nodeService.revertVersion(nodeId, versionLabel);
	
			return new NodeDao(repoDao, nodeId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	public List<String> getAspectsNative(){
		return this.aspects;
	}
	public Node asNode() throws DAOException {

		Node data = new Node();
		
		data.setRef(getRef());
		data.setParent(getParentRef());
		
		data.setType(getType());
		data.setIsDirectory(isDirectory());
		data.setAspects(NameSpaceTool.transFormToShortQName(this.aspects));

		data.setName(getName());
		data.setTitle(getTitle());
		data.setDescription(getDescription());

		data.setCreatedAt(getCreatedAt());
		data.setCreatedBy(getCreatedBy());
		data.setOwner(getOwner());

		data.setModifiedAt(getModifiedAt());
		data.setModifiedBy(getModifiedBy());

		data.setContentVersion(getContentVersion());
		data.setContentUrl(getContentUrl());
		
		data.setDownloadUrl(getDownloadUrl());
		data.setMetadataset(getMetadataSet());

		data.setProperties(getProperties());

		data.setAccess(getAccessAsString());

		data.setMimetype(getMimetype());
		data.setMediatype(getMediatype());
		data.setIconURL(getIconURL());
		data.setCommentCount(getCommentCount());
		data.setLicenseURL(getLicenseURL());
		data.setSize(getSize());
		try {
			data.setPreview(getPreview());
		}catch(Exception e) {
			e.printStackTrace();
		}
		data.setRepositoryType(getRepositoryType());

		if(isCollection()){
			Collection collection=new CollectionDao(repoDao, getRef().getId(),this,data).asCollection();
			data.setCollection(collection);
		}
		
		return data;
	}

	public String getType() {
		return NameSpaceTool.transformToShortQName(this.type);
	}

	private String getMetadataSet() {
		return (String)nodeProps.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
	}
	private String getLicenseURL() {
		if(isDirectory())
			return null;
		return new LicenseService().getIconUrl((String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY));
	}

	private String getIconURL() {
		return new MimeTypesV2(repoDao.getApplicationInfo()).getIcon(type,nodeProps,aspects);
	}
	public List<String> getPermissions(String authority) throws DAOException {
		try{
			return permissionService.getPermissionsForAuthority(nodeId,authority);
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}

	public NodePermissions getPermissions() throws DAOException {

		try {
			
			org.edu_sharing.repository.client.rpc.ACL permissions = null;
			try{
				
				permissions = permissionService.getPermissions(nodeId);
				
			}catch( org.alfresco.repo.security.permissions.AccessDeniedException accessDenied){
				//than you don't have the permission no ask for
				return null;
			}
			
			if (permissions == null) {
				return null;
			}
				
			NodePermissions result = new NodePermissions();
			
			ACL local = new ACL();
			local.setInherited(permissions.isInherited());
			local.setPermissions(new ArrayList<ACE>());

			result.setLocalPermissions(local);			
			result.setInheritedPermissions(new ArrayList<ACE>());

			org.edu_sharing.repository.client.rpc.ACE[] aces = permissions.getAces();
			
			if (aces != null) {
				
				HashMap<Authority,List<String>> authPerm = new HashMap<Authority,List<String>>();
				HashMap<Authority,List<String>> authPermInherited = new HashMap<Authority,List<String>>();
				for (org.edu_sharing.repository.client.rpc.ACE ace : aces) {
				
					if ("acepted".equals(ace.getAccessStatus())) {
					
						Authority authority = (Authority.Type.valueOf(ace.getAuthorityType()) == Authority.Type.GROUP )? new Group() : new Authority();
						if(authority instanceof Group) {
							Group g = (Group)authority;
							g.setGroupType(ace.getGroup().getGroupType());
							g.setEditable(ace.getGroup().isEditable());
						}
						
						authority.setAuthorityName(ace.getAuthority());
						authority.setAuthorityType(Authority.Type.valueOf(ace.getAuthorityType()));
						
						if (ace.isInherited()) {
							
							List<String> tmpPerms = authPermInherited.get(authority);
							if(tmpPerms == null){
								tmpPerms = new ArrayList<String>();
							}
							tmpPerms.add(ace.getPermission());
							authPermInherited.put(authority, tmpPerms);
								
						} else {
							List<String> tmpPerms = authPerm.get(authority);
							if(tmpPerms == null){
								tmpPerms = new ArrayList<String>();
							}
							tmpPerms.add(ace.getPermission());
							authPerm.put(authority, tmpPerms);
						}
					}
				}
				
				for(Map.Entry<Authority,List<String>> entry : authPerm.entrySet()){
					ACE ace = getACEAsSystem(entry.getKey());
					ace.setPermissions(entry.getValue());
					if(entry.getKey() instanceof Group) {
						ace.setEditable(((Group)entry.getKey()).isEditable());
					}
					result.getLocalPermissions().getPermissions().add(ace);
				}
				
				for(Map.Entry<Authority,List<String>> entry : authPermInherited.entrySet()){
					ACE ace = getACEAsSystem(entry.getKey());
					ace.setPermissions(entry.getValue());
					result.getInheritedPermissions().add(ace);
				}
				
			}
			
			return result;
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
		
	}
	
	private ACE getACEAsSystem(Authority key){
		return AuthenticationUtil.runAsSystem(new RunAsWork<ACE>() {

			@Override
			public ACE doWork() throws Exception {
				try{
					if(key.getAuthorityType().name().equals("GROUP")){
						GroupProfile group=GroupDao.getGroup(repoDao, key.getAuthorityName()).asGroup().getProfile();
						return new ACE(key,null,group);
					}else if(key.getAuthorityType().name().equals("EVERYONE")){
						//@TODO check if authority must be changed
						GroupProfile group=GroupDao.getGroup(repoDao, key.getAuthorityName()).asGroup().getProfile();
						return new ACE(key,null,group);
					}else if(key.getAuthorityName().equals("ROLE_OWNER")) {
						return new ACE(key,null,null);
					}
					else{
						UserProfile user=PersonDao.getPerson(repoDao, key.getAuthorityName()).asPerson().getProfile();
						return new ACE(key,user,null);
					}
				}catch(DAOException e){
					// this may happens for a virtual user, e.g. GROUP_EVERYONE
					return new ACE(key,null,null);
				}
			}
		});
	}

	public void setPermissions(ACL permissions, String mailText, Boolean sendMail, Boolean sendCopy, boolean createHandle) throws DAOException {
		
		try {
			
			List<org.edu_sharing.repository.client.rpc.ACE> aces = new ArrayList<org.edu_sharing.repository.client.rpc.ACE>(); 

			for (ACE permission : permissions.getPermissions()) {
				
				for(String tmpPerm : permission.getPermissions()){
					org.edu_sharing.repository.client.rpc.ACE ace = new org.edu_sharing.repository.client.rpc.ACE();
					
					ace.setAccessStatus("acepted");
					
					ace.setAuthority(permission.getAuthority().getAuthorityName());
					ace.setAuthorityType(permission.getAuthority().getAuthorityType().name());
					
					ace.setPermission(tmpPerm);
					
					aces.add(ace);
				}
			}
			
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(repoDao.getId());
			permissionService.setPermissions(
					nodeId, 
					aces,
					permissions.isInherited(), 
					mailText, sendMail, sendCopy,createHandle);
			
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	private HashMap<String,String[]> transformProperties(
			HashMap<String,String[]> properties) {
		return NodeServiceHelper.transformShortToLongProperties(properties);
	}

	private String getId() {

		return (String) nodeProps.get(CCConstants.SYS_PROP_NODE_UID);
	}

	public boolean isDirectory(){
		return MimeTypesV2.isDirectory(nodeProps);
	}
	
	public boolean isCollection(){
		return MimeTypesV2.isCollection(nodeProps);
	}
	
	public NodeRef getRef() {

		NodeRef nodeRef = createNodeRef(repoDao,nodeId);
		String storeProtocol = (String)this.nodeProps.get(CCConstants.SYS_PROP_STORE_PROTOCOL);
		if(Constants.archiveStoreRef.getProtocol().equals(storeProtocol)){
			nodeRef.setArchived(true);
		}

		return nodeRef;
	}

	private NodeRef getParentRef() {
		return createNodeRef(repoDao,getParentId());
	}
	public static NodeRef createNodeRef(RepositoryDao repoDao,String nodeId) {
		NodeRef parentRef = new NodeRef();
		parentRef.setRepo(repoDao.getId());
		parentRef.setId(nodeId);
		parentRef.setHomeRepo(repoDao.isHomeRepo());		
		return parentRef;
	}
	
	public String getStoreProtocol(){
		return (String)nodeProps.get(CCConstants.SYS_PROP_STORE_PROTOCOL);
	}
	
	public String getStoreIdentifier(){
		return (String)nodeProps.get(CCConstants.SYS_PROP_STORE_IDENTIFIER);
	}
	
	private String getParentId() {

		return (String) nodeProps
				.get(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID);
	}

	private String getName() {

		return (String) nodeProps.get(CCConstants.CM_NAME);
	}

	private String getTitle() {

		return (String) nodeProps.get(CCConstants.LOM_PROP_GENERAL_TITLE);
	}

	private String getDescription() {

		return (String) nodeProps.get(CCConstants.LOM_PROP_GENERAL_DESCRIPTION);
	}

	public Date getCreatedAt() {

		String key = CCConstants.CM_PROP_C_CREATED
				+ CCConstants.LONG_DATE_SUFFIX;

		return nodeProps.containsKey(key) ? new Date(
				Long.parseLong((String) nodeProps.get(key))) : null;
	}

	public Person getCreatedBy() {

		Person ref = new Person();
		ref.setFirstName((String) nodeProps
				.get(CCConstants.NODECREATOR_FIRSTNAME));
		ref.setLastName((String) nodeProps
				.get(CCConstants.NODECREATOR_LASTNAME));
		ref.setMailbox((String) nodeProps.get(CCConstants.NODECREATOR_EMAIL));
		return ref;
	}
	
	private Person getOwner() {
		User owner=nodeService.getOwner(storeId, storeProtocol, nodeId);
		if(owner==null)
			return null;
		Person ref = new Person();
		ref.setFirstName(owner.getGivenName());
		ref.setLastName(owner.getSurname());
		ref.setMailbox(owner.getEmail());
		return ref;
	}

	private Date getModifiedAt() {

		String key = CCConstants.CM_PROP_C_MODIFIED
				+ CCConstants.LONG_DATE_SUFFIX;

		return nodeProps.containsKey(key) ? new Date(
				Long.parseLong((String) nodeProps.get(key))) : null;
	}

	private Person getModifiedBy() {

		Person ref = new Person();

		ref.setFirstName((String) nodeProps
				.get(CCConstants.NODEMODIFIER_FIRSTNAME));
		ref.setLastName((String) nodeProps
				.get(CCConstants.NODEMODIFIER_LASTNAME));
		ref.setMailbox((String) nodeProps.get(CCConstants.NODEMODIFIER_EMAIL));

		return ref;
	}

	private String getContentVersion() {
		return (String) nodeProps.get(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
	}

	private String getContentUrl() {
		return (String) nodeProps.get(CCConstants.CONTENTURL);
	}
	
	private boolean isLink(){
		return nodeProps.containsKey(CCConstants.CCM_PROP_IO_WWWURL);
	}
	private String getDownloadUrl(){
		if(isLink())
			return null;
		// no download url if user can not access the content
		if(!getAccessAsString().contains(CCConstants.PERMISSION_READ_ALL))
			return null;
		return (String) nodeProps.get(CCConstants.DOWNLOADURL);
	}

	public NodeVersion getVersion(int major, int minor) throws DAOException {

		String versionLabel = getVersionLabel(major, minor);

		HashMap<String, Object> versionProps = getNodeHistory().get(versionLabel);

		return convertVersionProps(versionLabel, versionProps);
	}

	private NodeVersion convertVersionProps(String versionLabel, HashMap<String, Object> versionProps) throws DAOException {
		if (versionProps == null) {
			return null;
		}

		NodeVersion result = new NodeVersion();

		result.setVersion(transformVersion(versionLabel));

		result.setComment((String) versionProps
				.get(CCConstants.CCM_PROP_IO_VERSION_COMMENT));
		result.setContentUrl((String) versionProps.get(CCConstants.CONTENTURL));

		String keyCreated = CCConstants.CM_PROP_C_MODIFIED
				+ CCConstants.LONG_DATE_SUFFIX;
		result.setModifiedAt(versionProps.containsKey(keyCreated) ? ((String) versionProps.get(keyCreated)) : null);

		Person ref = new Person();

		ref.setFirstName((String) versionProps
				.get(CCConstants.NODEMODIFIER_FIRSTNAME));
		ref.setLastName((String) versionProps
				.get(CCConstants.NODEMODIFIER_LASTNAME));
		ref.setMailbox((String) versionProps.get(CCConstants.NODEMODIFIER_EMAIL));

		result.setModifiedBy(ref);

		result.setProperties(getProperties(versionLabel));

		return result;
	}

	private String getVersionLabel(int major, int minor) {
		return major + "." + minor;
	}

	private NodeVersionRef transformVersion(String versionLabel) {

		String[] versionTokens = versionLabel.split("\\.");

		NodeVersionRef version = new NodeVersionRef();
		version.setNode(getRef());
		version.setMajor(Integer.parseInt(versionTokens[0]));
		version.setMinor(Integer.parseInt(versionTokens[1]));
		version.setMinor(Integer.parseInt(versionTokens[1]));

		return version;
	}

	private List<NodeAccess> getAccess() {

		List<NodeAccess> result = new ArrayList<NodeAccess>();

		for (String permission : PermissionServiceHelper.PERMISSIONS) {

			NodeAccess access = new NodeAccess();
			access.setPermission(permission);
			access.setRight(hasPermissions.get(permission));

			result.add(access);
		}

		return result;
	}
	
	public List<String> getAccessAsString() {
		return PermissionServiceHelper.getPermissionsAsString(hasPermissions);
	}
	
	public HashMap<String,Object> getNativeProperties() throws DAOException {
		return nodeProps;
	}
	
	public HashMap<String,String[]> getAllProperties() throws DAOException {
		return getProperties(null,Filter.createShowAllFilter());
	}
	
	private HashMap<String,String[]> getProperties() throws DAOException {
		return getProperties(null);
	}
	
	private HashMap<String,String[]>  getProperties(String versionLabel) throws DAOException {
		return getProperties(versionLabel,filter);
	}
	
	public HashMap<String, Object> getNativeProperties(String versionLabel) throws DAOException{
		return versionLabel != null ? getNodeHistory()
				.get(versionLabel) : nodeProps;
	}
	
	public void addWorkflowHistory(WorkflowHistory history) throws DAOException{
		HashMap<String, String[]> properties = getAllProperties();
		String[] data=properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_PROTOCOL));
		List<String> list=new ArrayList<>();
		if(data!=null)
			list=new ArrayList<>(Arrays.asList(data));
		try{
			String[] receivers=new String[history.getReceiver().length];
			for(int i=0;i<receivers.length;i++){
				receivers[i]=history.getReceiver()[i].getAuthorityName();
			}
			JSONObject json=new JSONObject();
			json.put("comment", history.getComment());
			json.put("editor", (history.getEditor()!=null && !history.getEditor().getAuthorityName().isEmpty()) ? history.getEditor().getAuthorityName() : AuthenticationUtil.getFullyAuthenticatedUser());
			json.put("receiver", receivers);
			json.put("status", history.getStatus());
			json.put("time", history.getTime()>0 ? history.getTime() : System.currentTimeMillis());
			list.add(0, json.toString());
			String[] result=new String[0];
			result=list.toArray(result);
			properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_INSTRUCTIONS),new String[]{history.getComment()});
			properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_RECEIVER),receivers);
			properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_STATUS),new String[]{history.getStatus()});
			properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_PROTOCOL),result);
			changeProperties(properties);
		}catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public List<WorkflowHistory> getWorkflowHistory() throws DAOException{
		List<WorkflowHistory> workflow=new ArrayList<>();
		String[] data=getAllProperties().get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_PROTOCOL));
		if(data==null)
			return workflow;
		try{
			for(String entry : data){
				JSONObject json=new JSONObject(entry);
				WorkflowHistory history=new WorkflowHistory();
				if(json.has("comment"))
					history.setComment(json.getString("comment"));
				try{
					history.setEditor(new PersonDao(repoDao,json.getString("editor")).asPersonSimple());
				}catch(Throwable t){
					// The user may has no permission or entry deleted
					history.setEditor(new UserSimple());
					history.getEditor().setAuthorityName(json.getString("editor"));
				}
				JSONArray arr = json.getJSONArray("receiver");
				Authority[] list = new Authority[arr.length()];
				for(int i = 0; i < arr.length(); i++){
					try{
						String authority = arr.getString(i);
						if(authority.startsWith(PermissionService.GROUP_PREFIX)) {
							list[i]=new GroupDao(repoDao, authority).asGroup();
						}else {
							list[i]=new PersonDao(repoDao,authority).asPersonSimple();
						}			
					}catch(Throwable t){
						// The user may has no permission or entry deleted
						list[i] = new UserSimple();
						list[i].setAuthorityName(arr.getString(i));
					}
				}
				history.setReceiver(list);
				history.setStatus(json.getString("status"));
				history.setTime(Long.parseLong(json.getString("time")));
				
				workflow.add(history);
			}
			return workflow;

		}catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	
	private HashMap<String,String[]>  getProperties(String versionLabel,Filter filter) throws DAOException {

		HashMap<String, Object> props = getNativeProperties(versionLabel);
	
		if (props == null) {
			return null;
		}
		
		if(filter.getProperties().size() == 0){
			return new HashMap<String, String[]>();
		}

		ValueTool vt = new ValueTool();
		
		HashMap<String,String[]> properties = new HashMap<String,String[]>();
		for (Entry<String, Object> entry : props.entrySet()) {

			List<String> values = getPropertyValues(vt, entry.getValue());

			String shortPropName = NameSpaceTool.transformToShortQName(entry.getKey());
			
			if(shortPropName != null){
				
				if(filter.getProperties().size() > 0 && 
						!filter.getProperties().contains(Filter.ALL) 
						&& !filter.getProperties().contains(shortPropName)){
					continue;
				}
				if(props.containsKey(entry.getKey()+CCConstants.LONG_DATE_SUFFIX)){
					values = getPropertyValues(vt, props.get(entry.getKey()+CCConstants.LONG_DATE_SUFFIX));
					properties.put(shortPropName, values.toArray(new String[values.size()]));
				}
				else{
					properties.put(shortPropName, values.toArray(new String[values.size()]));
				}
			}
			
		}

		return properties;
	}

	private List<String> getPropertyValues(ValueTool vt,Object value) {
		List<String> values = new ArrayList<String>();
		if (value != null ){
			for (String mv : vt.getMultivalue(value.toString())) {
				values.add(mv);
			}
		}
		return values;
	}

	private String getMimetype() {
		return MimeTypesV2.getMimeType(nodeProps);
	}
	
	private String getMediatype() {
		return MimeTypesV2.getNodeType(type,nodeProps,aspects);
	}

	private String getSize() {
		return nodeProps.containsKey(CCConstants.LOM_PROP_TECHNICAL_SIZE) ? (String) nodeProps
						.get(CCConstants.LOM_PROP_TECHNICAL_SIZE) : null;
	}
	
	private String getRepositoryType(){
		return repoDao.getApplicationInfo().getRepositoryType();
	}
	
	private Preview getPreview() {
		Preview result = new Preview(	getRepositoryType(),
										getRef().getId(),getStoreProtocol(),
										getStoreIdentifier(),
										nodeProps);
			return result;
	}
	
	private String getPreviewImage() {
		return new MimeTypesV2(repoDao.getApplicationInfo()).getPreview(type,nodeProps,aspects);
	}

	public HashMap<String, HashMap<String, Object>> getNodeHistory() throws DAOException {
		try{
			if(nodeHistory == null){
				this.nodeHistory = new HashMap<String, HashMap<String, Object>>();
	
				HashMap<String, HashMap<String, Object>> versionHistory = nodeService
						.getVersionHistory(nodeId);
				
				if (versionHistory != null) {
	
					for (HashMap<String, Object> version : versionHistory.values()) {
						nodeHistory.put((String) version
								.get(CCConstants.CM_PROP_VERSIONABLELABEL), version);
					}
				}
			}
			return nodeHistory;
		}catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	public List<NotifyEntry> getNotifys() throws DAOException {
		try{
			throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
			List<Notify> notifys = permissionService.getNotifyList(nodeId);
			List<NotifyEntry> result = new ArrayList<>(notifys.size());
			for(Notify notify : notifys){
				result.add(new NotifyEntry(notify));
			}
			return result;
		}
		catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	private void throwIfPermissionIsMissing(String permission) throws DAOSecurityException {
		if(!permissionService.hasPermission(storeProtocol,storeId,nodeId,permission)){
			throw new DAOSecurityException(new SecurityException("Current user has no "+permission+" on node "+nodeId));
		}
	}

	public static List<NodeRef> getFilesSharedByMe(RepositoryDao repoDao,List<String> filter,SortDefinition sortDefinition) throws DAOException {
		SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
		try {
			List<org.alfresco.service.cmr.repository.NodeRef> refs = searchService.getFilesSharedByMe();
            refs=sortAlfrescoRefs(refs,filter,sortDefinition);
            return convertAlfrescoNodeRef(repoDao,refs);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}

    public static List<org.alfresco.service.cmr.repository.NodeRef> sortAlfrescoRefs(List<org.alfresco.service.cmr.repository.NodeRef> refs, List<String> filter, SortDefinition sortDefinition) {
        return NodeServiceFactory.getLocalService().sortNodeRefList(refs,filter,sortDefinition);
    }

    /**
	 * All files the current user is a receiver of the workflow
	 * @param repoDao
	 * @return
	 * @throws DAOException
	 */
	public static List<NodeRef> getWorkflowReceive(RepositoryDao repoDao,List<String> filter, SortDefinition sortDefinition) throws DAOException {
		SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
		try {
			List<org.alfresco.service.cmr.repository.NodeRef> refs = searchService.getWorkflowReceive(AuthenticationUtil.getFullyAuthenticatedUser());
			refs=NodeDao.sortAlfrescoRefs(refs,filter,sortDefinition);
			return convertAlfrescoNodeRef(repoDao,refs);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	
	public static List<NodeRef> getFilesSharedToMe(RepositoryDao repoDao, List<String> filter, SortDefinition sortDefinition) throws DAOException {
		SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
		try {
			List<org.alfresco.service.cmr.repository.NodeRef> refs = searchService.getFilesSharedToMe();
            refs=NodeDao.sortAlfrescoRefs(refs,filter,sortDefinition);
            return convertAlfrescoNodeRef(repoDao,refs);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}

	public List<NodeShare> getShares(String email) throws DAOSecurityException {
		throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
		ShareServiceImpl service = new ShareServiceImpl();
		List<NodeShare> entries=new ArrayList<>();
		for(Share share : service.getShares(this.nodeId)){
			if(email==null || email.equals(share.getEmail()))
				entries.add(new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef,this.nodeId),share));
		}
		return entries;
	}

	public NodeShare createShare(long expiryDate,String password) throws DAOException {
		ShareServiceImpl service = new ShareServiceImpl();
		try {
			throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
			return new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef,this.nodeId),service.createShare(nodeId, expiryDate,password));
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	
	public void removeShare(String shareId) throws DAOException {
		throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
		ShareServiceImpl service=new ShareServiceImpl();
    	for(Share share : service.getShares(this.nodeId)){
    		if(share.getNodeId().equals(shareId)){
    			service.removeShare(shareId);
    			return;
    		}
		}
    	throw DAOException.mapping(new Exception("share "+shareId+" was not found on node "+nodeId));
	}
	
	public NodeShare updateShare(String shareId, long expiryDate, String password) throws DAOException {
		throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
		ShareServiceImpl service=new ShareServiceImpl();
    	for(Share share : service.getShares(this.nodeId)){
    		if(share.getNodeId().equals(shareId)){
    			share.setExpiryDate(expiryDate);
    			share.setPassword(password);
    			service.updateShare(share);
    			return new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef,this.nodeId),share);
    		}
		}
    	throw DAOException.mapping(new Exception("share "+shareId+" was not found on node "+nodeId));
	}
	
	public NodeDao importNode(String parent) throws DAOException {
		try{
			String result=nodeService.importNode(this.nodeId,parent);
			return new NodeDao(RepositoryDao.getRepository(RepositoryDao.HOME),result);
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

 	public void createVersion(String comment) throws DAOException, Exception {
		this.changePropertiesWithVersioning(getAllProperties(), comment);
	}

    public static List<org.alfresco.service.cmr.repository.NodeRef> convertApiNodeRef(List<NodeRef> refs) {
        List<org.alfresco.service.cmr.repository.NodeRef> converted=new ArrayList<>(refs.size());
        for(NodeRef ref : refs){
            converted.add(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,ref.getId()));
        }
        return converted;
    }
 	public static List<NodeRef> convertAlfrescoNodeRef(RepositoryDao repoDao,List<org.alfresco.service.cmr.repository.NodeRef> refs){
 		List<NodeRef> converted=new ArrayList<>(refs.size());
 		for(org.alfresco.service.cmr.repository.NodeRef ref : refs){
 			converted.add(new NodeRef(repoDao.getId(),ref.getId()));
 		}
 		return converted;
 	}

    public void reportNode(String reason, String userEmail, String userComment) throws DAOException {
		try{
			NotificationServiceFactory.getNotificationService(repoDao.getApplicationInfo().getAppId())
			.notifyNodeIssue(nodeId, reason, userEmail, userComment);
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
		
	}
	/** store a new search node 
	 * @return 
	 * @throws DAO */
	public static NodeDao saveSearch(String repoId, String mdsId, String query, String name,
			List<MdsQueryCriteria> parameters,boolean replace) throws DAOException {
		try{
    		String parent = RepositoryDao.getHomeRepository().getUserSavedSearch();
    		NodeDao parentDao = new NodeDao(RepositoryDao.getHomeRepository(), parent);
    		HashMap<String, String[]> props=new HashMap();
    		props.put(CCConstants.CM_NAME, new String[]{name});
    		props.put(CCConstants.LOM_PROP_GENERAL_TITLE, new String[]{name});
    		props.put(CCConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY, new String[]{repoId});
    		props.put(CCConstants.CCM_PROP_SAVED_SEARCH_MDS, new String[]{mdsId});
    		props.put(CCConstants.CCM_PROP_SAVED_SEARCH_QUERY, new String[]{query});
    		props.put(CCConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS, new String[]{Json.pretty(parameters)});
    		props.put(CCConstants.CCM_PROP_IO_CREATE_VERSION, new String[]{"true"});
    		try{
    			return parentDao.createChild(CCConstants.CCM_TYPE_SAVED_SEARCH, null, props, false);
    		}catch(DAOException e){
    			if(e.getCause() instanceof DuplicateChildNodeNameException && replace){
	    			NodeDao old = NodeDao.getByParent(RepositoryDao.getHomeRepository(), parent, CCConstants.CCM_TYPE_SAVED_SEARCH, NodeServiceHelper.cleanupCmName(name));
	    			old.changeProperties(props);
	    			return old;
    			}
    			throw e;
    		}
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
		
	}
	
	public static NodeRemote prepareUsage(String repId, String nodeId) throws DAOException, Throwable{
 		
 		String tmpNodeId = new RemoteObjectService().getRemoteObject(repId, nodeId);
 		
 		NodeRemote nodeRemote = new NodeRemote();
		nodeRemote.setNode(new NodeDao(RepositoryDao.getRepository(repId),nodeId,Filter.createShowAllFilter()).asNode());
 		if(!tmpNodeId.equals(nodeId)) {
 			nodeRemote.setRemote(new NodeDao(RepositoryDao.getRepository(RepositoryDao.HOME),tmpNodeId,Filter.createShowAllFilter()).asNode());
 		}
 		
 		return nodeRemote;
 		
 	}

	public void addAspects(List<String> aspects) {
		for(String aspect : aspects) {
			nodeService.addAspect(nodeId, CCConstants.getValidGlobalName(aspect));
		}
	}
	public boolean getTemplateStatus() throws DAOException {
		Object value = this.getNativeProperties().getOrDefault(CCConstants.CCM_PROP_METADATA_PRESETTING_STATUS,false);
		if(value instanceof String){
			return Boolean.valueOf((String) value);
		}
		return (boolean)value;
	}
	public NodeDao getTemplateNode() throws DAOException {
		try {
			String template = nodeService.getTemplateNode(nodeId,false);
			if(template==null)
				return null;
			return NodeDao.getNode(repoDao, template,Filter.createShowAllFilter());
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
	public NodeDao changeTemplateProperties(Boolean enable,HashMap<String, String[]> properties) throws DAOException {
		try {
			nodeService.setTemplateStatus(nodeId, enable);
			if(enable) {
				nodeService.setTemplateProperties(nodeId, transformProperties(properties));
			}
			return getTemplateNode();
		}
		catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public void setOwner(String username) {
		nodeService.setOwner( this.getId(), username);
	}

	public void setProperty(String property, String value) {
		nodeService.setProperty(Constants.storeRef.getProtocol(), Constants.storeRef.getIdentifier(), this.getId(), property, value);
	}

	public NodeDao createFork(String sourceId) throws DAOException {
		try {
			org.alfresco.service.cmr.repository.NodeRef newNode = nodeService.copyNode(sourceId, nodeId, false);
			permissionService.createNotifyObject(newNode.getId(), new AuthenticationToolAPI().getCurrentUser(),	CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD);
			nodeService.addAspect(newNode.getId(),CCConstants.CCM_ASPECT_FORKED);
			nodeService.setProperty(newNode.getStoreRef().getProtocol(),newNode.getStoreRef().getIdentifier(),newNode.getId(),CCConstants.CCM_PROP_FORKED_ORIGIN,
					new org.alfresco.service.cmr.repository.NodeRef(storeProtocol,storeId,sourceId));
			nodeService.setProperty(newNode.getStoreRef().getProtocol(),newNode.getStoreRef().getIdentifier(),newNode.getId(),CCConstants.CCM_PROP_FORKED_ORIGIN_VERSION,
					nodeService.getProperty(storeProtocol,storeId,sourceId,CCConstants.LOM_PROP_LIFECYCLE_VERSION));
			return new NodeDao(repoDao, newNode.getId());
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public String storeXApiData(String xApi) throws DAOException {
		try {
			JSONObject json = new JSONObject(xApi);
			return XApiTool.sendToXApi(nodeId,json);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}
}
