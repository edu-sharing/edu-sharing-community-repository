package org.edu_sharing.restservices.assignment.v1.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Date;
import java.util.List;

@Validated
public record CreateAssignmentRequest(
        String id,
        @NotEmpty
        String title,
        @NotNull
        String summary,
        @NotNull
        Date startTime,
        @NotNull
        Date endTime,
        @NotNull
        Assignment.Status status,
        @NotNull
        Assignment.Type type,
        @NotNull
        boolean allowAdditionalDocumentSubmission,
        @NotNull
        List<PermissionRequest> permissions,
        @NotNull
        List<AssignmentFileRequest> assignmentFiles
) {
}

