package org.edu_sharing.restservices.node.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import org.edu_sharing.restservices.shared.NodeVersion;
import org.edu_sharing.restservices.shared.NodeVersionRef;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class NodeVersionEntries  {
  
  private List<NodeVersion> versions = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("versions")
  public List<NodeVersion> getVersions() {
    return versions;
  }
  public void setVersions(List<NodeVersion> versions) {
    this.versions = versions;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeVersionEntries{\n");
    
    sb.append("  versions: ").append(versions).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
