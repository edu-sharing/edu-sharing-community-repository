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
import java.io.IOException;

/**
 * MdsProperty
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class MdsProperty {
  @SerializedName("name")
  private String name = null;

  @SerializedName("type")
  private String type = null;

  @SerializedName("defaultValue")
  private String defaultValue = null;

  @SerializedName("processtype")
  private String processtype = null;

  @SerializedName("keyContenturl")
  private String keyContenturl = null;

  @SerializedName("concatewithtype")
  private Boolean concatewithtype = false;

  @SerializedName("multiple")
  private Boolean multiple = false;

  @SerializedName("copyFrom")
  private String copyFrom = null;

  public MdsProperty name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(required = true, value = "")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public MdsProperty type(String type) {
    this.type = type;
    return this;
  }

   /**
   * Get type
   * @return type
  **/
  @ApiModelProperty(required = true, value = "")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public MdsProperty defaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

   /**
   * Get defaultValue
   * @return defaultValue
  **/
  @ApiModelProperty(required = true, value = "")
  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public MdsProperty processtype(String processtype) {
    this.processtype = processtype;
    return this;
  }

   /**
   * Get processtype
   * @return processtype
  **/
  @ApiModelProperty(required = true, value = "")
  public String getProcesstype() {
    return processtype;
  }

  public void setProcesstype(String processtype) {
    this.processtype = processtype;
  }

  public MdsProperty keyContenturl(String keyContenturl) {
    this.keyContenturl = keyContenturl;
    return this;
  }

   /**
   * Get keyContenturl
   * @return keyContenturl
  **/
  @ApiModelProperty(required = true, value = "")
  public String getKeyContenturl() {
    return keyContenturl;
  }

  public void setKeyContenturl(String keyContenturl) {
    this.keyContenturl = keyContenturl;
  }

  public MdsProperty concatewithtype(Boolean concatewithtype) {
    this.concatewithtype = concatewithtype;
    return this;
  }

   /**
   * Get concatewithtype
   * @return concatewithtype
  **/
  @ApiModelProperty(required = true, value = "")
  public Boolean isConcatewithtype() {
    return concatewithtype;
  }

  public void setConcatewithtype(Boolean concatewithtype) {
    this.concatewithtype = concatewithtype;
  }

  public MdsProperty multiple(Boolean multiple) {
    this.multiple = multiple;
    return this;
  }

   /**
   * Get multiple
   * @return multiple
  **/
  @ApiModelProperty(required = true, value = "")
  public Boolean isMultiple() {
    return multiple;
  }

  public void setMultiple(Boolean multiple) {
    this.multiple = multiple;
  }

  public MdsProperty copyFrom(String copyFrom) {
    this.copyFrom = copyFrom;
    return this;
  }

   /**
   * Get copyFrom
   * @return copyFrom
  **/
  @ApiModelProperty(required = true, value = "")
  public String getCopyFrom() {
    return copyFrom;
  }

  public void setCopyFrom(String copyFrom) {
    this.copyFrom = copyFrom;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MdsProperty mdsProperty = (MdsProperty) o;
    return Objects.equals(this.name, mdsProperty.name) &&
        Objects.equals(this.type, mdsProperty.type) &&
        Objects.equals(this.defaultValue, mdsProperty.defaultValue) &&
        Objects.equals(this.processtype, mdsProperty.processtype) &&
        Objects.equals(this.keyContenturl, mdsProperty.keyContenturl) &&
        Objects.equals(this.concatewithtype, mdsProperty.concatewithtype) &&
        Objects.equals(this.multiple, mdsProperty.multiple) &&
        Objects.equals(this.copyFrom, mdsProperty.copyFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, defaultValue, processtype, keyContenturl, concatewithtype, multiple, copyFrom);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MdsProperty {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    processtype: ").append(toIndentedString(processtype)).append("\n");
    sb.append("    keyContenturl: ").append(toIndentedString(keyContenturl)).append("\n");
    sb.append("    concatewithtype: ").append(toIndentedString(concatewithtype)).append("\n");
    sb.append("    multiple: ").append(toIndentedString(multiple)).append("\n");
    sb.append("    copyFrom: ").append(toIndentedString(copyFrom)).append("\n");
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

