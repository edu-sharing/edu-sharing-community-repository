package org.edu_sharing.restservices.shared;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class NodeAccess  {
  
  private String permission = null;
  private Boolean hasRight = null;

  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("permission")
  public String getPermission() {
    return permission;
  }
  public void setPermission(String permission) {
    this.permission = permission;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("hasRight")
  public Boolean hasRight() {
    return hasRight;
  }
  public void setRight(Boolean hasRight) {
    this.hasRight = hasRight;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeAccess {\n");
    
    sb.append("  permission: ").append(permission).append("\n");
    sb.append("  hasRight: ").append(hasRight).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
