package org.edu_sharing.restservices.about.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class About  {
  
  private ServiceVersion version = null;
  private List<Service> services = new ArrayList<Service>();
  private String themesUrl;
  private long lastCacheUpdate;

  
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
  @JsonProperty("services")
  public List<Service> getServices() {
    return services;
  }
  public void setServices(List<Service> services) {
    this.services = services;
  }

  @JsonProperty
  public long getLastCacheUpdate() {
    return lastCacheUpdate;
  }

  public void setLastCacheUpdate(long lastCacheUpdate) {
    this.lastCacheUpdate = lastCacheUpdate;
  }

  @JsonProperty
  public String getThemesUrl() {
	return themesUrl;
	}
	public void setThemesUrl(String themesUrl) {
		this.themesUrl = themesUrl;
	}
@Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class About {\n");
    
    sb.append("  version: ").append(version).append("\n");
    sb.append("  services: ").append(services).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
