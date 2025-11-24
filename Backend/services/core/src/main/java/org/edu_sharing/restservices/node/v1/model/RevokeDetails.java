package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class RevokeDetails {
    @JsonProperty
    private String reason;

    @JsonProperty
    @JsonPropertyDescription("remove this node from all collections it is included")
    boolean cleanupCollections;
    @JsonProperty
    @JsonPropertyDescription("remove all set usages (i.e. in lms) for this course")
    boolean cleanupUsages;

    @JsonProperty
    @JsonPropertyDescription("remove GROUP_EVERYONE / publish permission from this node")
    boolean unpublish;

    @JsonProperty
    @JsonPropertyDescription("remove the nodes content")
    boolean removeContent = true;
}
