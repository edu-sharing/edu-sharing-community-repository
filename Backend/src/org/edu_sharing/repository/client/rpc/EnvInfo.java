package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;
import java.util.HashMap;

public class EnvInfo implements Serializable{

	String rootNode;
	
	HashMap<String,Object> rootNodeProps;
	
	String defaultUploadFolder;
	
	HashMap<String,Object> defaultUploadFolderProps;
	
	public EnvInfo() {
	}
	
	public void setDefaultUploadFolder(String defaultUploadFolder) {
		this.defaultUploadFolder = defaultUploadFolder;
	}
	
	public void setRootNode(String rootNode) {
		this.rootNode = rootNode;
	}
	
	public String getDefaultUploadFolder() {
		return defaultUploadFolder;
	}
	
	public String getRootNode() {
		return rootNode;
	}
	
	public HashMap<String, Object> getDefaultUploadFolderProps() {
		return defaultUploadFolderProps;
	}
	
	public HashMap<String, Object> getRootNodeProps() {
		return rootNodeProps;
	}
	
	public void setDefaultUploadFolderProps(HashMap<String, Object> defaultUploadFolderProps) {
		this.defaultUploadFolderProps = defaultUploadFolderProps;
	}
	
	public void setRootNodeProps(HashMap<String, Object> rootNodeProps) {
		this.rootNodeProps = rootNodeProps;
	}
}
