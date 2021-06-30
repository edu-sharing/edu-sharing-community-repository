package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.service.permission.HandleMode;
import org.edu_sharing.service.search.model.SortDefinition;

public interface NodeService {

	
	public void updateNode(String nodeId, HashMap<String, String[]> props) throws Throwable;

	public void createAssoc(String parentId,String childId,String assocName);

	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props)throws Throwable;
	
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props, String childAssociation) throws Throwable;
	
	public String createNodeBasic(String parentID, String nodeTypeString, HashMap<String, ?> _props);

	public String createNodeBasic(StoreRef store, String parentID, String nodeTypeString, String childAssociation,
			HashMap<String, ?> _props);

	public String findNodeByName(String parentId, String name );

	public NodeRef copyNode(String sourceId, String nodeId, boolean withChildren) throws Throwable;

	public String getCompanyHome();

	public HashMap<String, String[]> getNameProperty(String name);

    List<NodeRef> getChildrenRecursive(StoreRef store, String nodeId, List<String> types,RecurseMode recurseMode);

    public NodeRef getChild(StoreRef store, String parentId, String type, String property, Serializable value);

    String getType(String storeProtocol, String storeId, String nodeId);

    default String getType(String nodeId){
    	return getType(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId);
	}

    public void setOwner(String nodeId, String username);
	
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission) throws Exception;

	public String getOrCreateUserInbox();
	
	public String getOrCreateUserSavedSearch();

	public String getPrimaryParent(String nodeId);

	default List<ChildAssociationRef> getChildrenChildAssociationRef(String parentID){
		return getChildrenChildAssociationRefAssoc(parentID,null, null, new SortDefinition());
	}

    <T>List<T> sortNodeRefList(List<T> list, List<String> filter, SortDefinition sortDefinition);

    public List<ChildAssociationRef> getChildrenChildAssociationRefType(String parentID, String childType);

    public List<ChildAssociationRef> getChildrenChildAssociationRefAssoc(String parentID, String asoocName, List<String> filter, SortDefinition sortDefinition);

	public void createVersion(String nodeId) throws Exception;

	public void deleteVersionHistory(String nodeId) throws Exception;

	public void writeContent(final StoreRef store, final String nodeID, final InputStream content, final String mimetype, String _encoding,
			final String property) throws Exception;
	
	public void removeNode(String nodeID, String fromID);

	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable;

	/**
	 * this method is called when a local object has ccm:remoterepositry aspect, and all local properties will get
	 * updated on the fly with the properties provided by this method
	 */
	public HashMap<String, Object> getPropertiesDynamic(String storeProtocol, String storeId, String nodeId) throws Throwable;

	/**
	 * this method is called when a local object has ccm:remoterepositry aspect and the node should be stored localy
	 * You can define which properties should be copied locally and which should be fetched dynamically by skipping them here
	 */
	public HashMap<String, Object> getPropertiesPersisting(String storeProtocol, String storeId, String nodeId) throws Throwable;

	public default boolean hasAspect(String storeProtocol, String storeId, String nodeId, String aspect){
		return Arrays.asList(getAspects(storeProtocol,storeId,nodeId)).contains(aspect);
	}
	public String[] getAspects(String storeProtocol, String storeId, String nodeId);

	InputStream getContent(String storeProtocol, String storeId, String nodeId, String version, String contentProp) throws Throwable;

	String getContentHash(String storeProtocol, String storeId, String nodeId, String version, String contentProp);

	public void addAspect(String nodeId, String aspect);
	
	public void moveNode(String newParentId, String childAssocType, String nodeId);
	
	public void revertVersion(String nodeId, String verLbl) throws Exception;
	
	public HashMap<String, HashMap<String,Object>> getVersionHistory(String nodeId) throws Throwable;
	/**
	 * Import the node from a foreign repository to the local one, and return the local node Ref
	 * @param nodeId
	 * @param localParent
	 * @return
	 * @throws Exception 
	 * @throws Throwable 
	 */
	public String importNode(String nodeId,String localParent) throws Throwable;
	
	public User getOwner(String storeId, String storeProtocol, String nodeId);

	public void removeNode(String nodeId, String parentId, boolean recycle);
	
	public void removeNode(String potocol, String store, String nodeId);

	public void removeNodeForce(String storeProtocol, String storeId, String nodeId, boolean recycle);

	public void removeAspect(String nodeId, String aspect);

    public void updateNodeNative(String nodeId, HashMap<String, ?> _props);

	public void removeProperty(String storeProtocol, String storeId, String nodeId, String property);

	public boolean exists(String protocol, String store, String nodeId);

	default String getProperty(String storeProtocol, String storeId, String nodeId, String property) {
	    try {
            return (String)getProperties(storeProtocol, storeId, nodeId).get(property);
        }catch(Throwable t){
			Logger.getLogger(NodeService.class).warn(t);
	        return null;
        }
    };


	String getTemplateNode(String nodeId,boolean createIfNotExists) throws Throwable;

	/**
	 * Sets the properties for this node's template (inherit metadata to child nodes)
	 * Should only be supported for folder types
	 * @param nodeId
	 * @param stringHashMap
	 */
	void setTemplateProperties(String nodeId, HashMap<String,String[]> stringHashMap) throws Throwable;

	/**
	 * Set if the inherition of properties is enabled for this folder
	 * @param nodeId
	 * @param enable
	 */
	void setTemplateStatus(String nodeId, Boolean enable) throws Throwable;

    String getPrimaryParent(String protocol, String store, String nodeId);

	String getContentMimetype(String protocol, String storeId, String nodeId);

	List<AssociationRef> getNodesByAssoc(String nodeId, AssocInfo assoc);

	void setProperty(String protocol, String storeId, String nodeId, String property, Serializable value);

    GetPreviewResult getPreview(String storeProtocol, String storeIdentifier, String nodeId, HashMap<String, Object> nodeProps, String version);

    Collection<NodeRef> getFrontpageNodes() throws Throwable;

    Serializable getPropertyNative(String storeProtocol, String storeId, String nodeId, String property) throws Throwable;

	/**
	 * create a published copy of the node
	 * if handle mode is set, a handle should also be generated
	 */
	String publishCopy(String nodeId, HandleMode handleMode) throws Throwable;

	/**
	 * Get all published copies of this node
	 * @return
	 */
	List<String> getPublishedCopies(String nodeId);
}
