package org.edu_sharing.restservices.shared;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(description = "")
public class NodePermissions {

	private ACL localPermissions = null;
	private List<ACE> inheritedPermissions = null;
	
	@Schema(required = true, description = "")
	@JsonProperty("localPermissions")
	public ACL getLocalPermissions() {
		return localPermissions;
	}
	public void setLocalPermissions(ACL localPermissions) {
		this.localPermissions = localPermissions;
	}
	
	@Schema(required = true, description = "")
	@JsonProperty("inheritedPermissions")
	public List<ACE> getInheritedPermissions() {
		return inheritedPermissions;
	}
	public void setInheritedPermissions(List<ACE> inheritedPermissions) {
		this.inheritedPermissions = inheritedPermissions;
	}

}
