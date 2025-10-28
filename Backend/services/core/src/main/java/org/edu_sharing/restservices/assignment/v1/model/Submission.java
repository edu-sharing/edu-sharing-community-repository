package org.edu_sharing.restservices.assignment.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.UserSimple;

import java.util.Map;

public record Submission(
        NodeRef ref,
        Map<String, String[]> properties,
        UserSimple assignee,
        @Schema(description = "internal note (not visible for assignee)")
        String validationNotes,
        String feedback,
        Status submissionStatus,
        Status validationStatus



) {
    public enum Status {
        NOT_STARTET,
        PENDING,
        FINISHED
    }
}
