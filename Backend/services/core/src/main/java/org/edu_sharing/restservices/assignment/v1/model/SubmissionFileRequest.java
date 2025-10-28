package org.edu_sharing.restservices.assignment.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record SubmissionFileRequest(
        @Schema(description ="id of an other file (this must create a full copy of this file)")
        String originalFile,
        @Schema(description ="id of the original assignment file (if applicable)")
        String assignmentFile,
        @Schema(description ="only editable by the coordinator of the task")
        Submission.Status validationStatus,
        Map<String, String[]> properties
) {
}
