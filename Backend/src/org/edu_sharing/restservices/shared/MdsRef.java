package org.edu_sharing.restservices.shared;


import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class MdsRef  {
  
  private String repo = null;
  private String id = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("repo")
  public String getRepo() {
    return repo;
  }
  public void setRepo(String repo) {
    this.repo = repo;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

}
