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
import io.swagger.client.model.StatisticsSubGroup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * StatisticsKeyGroup
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-20T14:32:44.166+01:00")
public class StatisticsKeyGroup {
  @SerializedName("key")
  private String key = null;

  @SerializedName("displayName")
  private String displayName = null;

  @SerializedName("count")
  private Integer count = null;

  @SerializedName("subGroups")
  private List<StatisticsSubGroup> subGroups = null;

  public StatisticsKeyGroup key(String key) {
    this.key = key;
    return this;
  }

   /**
   * Get key
   * @return key
  **/
  @ApiModelProperty(value = "")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public StatisticsKeyGroup displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

   /**
   * Get displayName
   * @return displayName
  **/
  @ApiModelProperty(value = "")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public StatisticsKeyGroup count(Integer count) {
    this.count = count;
    return this;
  }

   /**
   * Get count
   * @return count
  **/
  @ApiModelProperty(value = "")
  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public StatisticsKeyGroup subGroups(List<StatisticsSubGroup> subGroups) {
    this.subGroups = subGroups;
    return this;
  }

  public StatisticsKeyGroup addSubGroupsItem(StatisticsSubGroup subGroupsItem) {
    if (this.subGroups == null) {
      this.subGroups = new ArrayList<StatisticsSubGroup>();
    }
    this.subGroups.add(subGroupsItem);
    return this;
  }

   /**
   * Get subGroups
   * @return subGroups
  **/
  @ApiModelProperty(value = "")
  public List<StatisticsSubGroup> getSubGroups() {
    return subGroups;
  }

  public void setSubGroups(List<StatisticsSubGroup> subGroups) {
    this.subGroups = subGroups;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StatisticsKeyGroup statisticsKeyGroup = (StatisticsKeyGroup) o;
    return Objects.equals(this.key, statisticsKeyGroup.key) &&
        Objects.equals(this.displayName, statisticsKeyGroup.displayName) &&
        Objects.equals(this.count, statisticsKeyGroup.count) &&
        Objects.equals(this.subGroups, statisticsKeyGroup.subGroups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, displayName, count, subGroups);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StatisticsKeyGroup {\n");
    
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
    sb.append("    subGroups: ").append(toIndentedString(subGroups)).append("\n");
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

