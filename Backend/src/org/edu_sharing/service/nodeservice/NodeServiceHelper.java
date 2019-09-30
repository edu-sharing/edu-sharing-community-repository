package org.edu_sharing.service.nodeservice;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.NameSpaceTool;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.service.search.model.SortDefinition;
import org.springframework.context.ApplicationContext;

public class NodeServiceHelper {
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
     * @param nodeRef
     * @return
     * @throws Throwable
     */
    public static HashMap<String, String[]> getPropertiesMultivalue(NodeRef nodeRef) throws Throwable {
        HashMap<String, Object> properties = getProperties(nodeRef);
        HashMap<String, String[]> propertiesMultivalue = new HashMap<>();
        properties.entrySet().forEach((e)->propertiesMultivalue.put(e.getKey(),ValueTool.getMultivalue(e.getValue().toString())));
        return propertiesMultivalue;
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


	public static GetPreviewResult getPreview(NodeRef ref) {
		return NodeServiceFactory.getLocalService().getPreview(ref.getStoreRef().getProtocol(),ref.getStoreRef().getIdentifier(),ref.getId(),null);
	}
	public static GetPreviewResult getPreview(org.edu_sharing.service.model.NodeRef ref) {
		return NodeServiceFactory.getNodeService(ref.getRepositoryId()).getPreview(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId(),null);
	}
}
