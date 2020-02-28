package org.edu_sharing.restservices.collection.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.restservices.shared.Node;


@ApiModel(description = "")
public class CollectionEntry  {
  
  private Node collection = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("collection")
  public Node getCollection() {
    return collection;
  }
  public void setCollection(Node collection) {
    this.collection = collection;
  }

}
