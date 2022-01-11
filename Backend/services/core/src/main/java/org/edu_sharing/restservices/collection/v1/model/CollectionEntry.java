package org.edu_sharing.restservices.collection.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.restservices.shared.Node;


@Schema(description = "")
public class CollectionEntry  {
  
  private Node collection = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("collection")
  public Node getCollection() {
    return collection;
  }
  public void setCollection(Node collection) {
    this.collection = collection;
  }

}
