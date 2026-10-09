package org.edu_sharing.service.scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Entry of the scope switcher.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeInfo implements Serializable {
    private String scopeId;
    private String displayName;
    private ScopeRole role;
    /** true for the scope of the current domain the user is not a member of (shown read only) */
    private boolean temporary;
}
