package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.restservices.shared.Node;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class NodeEntry  {
  
  private Node node = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("node")
  public Node getNode() {
    return node;
  }
  public void setNode(Node node) {
    this.node = node;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeEntry {\n");
    
    sb.append("  node: ").append(node).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
