package org.edu_sharing.restservices.assignment.v1.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public record PermissionRequest(
        @NotEmpty
        String authorityName,
        @NotNull
        Assignment.Role role){

}
