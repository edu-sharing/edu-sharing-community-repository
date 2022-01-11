package org.edu_sharing.restservices.login.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class ScopeAccess  {
  
  private boolean hasAccess;

  
  public ScopeAccess(boolean hasAccess) {
	this.hasAccess = hasAccess;
  }
/**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("hasAccess")
  public boolean hasAccess() {
    return hasAccess;
  }
  public void setHasAccess(boolean hasAccess) {
    this.hasAccess = hasAccess;
  }
  
  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScopeAccess {\n");
    
    sb.append("  hasAccess: ").append(hasAccess).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
