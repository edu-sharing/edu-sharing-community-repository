package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.restservices.shared.NodePermissions;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class NodePermissionEntry  {
  
  private NodePermissions permissions = null;
  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("permissions")
  public NodePermissions getPermissions() {
    return permissions;
  }
  public void setPermissions(NodePermissions permissions) {
    this.permissions = permissions;
  }

}
