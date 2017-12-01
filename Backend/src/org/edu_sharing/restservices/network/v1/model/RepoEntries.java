package org.edu_sharing.restservices.network.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import org.edu_sharing.restservices.shared.Repo;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class RepoEntries  {
  
  private List<Repo> list = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("repositories")
  public List<Repo> getList() {
    return list;
  }
  public void setList(List<Repo> list) {
    this.list = list;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class RepoEntries {\n");
    
    sb.append("  list: ").append(list).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
