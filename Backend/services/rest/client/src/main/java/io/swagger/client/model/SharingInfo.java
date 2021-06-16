/*
 * edu-sharing Repository REST API
 * The public restful API of the edu-sharing repository.
 *
 * OpenAPI spec version: 1.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.Node;
import io.swagger.client.model.Person;
import java.io.IOException;

/**
 * SharingInfo
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class SharingInfo {
  @SerializedName("passwordMatches")
  private Boolean passwordMatches = false;

  @SerializedName("password")
  private Boolean password = false;

  @SerializedName("expired")
  private Boolean expired = false;

  @SerializedName("invitedBy")
  private Person invitedBy = null;

  @SerializedName("node")
  private Node node = null;

  public SharingInfo passwordMatches(Boolean passwordMatches) {
    this.passwordMatches = passwordMatches;
    return this;
  }

   /**
   * Get passwordMatches
   * @return passwordMatches
  **/
  @ApiModelProperty(value = "")
  public Boolean isPasswordMatches() {
    return passwordMatches;
  }

  public void setPasswordMatches(Boolean passwordMatches) {
    this.passwordMatches = passwordMatches;
  }

  public SharingInfo password(Boolean password) {
    this.password = password;
    return this;
  }

   /**
   * Get password
   * @return password
  **/
  @ApiModelProperty(value = "")
  public Boolean isPassword() {
    return password;
  }

  public void setPassword(Boolean password) {
    this.password = password;
  }

  public SharingInfo expired(Boolean expired) {
    this.expired = expired;
    return this;
  }

   /**
   * Get expired
   * @return expired
  **/
  @ApiModelProperty(value = "")
  public Boolean isExpired() {
    return expired;
  }

  public void setExpired(Boolean expired) {
    this.expired = expired;
  }

  public SharingInfo invitedBy(Person invitedBy) {
    this.invitedBy = invitedBy;
    return this;
  }

   /**
   * Get invitedBy
   * @return invitedBy
  **/
  @ApiModelProperty(value = "")
  public Person getInvitedBy() {
    return invitedBy;
  }

  public void setInvitedBy(Person invitedBy) {
    this.invitedBy = invitedBy;
  }

  public SharingInfo node(Node node) {
    this.node = node;
    return this;
  }

   /**
   * Get node
   * @return node
  **/
  @ApiModelProperty(value = "")
  public Node getNode() {
    return node;
  }

  public void setNode(Node node) {
    this.node = node;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SharingInfo sharingInfo = (SharingInfo) o;
    return Objects.equals(this.passwordMatches, sharingInfo.passwordMatches) &&
        Objects.equals(this.password, sharingInfo.password) &&
        Objects.equals(this.expired, sharingInfo.expired) &&
        Objects.equals(this.invitedBy, sharingInfo.invitedBy) &&
        Objects.equals(this.node, sharingInfo.node);
  }

  @Override
  public int hashCode() {
    return Objects.hash(passwordMatches, password, expired, invitedBy, node);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SharingInfo {\n");
    
    sb.append("    passwordMatches: ").append(toIndentedString(passwordMatches)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    expired: ").append(toIndentedString(expired)).append("\n");
    sb.append("    invitedBy: ").append(toIndentedString(invitedBy)).append("\n");
    sb.append("    node: ").append(toIndentedString(node)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

