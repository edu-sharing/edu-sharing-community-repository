package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.UserSimple;

import java.util.Map;

public record Submission(
        @JsonProperty(required = true)
        NodeRef ref,
        @JsonProperty(required = true)
        UserSimple assignee,
        @Schema(description = "internal note (not visible for assignee)")
        String validationNotes,
        String feedback,
        @JsonProperty(required = true)
        Status submissionStatus,
        @JsonProperty(required = true)
        Status validationStatus

        /**
         * @TODO: We would need a date of at least
         * the date of submission (from the student)
         * The date of getting it back (from the teacher)
         */
) {
    public enum Status {
        NOT_STARTET,
        PENDING,
        FINISHED
    }
}
