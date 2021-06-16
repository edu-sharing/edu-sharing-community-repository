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
import io.swagger.client.model.Element;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CollectionCounts
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class CollectionCounts {
  @SerializedName("refs")
  private List<Element> refs = null;

  @SerializedName("collections")
  private List<Element> collections = null;

  public CollectionCounts refs(List<Element> refs) {
    this.refs = refs;
    return this;
  }

  public CollectionCounts addRefsItem(Element refsItem) {
    if (this.refs == null) {
      this.refs = new ArrayList<Element>();
    }
    this.refs.add(refsItem);
    return this;
  }

   /**
   * Get refs
   * @return refs
  **/
  @ApiModelProperty(value = "")
  public List<Element> getRefs() {
    return refs;
  }

  public void setRefs(List<Element> refs) {
    this.refs = refs;
  }

  public CollectionCounts collections(List<Element> collections) {
    this.collections = collections;
    return this;
  }

  public CollectionCounts addCollectionsItem(Element collectionsItem) {
    if (this.collections == null) {
      this.collections = new ArrayList<Element>();
    }
    this.collections.add(collectionsItem);
    return this;
  }

   /**
   * Get collections
   * @return collections
  **/
  @ApiModelProperty(value = "")
  public List<Element> getCollections() {
    return collections;
  }

  public void setCollections(List<Element> collections) {
    this.collections = collections;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CollectionCounts collectionCounts = (CollectionCounts) o;
    return Objects.equals(this.refs, collectionCounts.refs) &&
        Objects.equals(this.collections, collectionCounts.collections);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refs, collections);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CollectionCounts {\n");
    
    sb.append("    refs: ").append(toIndentedString(refs)).append("\n");
    sb.append("    collections: ").append(toIndentedString(collections)).append("\n");
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

