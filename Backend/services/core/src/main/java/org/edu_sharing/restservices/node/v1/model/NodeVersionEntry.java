package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.restservices.shared.NodeVersion;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class NodeVersionEntry  {
  
  private NodeVersion version = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
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
