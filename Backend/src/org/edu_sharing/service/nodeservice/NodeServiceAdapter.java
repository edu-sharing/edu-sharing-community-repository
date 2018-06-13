package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;

public class NodeServiceAdapter implements NodeService {
	
	protected String appId;

	public NodeServiceAdapter(String appId) {
		this.appId=appId;
	}

	@Override
	public void updateNode(String nodeId, HashMap<String, String[]> props) throws Throwable {
	}

	@Override
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props) throws Throwable {
		return null;
	}

	@Override
	public String createNodeBasic(String parentID, String nodeTypeString, HashMap<String, Object> _props) {
		return null;
	}

	@Override
	public String findNodeByName(String parentId, String name) {
		return null;
	}

	@Override
	public NodeRef copyNode(String sourceId, String nodeId, boolean withChildren) throws Throwable {
		return null;
	}

	@Override
	public String getCompanyHome() {
		return null;
	}

	@Override
	public HashMap<String, String[]> getNameProperty(String name) {
		return null;
	}

	@Override
	public HashMap<String, Object> getChild(StoreRef store, String parentId, String type, String property,
			String value) {
		return null;
	}

	@Override
	public void setOwner(String nodeId, String username) {
	}

	@Override
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission)
			throws Exception {
	}

	@Override
	public String getOrCreateUserInbox() {
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRef(String parentID) {
		return null;
	}

	@Override
	public void createVersion(String nodeId, HashMap _properties) throws Exception {
	}

	@Override
	public void writeContent(StoreRef store, String nodeID, InputStream content, String mimetype, String _encoding,
			String property) throws Exception {
	}

	@Override
	public void removeNode(String nodeID, String fromID) {	
	}

	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return new HashMap<String,Object>();
	}
	
	@Override
	public void addAspect(String nodeId, String aspect) {
	}
	
	@Override
	public String[] getAspects(String storeProtocol, String storeId, String nodeId) {
		return null;
	}
	
	@Override
	public void moveNode(String newParentId, String childAssocType, String nodeId) {
	}
	
	@Override
	public void revertVersion(String nodeId, String verLbl) throws Exception {
	}
	
	@Override
	public HashMap<String, HashMap<String, Object>> getVersionHistory(String nodeId) throws Exception {
		return null;
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
	
	private HashMap<String,String[]> convertProperties(HashMap<String,Object> propsIn){
		ValueTool vt = new ValueTool();
		HashMap<String,String[]> properties = new HashMap<String,String[]>();
		for (Entry<String, Object> entry : propsIn.entrySet()) {
			List<String> values = getPropertyValues(vt, entry.getValue());
			properties.put(entry.getKey(), values.toArray(new String[values.size()]));
		}
		return properties;
	}
	
	// to be overwritten if necessary
	public InputStream getContent(String nodeId) throws Throwable{
		throw new Exception("getContent not implemented for this repository");
	}
	
	/**
	 * Import the node from a foreign repository to the local one
	 * @param localParent
	 * @return
	 * @throws Exception 
	 */
	@Override
	public String importNode(String nodeId,String localParent) throws Throwable {
		HashMap<String, String[]> props = convertProperties(getProperties(null, null, nodeId));
		String mimetype=null;
		if(props.containsKey(CCConstants.LOM_PROP_TECHNICAL_FORMAT))
			mimetype=props.get(CCConstants.LOM_PROP_TECHNICAL_FORMAT)[0];
		InputStream content=getContent(nodeId);
		NodeService service=NodeServiceFactory.getLocalService();
		props.remove(CCConstants.SYS_PROP_NODE_UID);
		props.remove(CCConstants.CM_PROP_C_CREATED);
		props.remove(CCConstants.CM_PROP_C_MODIFIED);
		String localNode=service.createNode(localParent, CCConstants.CCM_TYPE_IO,props);
		service.writeContent(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"), localNode, content,mimetype, null, CCConstants.CM_PROP_CONTENT);
		return localNode;
	}
	
	@Override
	public User getOwner(String storeId, String storeProtocol, String nodeId) {
		return null;
	}

	@Override
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props, String childAssociation)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public InputStream getContent(String storeProtocol, String storeId, String nodeId, String contentProp)
			throws Throwable {
		return null;
	}
	
	@Override
	public void removeNode(String nodeId, String parentId, boolean recycle) {
	}
	
	@Override
	public void removeNode(String potocol, String store, String nodeId) {	
	}

	@Override
	public String getOrCreateUserSavedSearch() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAspect(String nodeId, String aspect) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNodeNative(String nodeId, HashMap<String, Object> _props) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeProperty(String storeProtocol, String storeId, String nodeId, String property) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String createNodeBasic(StoreRef store, String parentID, String nodeTypeString, String childAssociation,
			HashMap<String, Object> _props) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRef(String parentID, String childType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType(String nodeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(String protocol, String store, String nodeId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getProperty(String storeProtocol, String storeId, String nodeId, String property) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTemplateNode(String nodeId,boolean create) throws Throwable {
		return null;
	}

	@Override
	public void setTemplateProperties(String nodeId, HashMap<String, String[]> stringHashMap) throws Throwable {

	}

	@Override
	public void setTemplateStatus(String nodeId, Boolean enable) throws Throwable {

	}

}
