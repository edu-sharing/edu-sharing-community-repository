package org.edu_sharing.restservices.assignment.v1.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * Represents a request for handling an assignment file within the assignment processing system.
 * This record encapsulates information about the assignment file, its role, and completion status.
 *
 * @param refId - The unique identifier of the file reference. This field cannot be empty.
 * @param documentRole - The role of the file within the assignment, such as supplementary or submittable. This field cannot be null.
 *
 * This request is used in operations where assignment files need to be created, modified, or managed.
 */
@Validated
public record AssignmentFileRequest(
        @NotEmpty
        String refId,
        @NotNull
        AssignmentFile.Role documentRole
) {
}
