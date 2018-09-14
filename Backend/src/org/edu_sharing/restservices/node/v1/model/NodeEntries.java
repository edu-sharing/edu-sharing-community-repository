package org.edu_sharing.restservices.node.v1.model;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;

import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


public class NodeEntries  {
  
  private List<Node> nodes = new ArrayList<Node>();
  private Pagination pagination = null;

  public NodeEntries(){
  }
  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("nodes")
  public List<Node> getNodes() {
    return nodes;
  }
  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("pagination")
  public Pagination getPagination() {
    return pagination;
  }
  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeEntries {\n");
    
    sb.append("  nodes: ").append(nodes).append("\n");
    sb.append("  pagination: ").append(pagination).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
