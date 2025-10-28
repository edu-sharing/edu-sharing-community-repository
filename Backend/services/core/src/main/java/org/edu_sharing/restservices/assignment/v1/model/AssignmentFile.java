package org.edu_sharing.restservices.assignment.v1.model;

import org.edu_sharing.restservices.shared.NodeRef;

import java.util.Map;

public record AssignmentFile(
        NodeRef ref,
        Map<String, String[]> properties,
        Role documentRole,
        boolean isDone
) {

    public enum Role {
        SUPPLEMENTARY,
        SUBMITTABLE
    }
}
