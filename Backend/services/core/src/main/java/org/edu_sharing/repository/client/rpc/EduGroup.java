package org.edu_sharing.repository.client.rpc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class EduGroup implements Serializable {
	
	public EduGroup() {
	}

    String groupname;
    String groupDisplayName;
    String groupId;
    String folderId;
    String scope;

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
