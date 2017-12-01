package org.edu_sharing.restservices.shared;

import org.edu_sharing.repository.client.rpc.Group;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class GroupProfile  {
  
  private String displayName = null;
  private String groupType = null;

  public GroupProfile(){}
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
@Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class GroupProfile {\n");
    
    sb.append("  displayName: ").append(displayName).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
