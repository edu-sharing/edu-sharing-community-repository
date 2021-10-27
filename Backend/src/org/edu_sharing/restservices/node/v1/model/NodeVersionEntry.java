package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.restservices.shared.NodeVersion;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class NodeVersionEntry  {
  
  private NodeVersion version = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("version")
  public NodeVersion getVersion() {
    return version;
  }
  public void setVersion(NodeVersion version) {
    this.version = version;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeVersionEntry{\n");
    
    sb.append("  version: ").append(version).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
