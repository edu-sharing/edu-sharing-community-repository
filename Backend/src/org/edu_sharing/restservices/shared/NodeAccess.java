package org.edu_sharing.restservices.shared;


import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class NodeAccess  {
  
  private String permission = null;
  private Boolean hasRight = null;

  /**
   **/
  @Schema(description = "")
  @JsonProperty("permission")
  public String getPermission() {
    return permission;
  }
  public void setPermission(String permission) {
    this.permission = permission;
  }

  
  /**
   **/
  @Schema(description = "")
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
