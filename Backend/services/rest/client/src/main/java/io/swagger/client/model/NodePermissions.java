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
import io.swagger.client.model.ACE;
import io.swagger.client.model.ACL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * NodePermissions
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class NodePermissions {
  @SerializedName("localPermissions")
  private ACL localPermissions = null;

  @SerializedName("inheritedPermissions")
  private List<ACE> inheritedPermissions = new ArrayList<ACE>();

  public NodePermissions localPermissions(ACL localPermissions) {
    this.localPermissions = localPermissions;
    return this;
  }

   /**
   * Get localPermissions
   * @return localPermissions
  **/
  @ApiModelProperty(required = true, value = "")
  public ACL getLocalPermissions() {
    return localPermissions;
  }

  public void setLocalPermissions(ACL localPermissions) {
    this.localPermissions = localPermissions;
  }

  public NodePermissions inheritedPermissions(List<ACE> inheritedPermissions) {
    this.inheritedPermissions = inheritedPermissions;
    return this;
  }

  public NodePermissions addInheritedPermissionsItem(ACE inheritedPermissionsItem) {
    this.inheritedPermissions.add(inheritedPermissionsItem);
    return this;
  }

   /**
   * Get inheritedPermissions
   * @return inheritedPermissions
  **/
  @ApiModelProperty(required = true, value = "")
  public List<ACE> getInheritedPermissions() {
    return inheritedPermissions;
  }

  public void setInheritedPermissions(List<ACE> inheritedPermissions) {
    this.inheritedPermissions = inheritedPermissions;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NodePermissions nodePermissions = (NodePermissions) o;
    return Objects.equals(this.localPermissions, nodePermissions.localPermissions) &&
        Objects.equals(this.inheritedPermissions, nodePermissions.inheritedPermissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localPermissions, inheritedPermissions);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodePermissions {\n");
    
    sb.append("    localPermissions: ").append(toIndentedString(localPermissions)).append("\n");
    sb.append("    inheritedPermissions: ").append(toIndentedString(inheritedPermissions)).append("\n");
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

