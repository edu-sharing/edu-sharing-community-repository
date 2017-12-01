package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class Repo  {
  
  private String id = null;
  private Boolean isHomeRepo = null;
  private String title = null;
  private Preview preview = null;
  private String repositoryType = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("isHomeRepo")
  public Boolean isHomeRepo() {
    return isHomeRepo;
  }
  public void setHomeRepo(Boolean isHomeRepo) {
    this.isHomeRepo = isHomeRepo;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }


  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("preview")
  public Preview getPreview() {
    return preview;
  }
  public void setPreview(Preview preview) {
    this.preview = preview;
  }

  
  @JsonProperty
  public String getRepositoryType() {
	  return repositoryType;
  }
  public void setRepositoryType(String repositoryType) {
		this.repositoryType = repositoryType;
  }
@Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Repo {\n");
    
    sb.append("  id: ").append(id).append("\n");
    sb.append("  isHomeRepo: ").append(isHomeRepo).append("\n");
    sb.append("  title: ").append(title).append("\n");
    sb.append("  preview: ").append(preview).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
