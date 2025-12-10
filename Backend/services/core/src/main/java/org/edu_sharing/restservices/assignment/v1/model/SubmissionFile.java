package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record SubmissionFile(
        @NotNull
        @JsonProperty(required = true)
        NodeRef ref,
        @NotNull
        @JsonProperty(required = true)
        Node content,
        @Schema(description ="A pdf overlay document for a corrected content representation")
        Node correction,
        @Schema(description ="object of the original assignment file (if applicable)")
        AssignmentFile assignmentFile,
        @Schema(description ="only visible by the coordinator of the task")
        Submission.Status validationStatus
) {
}
