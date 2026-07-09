package org.edu_sharing.restservices.usage.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUsage {
    @JsonProperty
    public String appId;
    @JsonProperty
    public String courseId;
    @JsonProperty
    public String resourceId;
    @JsonProperty
    public String nodeId;
    @JsonProperty(required = false)
    public String nodeVersion;
}
