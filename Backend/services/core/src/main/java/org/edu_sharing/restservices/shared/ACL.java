package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ACL {

	private boolean inherited = false;	
	private List<ACE> permissions = null;
	
	public ACL(){}
	public ACL(org.edu_sharing.repository.client.rpc.ACL acl) {
		inherited=acl.isInherited();
		permissions=new ArrayList<>();
		if(acl.getAces()==null){
			return;
		}
		
		for(org.edu_sharing.repository.client.rpc.ACE ace : acl.getAces()){
			boolean added=false;
			for(ACE check : permissions){
				if(check.getAuthority().getAuthorityName().equals(ace.getAuthority())){
					check.getPermissions().add(ace.getPermission());
					added=true;
					break;
				}
			}
			if(added)
				continue;
			
			permissions.add(new ACE(ace));
		}
	}
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("inherited")
	public boolean isInherited() {
		return inherited;
	}
	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}
		
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("permissions")
	public List<ACE> getPermissions() {
		return permissions;
	}
	public void setPermissions(List<ACE> permissions) {
		this.permissions = permissions;
	}

}
