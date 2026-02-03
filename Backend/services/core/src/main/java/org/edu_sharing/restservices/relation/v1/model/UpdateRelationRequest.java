package org.edu_sharing.restservices.relation.v1.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.edu_sharing.service.relations.InputRelationType;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
public record UpdateRelationRequest(
        @NotEmpty String fromNode,
        @NotEmpty String toNode,
        @NotNull InputRelationType type,
        Map<String, Object> metadata
) {
}
