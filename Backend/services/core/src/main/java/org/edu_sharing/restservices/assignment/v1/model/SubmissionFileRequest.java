package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
public record SubmissionFileRequest(
        @Schema(description ="id of an other file (this must create a full copy of this file)")
        String originalFile,
        @Schema(description ="id of the original assignment file (if applicable)")
        String assignmentFile,
        @NotNull
        @JsonProperty(required = true)
        @Schema(description ="properties applied to the submitted file")
        Map<String, String[]> properties
) {
}
