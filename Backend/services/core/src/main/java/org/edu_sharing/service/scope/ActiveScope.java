package org.edu_sharing.service.scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * The scope the current user is working in (at most one, see concept §2.1).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveScope implements Serializable {
    private ScopeDefinition definition;
    private ScopeRole role;
    /** id of the context the scope was resolved from, null if selected via the switcher */
    private String contextId;
    /** the user is org admin of the scope group (may change the base query) */
    private boolean orgAdmin;

    public String getScopeId() {
        return definition == null ? null : definition.getScopeId();
    }
}
