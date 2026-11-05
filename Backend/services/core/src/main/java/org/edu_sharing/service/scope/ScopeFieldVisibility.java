package org.edu_sharing.service.scope;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ScopeFieldVisibility {
    /** only visible for members of the scope group */
    @JsonProperty("internal")
    INTERNAL,
    /** also visible for guests and, for published entries, for everyone (unless the field replaces an original value) */
    @JsonProperty("public")
    PUBLIC
}
