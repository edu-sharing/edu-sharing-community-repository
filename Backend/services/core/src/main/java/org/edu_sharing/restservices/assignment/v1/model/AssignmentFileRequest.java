package org.edu_sharing.restservices.assignment.v1.model;

public record AssignmentFileRequest(
        String refId,
        AssignmentFile.Role documentRole,
        boolean isDone
) {
}
