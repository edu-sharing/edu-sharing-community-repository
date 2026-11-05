package org.edu_sharing.restservices.scope.v1.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ScopeAssignmentRequestDTO {
    /** local property name -&gt; values. An empty list removes the property from the entry */
    private Map<String, List<String>> properties;
    /** optional reason, stored in the history */
    private String reason;
}
