package org.edu_sharing.restservices.assignment.v1.model;

import jakarta.ws.rs.FormParam;

import java.util.Map;

public record AssignmentFileRequest(
        @FormParam("refId") String refId,
        @FormParam("documentRole") AssignmentFile.Role documentRole,
        @FormParam("isDone") boolean isDone,
        @FormParam("properties") Map<String, String[]> properties
) {
}
