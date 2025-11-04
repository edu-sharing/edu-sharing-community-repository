package org.edu_sharing.restservices.assignment.v1.model;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;

public record AssignmentFile(
        NodeRef ref,
        Node referNode,
        Role documentRole,
        boolean isDone
) {
    public enum Role {
        SUPPLEMENTARY,
        SUBMITTABLE
    }
}
