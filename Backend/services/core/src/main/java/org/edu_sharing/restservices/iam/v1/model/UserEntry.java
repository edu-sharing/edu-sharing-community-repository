package org.edu_sharing.restservices.iam.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.edu_sharing.restservices.shared.User;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class UserEntry  {
  
  private User person = null;
  private Boolean editProfile = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("person")
  public User getPerson() {
    return person;
  }
  public void setPerson(User person) {
    this.person = person;
  }
  
  public void setEditProfile(Boolean editProfile) {
	this.editProfile = editProfile;
  }
  
  public Boolean getEditProfile() {
	return editProfile;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserEntry {\n");
    
    sb.append("  person: ").append(person).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
