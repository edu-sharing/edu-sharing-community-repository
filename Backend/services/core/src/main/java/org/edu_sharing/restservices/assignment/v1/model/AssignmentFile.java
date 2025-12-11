package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;

/**
 * Represents a file associated with an assignment. This file may be submitted or supplementary,
 * and carries additional metadata about its state and role within the assignment context.
 *
 * @param ref          The node reference associated with this file. This is required.
 * @param referNode    The node object that this file refers to, providing further details about the file if available.
 * @param documentRole The role of the document in the context of the assignment, which can either be SUPPLEMENTARY
 *                     or SUBMITTABLE. This is required.
 * @param isDone       Indicates whether the associated task for this file is complete. This is required.
 */
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
