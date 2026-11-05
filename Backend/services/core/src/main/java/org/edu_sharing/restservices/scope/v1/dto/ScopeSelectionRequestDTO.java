package org.edu_sharing.restservices.scope.v1.dto;

import lombok.Data;

@Data
public class ScopeSelectionRequestDTO {
    /** scope group to activate, null to switch to global */
    private String scopeId;
}
