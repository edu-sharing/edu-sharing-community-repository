package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class Repo  {
  
  private String id = null;
  private Boolean isHomeRepo = null;
  private String title = null;
  private String repositoryType = null;
  private String icon = null;
  private String logo = null;
  private boolean renderingSupported;


  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  
  @JsonProperty("icon")
  public String getIcon() {
	return icon;
  }
  public void setIcon(String icon) {
		this.icon = icon;
	}
  @JsonProperty("logo")
  public String getLogo() {
	return logo;
  }
  public void setLogo(String logo) {
		this.logo = logo;
	}

  @JsonProperty("isHomeRepo")
  public Boolean isHomeRepo() {
    return isHomeRepo;
  }
  public void setHomeRepo(Boolean isHomeRepo) {
    this.isHomeRepo = isHomeRepo;
  }

  @JsonProperty("title")
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }

  @JsonProperty
  public String getRepositoryType() {
	  return repositoryType;
  }
  public void setRepositoryType(String repositoryType) {
		this.repositoryType = repositoryType;
  }

  @JsonProperty
  public boolean getRenderingSupported() {
    return renderingSupported;
  }
  public void setRenderingSupported(boolean renderingSupported) {
    this.renderingSupported = renderingSupported;
  }
}
