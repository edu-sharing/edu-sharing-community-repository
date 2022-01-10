package org.edu_sharing.restservices.about.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;


@Schema(description = "")
public class ServiceInstance  {
  
  private ServiceVersion version = null;
  private String endpoint = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("version")
  public ServiceVersion getVersion() {
    return version;
  }
  public void setVersion(ServiceVersion version) {
    this.version = version;
  }

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("endpoint")
  public String getEndpoint() {
    return endpoint;
  }
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceInstance {\n");
    
    sb.append("  version: ").append(version).append("\n");
    sb.append("  endpoint: ").append(endpoint).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
