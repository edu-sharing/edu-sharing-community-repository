package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.restservices.shared.NodePermissions;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;


@Schema(description = "")
public class NodePermissionEntry  {
  
  private NodePermissions permissions = null;
  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("permissions")
  public NodePermissions getPermissions() {
    return permissions;
  }
  public void setPermissions(NodePermissions permissions) {
    this.permissions = permissions;
  }

}
