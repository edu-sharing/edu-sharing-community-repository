package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;

public record AssignmentFile(
        @JsonProperty(required = true)
        NodeRef ref,
        Node referNode,
        @JsonProperty(required = true)
        Role documentRole,
        @JsonProperty(required = true)
        boolean isDone
) {
    public enum Role {
        SUPPLEMENTARY,
        SUBMITTABLE
    }
}
