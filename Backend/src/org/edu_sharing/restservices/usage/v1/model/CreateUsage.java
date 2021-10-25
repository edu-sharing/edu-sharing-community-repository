package org.edu_sharing.restservices.usage.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
