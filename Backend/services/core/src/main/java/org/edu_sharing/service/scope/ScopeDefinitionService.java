package org.edu_sharing.service.scope;

import java.util.List;
import java.util.Optional;

/**
 * Scope definitions stored at the scope groups (groupType = SCOPE, aspect ccm:scopeGroup).
 * Implemented in services/core (package C3), independent of the mongo plugin. Implementations carry {@link ScopesFeature}.
 */
public interface ScopeDefinitionService {

    Optional<ScopeDefinition> getDefinition(String scopeId);

    /**
     * all scopes (authority search by groupType). Admin only.
     */
    List<ScopeDefinition> getAllScopes();

    /**
     * properties that only exist in scope entries (sorted). Every other property replaces the original value
     * inside the scope (see {@link ScopeField#isReplaces()}). Admin only.
     */
    List<String> getScopeOnlyProperties();

    /**
     * scopes of all scope groups the current user is a member of
     */
    List<ScopeDefinition> getScopesOfCurrentUser();

    boolean isMember(String scopeId);

    /**
     * creates a scope group like an organization (group GROUP_ORG_&lt;name&gt;, org admin group, shared folder) and marks it
     * as scope group. The scope group gets Collaborator on the shared folder, the org admins Coordinator.
     * Global admins only (operator).
     *
     * @throws IllegalArgumentException if a field is not known by the dictionary
     */
    ScopeDefinition createScope(String name, String displayName, String mds, List<ScopeField> fields, ScopePresentation presentation);

    /**
     * replaces mds, field list and presentation settings (null removes them) of a scope group. Global admins only
     * (operator).
     */
    ScopeDefinition updateScope(String scopeId, String mds, List<ScopeField> fields, ScopePresentation presentation);

    /**
     * writes ccm:scope_base_query and increases its version.
     * Requires the org admin role of the scope group (no toolpermission).
     *
     * @return the stored query with its new version
     */
    ScopeBaseQuery setBaseQuery(String scopeId, ScopeBaseQuery query);
}
