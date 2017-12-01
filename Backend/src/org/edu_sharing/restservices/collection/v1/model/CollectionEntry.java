package org.edu_sharing.restservices.collection.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class CollectionEntry  {
  
  private Collection collection = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("collection")
  public Collection getCollection() {
    return collection;
  }
  public void setCollection(Collection collection) {
    this.collection = collection;
  }

}
