package org.edu_sharing.restservices.shared;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class NodePermissions {

	private ACL localPermissions = null;
	private List<ACE> inheritedPermissions = null;
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("localPermissions")
	public ACL getLocalPermissions() {
		return localPermissions;
	}
	public void setLocalPermissions(ACL localPermissions) {
		this.localPermissions = localPermissions;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("inheritedPermissions")
	public List<ACE> getInheritedPermissions() {
		return inheritedPermissions;
	}
	public void setInheritedPermissions(List<ACE> inheritedPermissions) {
		this.inheritedPermissions = inheritedPermissions;
	}

}
