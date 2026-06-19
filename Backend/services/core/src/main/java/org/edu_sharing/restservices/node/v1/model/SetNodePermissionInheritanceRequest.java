package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Valid
public record SetNodePermissionInheritanceRequest(
        @NotEmpty
        @JsonProperty(required = true)
        List<NodePermissionInheritance> inheritanceList
) {
}

