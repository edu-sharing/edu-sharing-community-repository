package org.edu_sharing.restservices.assignment.v1.model;

import jakarta.validation.constraints.NotNull;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import org.springframework.validation.annotation.Validated;

@Validated
public record AssignmentFileRequest(
        @NotEmpty
        String refId,
        @NotNull
        AssignmentFile.Role documentRole,
        @NotNull
        boolean isDone
) {
}
