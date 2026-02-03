package org.edu_sharing.restservices.relation.v1.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.edu_sharing.service.relations.InputRelationType;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
public record CreateRelationRequest(
        @NotEmpty String fromNode,
        @NotEmpty String toNode,
        @NotNull InputRelationType type,
        boolean isAiGenerated,
        boolean isEvaluated,
        Map<String, Object> metadata
) {
}
