package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.restservices.shared.NodeRef;

import java.util.Map;

public record SubmissionFile(
        NodeRef ref,
        @Schema(description ="object of the original assignment file (if applicable)")
        AssignmentFile assignmentFile,
        Map<String, String[]> properties,
        @Schema(description ="only visible by the coordinator of the task")
        Submission.Status validationStatus
) {
}
