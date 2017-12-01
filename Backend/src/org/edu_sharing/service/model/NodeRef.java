package org.edu_sharing.service.model;

import java.util.HashMap;

public interface NodeRef {
	
	public String getRepositoryId();
	
	public void setRepositoryId(String repositoryId);
	
	public String getStoreProtocol();
	
	public void setStoreProtocol(String storeProtocol);
	
	public String getStoreId();
	
	public void setStoreId(String storeId);
	
	public String getNodeId();
	
	public void setNodeId(String nodeId);
	
	public HashMap<String,Object> getProperties();
	
	public void setProperties(HashMap<String,Object> properties);

}
