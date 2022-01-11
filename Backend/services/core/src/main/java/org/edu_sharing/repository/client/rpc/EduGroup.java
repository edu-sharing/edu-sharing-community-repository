package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;

public class EduGroup implements Serializable {
	
	public EduGroup() {
	}
	
	String groupname;
	
	String groupDisplayName;
	
	String groupId;
	
	String folderName;
	
	String folderPath;
	
	String folderId;
	
	String scope;

	public String getGroupname() {
		return groupname;
	}

	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public String getFolderId() {
		return folderId;
	}

	public void setFolderId(String folderId) {
		this.folderId = folderId;
	}
	
	public String getFolderPath() {
		return folderPath;
	}
	
	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}
	
	public String getGroupDisplayName() {
		return groupDisplayName;
	}
	
	public void setGroupDisplayName(String groupDisplayName) {
		this.groupDisplayName = groupDisplayName;
	}
	
	public void setScope(String scope) {
		this.scope = scope;
	}
	
	public String getScope() {
		return scope;
	}
	
	@Override
	public boolean equals(Object obj) {

		EduGroup eduGroup = (EduGroup)obj;
		
		if(eduGroup == null) return false;
		
		/**
		 * ogroiup name is unique in alfresco
		 */
		if(this.groupname.equals(eduGroup.getGroupname())){
			return true;
		}
		
		
		return super.equals(obj);
	}
	
}
