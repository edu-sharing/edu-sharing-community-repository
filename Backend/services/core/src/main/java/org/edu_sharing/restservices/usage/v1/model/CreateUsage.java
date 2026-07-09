package org.edu_sharing.restservices.usage.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUsage {
    public String appId;
    public String courseId;
    public String resourceId;
    public String nodeId;
    public String nodeVersion;
}
