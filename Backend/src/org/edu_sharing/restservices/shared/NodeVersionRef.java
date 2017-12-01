package org.edu_sharing.restservices.shared;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class NodeVersionRef  {
  
  private NodeRef node = null;
  private int major = 0;
  private int minor = 0;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("node")
  public NodeRef getNode() {
    return node;
  }
  public void setNode(NodeRef node) {
    this.node = node;
  }

  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("major")
  public int getMajor() {
    return major;
  }
  public void setMajor(int major) {
    this.major = major;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("minor")
  public int getMinor() {
    return minor;
  }
  public void setMinor(int minor) {
    this.minor = minor;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeVersionRef {\n");
    
    sb.append("  node: ").append(node).append("\n");
    sb.append("  major: ").append(major).append("\n");
    sb.append("  minor: ").append(minor).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
