package org.edu_sharing.restservices.shared;

import org.edu_sharing.repository.client.rpc.Group;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class GroupProfile  {
  
  private String displayName = null;
  private String groupType = null;
  private String groupEmail = null;
  private String scopeType = null;

  public GroupProfile(){}
  public GroupProfile(GroupProfile GroupProfile) {
    displayName=GroupProfile.getDisplayName();
    groupType=GroupProfile.getGroupType();
    groupEmail=GroupProfile.getGroupEmail();
    scopeType=GroupProfile.getScopeType();
  }
  public GroupProfile(Group group) {
    displayName=group.getAuthorityDisplayName();
  }
/**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("displayName")
  public String getDisplayName() {
    return displayName;
  }
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @JsonProperty("groupType")
  public String getGroupType() {
	return groupType;
  }
  public void setGroupType(String groupType) {
	this.groupType = groupType;
  }

  @JsonProperty("scopeType")
  public void setScopeType(String scopeType) {
	this.scopeType = scopeType;
}

  public String getScopeType() {
	return scopeType;
}

  @JsonProperty
  public String getGroupEmail() {
    return groupEmail;
  }

  public void setGroupEmail(String groupEmail) {
    this.groupEmail = groupEmail;
  }
}
