package org.edu_sharing.restservices.node.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import org.edu_sharing.restservices.shared.NodeVersionRef;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class NodeVersionRefEntries  {
  
  private List<NodeVersionRef> versions = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("versions")
  public List<NodeVersionRef> getVersions() {
    return versions;
  }
  public void setVersions(List<NodeVersionRef> versions) {
    this.versions = versions;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeVersionRefEntries{\n");
    
    sb.append("  versions: ").append(versions).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
