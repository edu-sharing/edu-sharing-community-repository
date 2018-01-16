package org.edu_sharing.restservices.shared;

import org.edu_sharing.repository.client.rpc.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class UserProfile  {
  
  private String firstName = null;
  private String lastName = null;
  private String email = null;
  private String avatar = null;

  public UserProfile(){
	  
  }
  public UserProfile(String firstName,String lastName,String email){
	  this.firstName=firstName;
	  this.lastName=lastName;
	  this.email=email;
  }
  public UserProfile(User user) {
	this(user.getGivenName(),user.getSurname(),user.getEmail());
  }

/**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("firstName")
  public String getFirstName() {
    return firstName;
  }
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("lastName")
  public String getLastName() {
    return lastName;
  }
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }
  
  @JsonProperty("avatar")
  public String getAvatar() {
	return avatar;
}
public void setAvatar(String avatar) {
	this.avatar = avatar;
}
@Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserProfile {\n");
    
    sb.append("  firstName: ").append(firstName).append("\n");
    sb.append("  lastName: ").append(lastName).append("\n");
    sb.append("  email: ").append(email).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
