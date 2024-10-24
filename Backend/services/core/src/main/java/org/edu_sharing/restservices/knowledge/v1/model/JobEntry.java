package org.edu_sharing.restservices.knowledge.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class JobEntry  {
  
  private Job data = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("data")
  public Job getData() {
    return data;
  }
  public void setData(Job data) {
    this.data = data;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobEntry {\n");
    
    sb.append("  data: ").append(data).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
