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
import io.swagger.client.model.MetadataSetInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MdsEntriesV2
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class MdsEntriesV2 {
  @SerializedName("metadatasets")
  private List<MetadataSetInfo> metadatasets = null;

  public MdsEntriesV2 metadatasets(List<MetadataSetInfo> metadatasets) {
    this.metadatasets = metadatasets;
    return this;
  }

  public MdsEntriesV2 addMetadatasetsItem(MetadataSetInfo metadatasetsItem) {
    if (this.metadatasets == null) {
      this.metadatasets = new ArrayList<MetadataSetInfo>();
    }
    this.metadatasets.add(metadatasetsItem);
    return this;
  }

   /**
   * Get metadatasets
   * @return metadatasets
  **/
  @ApiModelProperty(value = "")
  public List<MetadataSetInfo> getMetadatasets() {
    return metadatasets;
  }

  public void setMetadatasets(List<MetadataSetInfo> metadatasets) {
    this.metadatasets = metadatasets;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MdsEntriesV2 mdsEntriesV2 = (MdsEntriesV2) o;
    return Objects.equals(this.metadatasets, mdsEntriesV2.metadatasets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadatasets);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MdsEntriesV2 {\n");
    
    sb.append("    metadatasets: ").append(toIndentedString(metadatasets)).append("\n");
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

