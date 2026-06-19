package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

public record NodePermissionInheritance(
        @NotEmpty
        @JsonProperty(required = true)
        String node,
        @JsonProperty(required = true)
        boolean inherit
) {
}
