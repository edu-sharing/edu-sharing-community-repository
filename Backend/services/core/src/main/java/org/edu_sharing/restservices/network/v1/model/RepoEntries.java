package org.edu_sharing.restservices.network.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import org.edu_sharing.restservices.shared.Repo;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class RepoEntries  {
  
  private List<Repo> list = null;

  
  /**
   **/
  @Schema(required = true, description = "")
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
