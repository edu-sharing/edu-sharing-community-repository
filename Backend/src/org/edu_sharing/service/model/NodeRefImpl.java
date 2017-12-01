package org.edu_sharing.service.model;

import java.util.HashMap;

import org.edu_sharing.repository.client.tools.CCConstants;

public class NodeRefImpl implements NodeRef {

	String repositoryId;
	
	String storeProtocol;
	
	String storeId;
	
	String nodeId;
	
	HashMap<String, Object> properties;
	
	public NodeRefImpl(){
		
	}
	public NodeRefImpl(String repositoryId, String storeProtocol, String storeId, String nodeId){
		this.repositoryId = repositoryId;
		this.storeId = storeId;
		this.storeProtocol = storeProtocol;
		this.nodeId = nodeId;
	}
	public NodeRefImpl(String repositoryId, String storeProtocol, String storeId, HashMap<String, Object> properties ){
		this.repositoryId = repositoryId;
		this.storeId = storeId;
		this.storeProtocol = storeProtocol;
		this.nodeId = (String)properties.get(CCConstants.SYS_PROP_NODE_UID);
		this.properties = properties;
	}

	@Override
	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	@Override
	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	@Override
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public HashMap<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(HashMap<String, Object> properties) {
		this.properties = properties;
	}
	
	@Override
	public String getStoreProtocol() {
		return this.storeProtocol;
	}
	
	public void setStoreProtocol(String storeProtocol) {
		this.storeProtocol = storeProtocol;
	}
	
}
