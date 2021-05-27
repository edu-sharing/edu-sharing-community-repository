package org.edu_sharing.service.nodeservice;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.NameSpaceTool;
import org.edu_sharing.repository.server.tools.NodeTool;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.service.permission.PermissionException;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.permission.RestrictedAccessException;
import org.edu_sharing.service.search.model.SortDefinition;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class NodeServiceHelper {
	private static final String SEPARATOR = "/";
	private static final Lock lock = new ReentrantLock();
	private static final Map<String, String> cache = new HashMap<>();
	private static Logger logger=Logger.getLogger(NodeServiceHelper.class);

	/**
	 * Clean the CM_NAME property so it does not cause an org.alfresco.repo.node.integrity.IntegrityException
	 * @param cmNameReadableName
	 * @return
	 */
	public static String cleanupCmName(String cmNameReadableName){
		return EduSharingNodeHelper.cleanupCmName(cmNameReadableName);
	}

	/**
	 * enable or disable the create version for the node
	 * Note:Only works for local nodes!
	 */
	public static void setCreateVersion(String nodeId, boolean create) {
		new MCAlfrescoAPIClient().setProperty(nodeId, CCConstants.CCM_PROP_IO_CREATE_VERSION, create);
	}
	public static <T> Map<String, T> transformLongToShortProperties(Map<String, T> properties) {
		HashMap<String, T> result = new HashMap<>();
		for(Map.Entry<String, T> prop: properties.entrySet()){
			if(CCConstants.getValidLocalName(prop.getKey()) != null) {
				result.put(CCConstants.getValidLocalName(prop.getKey()), prop.getValue());
			}
		}
		return result;
	}
	public static HashMap<String, String[]> transformShortToLongProperties(HashMap<String, String[]> properties) {

		/**
		 * shortNames to long names
		 */
		HashMap<String,String[]>  propsLongKeys = (HashMap<String,String[]>)new NameSpaceTool<String[]>()
				.transformKeysToLongQname(properties);

		HashMap<String, String[]> result = new HashMap<String, String[]>();

		for (Map.Entry<String,String[]> property : propsLongKeys.entrySet()) {
			if(result.containsKey(property.getKey())) continue;

			result.put(property.getKey(), property.getValue());
		}

		return result;
	}

	public static List<Map<String,Object>> getSubobjects(NodeService service, String nodeId) throws Throwable {
		List<Map<String,Object>> result = new ArrayList<>();
		List<String> filter=new ArrayList<>();
		filter.add("files");
		SortDefinition sort=new SortDefinition();
		sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_CHILDOBJECT_ORDER),true));
		sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CM_NAME),true));
		List<ChildAssociationRef> childs=service.getChildrenChildAssociationRefAssoc(nodeId,null,filter,sort);
		for(ChildAssociationRef child : childs) {
			NodeRef ref = child.getChildRef();
			HashMap<String, Object> props = service.getProperties(ref.getStoreRef().getProtocol(),ref.getStoreRef().getIdentifier(),ref.getId());
			result.add(props);
		}
		return result;
	}

    public static boolean isChildOf(NodeService nodeService,String childId, String parentId) {
		for(ChildAssociationRef ref : nodeService.getChildrenChildAssociationRef(parentId)){
			if(ref.getChildRef().getId().equals(childId))
				return true;
		}
		return false;
    }

	/**
	 * return the property from a local stored node via a node ref (shortcut)
	 * @param nodeRef
	 * @param key
	 * @return
	 */
    public static String getProperty(NodeRef nodeRef,String key){
		return NodeServiceFactory.getLocalService().getProperty(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId(),key);
	}
	public static void setProperty(NodeRef nodeRef,String key, Serializable value){
		NodeServiceFactory.getLocalService().setProperty(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId(),key,value);
	}
	public static void addAspect(NodeRef nodeRef,String aspect){
		NodeServiceFactory.getLocalService().addAspect(nodeRef.getId(),aspect);
	}
	public static void removeAspect(NodeRef nodeRef,String aspect){
		NodeServiceFactory.getLocalService().removeAspect(nodeRef.getId(),aspect);
	}
	public static void removeProperty(NodeRef nodeRef,String key){
		NodeServiceFactory.getLocalService().removeProperty(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId(),key);
	}
	public static Serializable getPropertyNative(NodeRef nodeRef, String key){
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		return serviceRegistry.getNodeService().getProperty(nodeRef,QName.createQName(key));
	}
	public static String getType(NodeRef nodeRef){
		return NodeServiceFactory.getLocalService().getType(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId());
	}
	public static String[] getAspects(NodeRef nodeRef){
		return NodeServiceFactory.getLocalService().getAspects(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId());
	}
	public static boolean hasAspect(NodeRef nodeRef,String aspect){
		return NodeServiceFactory.getLocalService().hasAspect(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId(),aspect);
	}
    public static HashMap<String, Object> getProperties(NodeRef nodeRef) throws Throwable {
        return NodeServiceFactory.getLocalService().getProperties(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId());
    }
	public static HashMap<String, Object> getPropertiesVersion(NodeRef nodeRef, String version) throws Throwable {
    	if(version == null){
    		return getProperties(nodeRef);
		}
		HashMap<String, HashMap<String, Object>> versionHistory = NodeServiceFactory.getLocalService().getVersionHistory(nodeRef.getId());

		if (versionHistory != null) {
			for (HashMap<String, Object> versionData : versionHistory.values()) {
				if(version.equals(versionData.get(CCConstants.CM_PROP_VERSIONABLELABEL))){
					return versionData;
				}
			}
		}
		throw new IllegalArgumentException("No version " + version +" found for node " + nodeRef);
	}

	/**
	 * return the native properties
	 * this means:
	 *  - no edu-sharing caches
	 *  - no post-processing for dates or valuespaces
	 *  - no type conversion, use raw alfresco types
	 * @return
	 */
	public static Map<QName, Serializable> getPropertiesNative(NodeRef nodeRef) throws Throwable {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		return serviceRegistry.getNodeService().getProperties(nodeRef);
	}
	public static InputStream getContent(NodeRef nodeRef) throws Throwable {
		return NodeServiceFactory.getLocalService().getContent(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId(),null, ContentModel.PROP_CONTENT.toString());
	}

	public static void validatePermissionRestrictedAccess(NodeRef nodeRef, String permission) throws RestrictedAccessException {
		if(NodeServiceHelper.hasAspect(nodeRef,CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
			String refNodeId = NodeServiceHelper.getProperty(nodeRef, CCConstants.CCM_PROP_IO_ORIGINAL);
			Boolean restricted = (Boolean) AuthenticationUtil.runAsSystem(() -> NodeServiceHelper.getPropertyNative(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, refNodeId), CCConstants.CCM_PROP_RESTRICTED_ACCESS));
			if (restricted != null && restricted && !PermissionServiceHelper.hasPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, refNodeId), permission)) {
				throw new RestrictedAccessException(refNodeId);
			}
		}
	}
	public static void writeContent(NodeRef nodeRef,InputStream content,String mimetype) throws Throwable {
		NodeServiceFactory.getLocalService().writeContent(
				nodeRef.getStoreRef(),
				nodeRef.getId(),
				content,
				mimetype,
				null,
				ContentModel.PROP_CONTENT.toString()
		);
	}

	/**
	 * write the given text as text/plain content to node
	 * @param nodeRef
	 * @param content
	 * @throws Throwable
	 */
	public static void writeContentText(NodeRef nodeRef,String content) throws Throwable {
		NodeServiceFactory.getLocalService().writeContent(
				nodeRef.getStoreRef(),
				nodeRef.getId(),
				new ByteArrayInputStream(content.getBytes()),
				"text/plain",
				null,
				ContentModel.PROP_CONTENT.toString()
		);
	}
    /**
     * Get all properties automatically splitted by multivalue
     * Each property is always returned as an array
     * @return
     * @throws Throwable
     */
    public static HashMap<String, String[]> getPropertiesMultivalue(Map<String, ?> properties) {
        HashMap<String, String[]> propertiesMultivalue = new HashMap<>();
        if(properties!=null) {
			properties.forEach((key, value) -> propertiesMultivalue.put(key, value==null ? null : ValueTool.getMultivalue(value.toString())));
			return propertiesMultivalue;
		}
        return null;
    }
	/**
	 * Get all properties automatically concated from multivalue to singlevalue
	 * Each property is always returned as an array
	 * @return
	 * @throws Throwable
	 */
	public static HashMap<String, Object> getPropertiesSinglevalue(Map<String, String[]> properties) {
		HashMap<String, Object> propertiesMultivalue = new HashMap<>();
		if(properties!=null) {
			properties.forEach((key, value) -> convertMutlivaluePropToGeneric(value, propertiesMultivalue, key));
			return propertiesMultivalue;
		}
		return null;
	}
    public static boolean downloadAllowed(String nodeId){
		NodeRef ref=new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
		return new MCAlfrescoAPIClient().downloadAllowed(
				nodeId,
				getProperty(ref,CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY),
				getProperty(ref,CCConstants.CCM_PROP_EDITOR_TYPE)
		);
	}

	public static String renameNode(String oldName,int number){
		String[] split=oldName.split("\\.");
		int i=split.length-2;
		i=Math.max(0, i);
		split[i]+=" - "+number;
		return String.join(".",split);
	}

	/**
	 * Find all nodes that are having the properties set in the properties match
	 * @param parent The start folder
	 * @param types The node type to filter
	 * @param properties The properties that must match
	 * @return
	 */
	public static List<NodeRef> findNodeByPropertiesRecursive(NodeRef parent, List<String> types, Map<String, Object> properties){
		NodeService nodeService = NodeServiceFactory.getLocalService();
		List<NodeRef> list = nodeService.getChildrenRecursive(parent.getStoreRef(), parent.getId(), types, RecurseMode.Folders);
		return list.stream().filter((ref) ->
				properties.entrySet().stream().allMatch((e) ->
						Objects.equals(NodeServiceHelper.getProperty(ref, e.getKey()),e.getValue()))
		).collect(Collectors.toList());
	}

	public static GetPreviewResult getPreview(NodeRef ref) {
		return NodeServiceFactory.getLocalService().getPreview(ref.getStoreRef().getProtocol(),ref.getStoreRef().getIdentifier(),ref.getId(), null, null);
	}
	public static GetPreviewResult getPreview(NodeRef ref, HashMap<String, Object> nodeProps) {
		return NodeServiceFactory.getLocalService().getPreview(ref.getStoreRef().getProtocol(),ref.getStoreRef().getIdentifier(),ref.getId(), nodeProps, null);
	}
	public static GetPreviewResult getPreview(org.edu_sharing.service.model.NodeRef ref) {
		return NodeServiceFactory.getNodeService(ref.getRepositoryId()).getPreview(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId(), null, null);
	}
	public static NodeRef getCompanyHome() {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
		return repositoryHelper.getCompanyHome();
	}
	public static NodeRef getNodeInCompanyNode(String name){
		return getNodeByName(getCompanyHome(), CCConstants.CCM_TYPE_MAP, name);
	}
	public static NodeRef getNodeByName(NodeRef parent, String type, String name){
		return NodeServiceFactory.getLocalService().getChild(parent.getStoreRef(), parent.getId(), type, CCConstants.CM_NAME, name);
	}
	public static void blockImport(NodeRef node) throws Exception {
		setProperty(node, CCConstants.CCM_PROP_IO_IMPORT_BLOCKED, true);
		PermissionServiceFactory.getLocalService().setPermissions(node.getId(), new ArrayList<>(), false);
	}


	public static String getContainerId(String rootId, String pattern) {
		String result = null;
		try{
			MCAlfrescoAPIClient client = new MCAlfrescoAPIClient();
			// request node


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
		}
		return result;
	}
	/**
	 * Find the path to a container based on a pattern
	 * @param rootPath
	 * @param pattern Splitted by "/" - a date like pattern, e.g. yyyy/MM/dd/HH/mm/ss/SS
	 * @return the node id of the target folder
	 */
	public static String getContainerIdByPath(String rootPath, String pattern){
		try {
			String rootId = getContainerRootPath(rootPath);
			return getContainerId(rootId, pattern);
		}catch(Throwable t){
			logger.error(t.getMessage(), t);
		}
		return null;
	}

	public static String getContainerRootPath(String rootPath) throws Throwable {
		MCAlfrescoAPIClient client = new MCAlfrescoAPIClient();
		HashMap<String, HashMap<String, Object>> search = client.search("PATH:\"" + rootPath + "\"", CCConstants.CM_TYPE_FOLDER);
		String rootId = null;
		if (search.size() != 1) {
			if(search.size() > 1) throw new IllegalArgumentException("The path must reference a unique node.");


			String startAt = client.getCompanyHomeNodeId();
			String collectionPath = rootPath;
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
		return rootId;
	}

	/** fetches the properties from the original node
	 * if the node is a collection ref, the permissions will be handled accordingly
	 * @param nodeRef
	 * @return
	 */
	public static HashMap<String, Object> getPropertiesOriginal(NodeRef nodeRef) throws Throwable{
		if(NodeServiceHelper.hasAspect(nodeRef, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)){
			if(!PermissionServiceHelper.hasPermission(nodeRef, CCConstants.PERMISSION_READ)){
				throw new PermissionException(nodeRef.toString(), CCConstants.PERMISSION_READ);
			}
			// @TODO: Additional permissions check might later required for licensed content
			NodeRef original;
			Serializable originalProp = NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.CCM_PROP_IO_ORIGINAL);
			if(originalProp instanceof String){
				original = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, (String) originalProp);
			} else {
				original = (NodeRef) originalProp;
			}
			return AuthenticationUtil.runAsSystem(() -> {
				try {
					return NodeServiceHelper.getProperties(original);
				} catch (Throwable throwable) {
					throw new RuntimeException(throwable);
				}
			});
		}
		return NodeServiceHelper.getProperties(nodeRef);
	}

	public static void convertMutlivaluePropToGeneric(String[] arr, HashMap<String, Object> target, String property) {
		if(arr != null){
			if(arr.length==0)
				target.put(property,null);
			else if(arr.length > 1)
				target.put(property,new ArrayList<String>(Arrays.asList(arr)));
			else
				target.put(property, arr[0]);
		}
	}

	public static List<NodeRef> getParentPath(NodeRef target){
		List<NodeRef> path = new ArrayList<>();
		NodeRef currentNode = target;
		while(currentNode != null){
			String parent=null;
			try {
				parent = NodeServiceFactory.getLocalService().getPrimaryParent(currentNode.getId());
			}catch(Exception e){
			}
			if(parent != null){
				currentNode = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parent);
				path.add(currentNode);
			} else {
				currentNode = null;
			}
		}
		Collections.reverse(path);
		return path;
	}

	public static void copyProperty(NodeRef sourceNode, NodeRef targetNode, String property) {
		NodeServiceHelper.setProperty(targetNode, property, NodeServiceHelper.getProperty(sourceNode, property));
	}

	/**
	 * add virtual properties
	 * @return
	 */
	public static Map<String, Object> addVirtualProperties(String type, List<String> aspects, Map<String, Object> properties) {
		properties.put(CCConstants.VIRT_PROP_MEDIATYPE, I18nAngular.getTranslationAngular("common", "MEDIATYPE." + MimeTypesV2.getNodeType(type,
				properties,
				aspects))
		);
		return properties;
	}
}
