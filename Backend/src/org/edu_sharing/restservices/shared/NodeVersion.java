package org.edu_sharing.restservices.shared;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class NodeVersion  {
  
  private NodeVersionRef version = null;
  private String comment = null;
  private String modifiedAt = null;
  private Person modifiedBy = null;
  private String contentUrl = null;
  private HashMap<String, String[]> properties = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty(value = "version")
  public NodeVersionRef getVersion() {
    return version;
  }
  public void setVersion(NodeVersionRef version) {
    this.version = version;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty(value = "comment")
  public String getComment() {
    return comment;
  }
  public void setComment(String comment) {
    this.comment = comment;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty(value = "modifiedAt")
  public String getModifiedAt() {
    return modifiedAt;
  }
  public void setModifiedAt(String modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty(value = "modifiedBy")
  public Person getModifiedBy() {
    return modifiedBy;
  }
  public void setModifiedBy(Person modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty(value = "contentUrl")
  public String getContentUrl() {
    return contentUrl;
  }
  public void setContentUrl(String contentUrl) {
    this.contentUrl = contentUrl;
  }

  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty(value = "properties")
  public HashMap<String, String[]> getMetadata() {
    return properties;
  }
  public void setProperties(HashMap<String, String[]> metadata) {
    this.properties = metadata;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeVersion {\n");
    
    sb.append("  version: ").append(version).append("\n");
    sb.append("  comment: ").append(comment).append("\n");
    sb.append("  modifiedAt: ").append(modifiedAt).append("\n");
    sb.append("  modifiedBy: ").append(modifiedBy).append("\n");
    sb.append("  contentUrl: ").append(contentUrl).append("\n");
    sb.append("  properties: ").append(properties).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
