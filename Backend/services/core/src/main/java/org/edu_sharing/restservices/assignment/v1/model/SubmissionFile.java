package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.restservices.shared.NodeRef;

import java.util.Map;

public record SubmissionFile(
        @JsonProperty(required = true)
        NodeRef ref,
        @Schema(description ="object of the original assignment file (if applicable)")
        AssignmentFile assignmentFile,
        @JsonProperty(required = true)
        Map<String, String[]> properties,
        @JsonProperty(required = true)
        @Schema(description ="only visible by the coordinator of the task")
        Submission.Status validationStatus
) {
}
