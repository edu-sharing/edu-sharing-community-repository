package org.edu_sharing.service.scope;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.Optional;

/**
 * Resolves the active scope of the current request (ADR-4). Implemented in services/core (package D1).
 * Order: context of the current domain with repository.scopes.group -&gt; selection of the switcher (session) -&gt; none.
 * Implementations carry {@link ScopesFeature}.
 */
public interface ScopeContextResolver {

    /**
     * @return the active scope, empty if the user works globally or the feature is disabled
     */
    Optional<ActiveScope> resolve();

    /**
     * entries of the switcher: the scopes of the user's scope groups plus the temporary scope of the current domain
     */
    List<ScopeInfo> getAvailableScopes();

    /**
     * stores the selection of the switcher in the session.
     *
     * @param scopeId scope group of the current user, null to switch to global
     * @throws org.edu_sharing.service.InsufficientPermissionException if the user is not a member of the scope group
     */
    void setSelectedScope(String scopeId);

    /**
     * runs the work with the given scope as active scope of the current thread, for jobs without request and session
     * (e.g. the bulk job resolving its search). The role is resolved for the current user like for a selection of the
     * switcher: members are editors, others readers.
     *
     * @throws ScopeNotFoundException if the scope does not exist
     */
    <T> T runWithScope(String scopeId, Callable<T> work) throws Exception;

    /**
     * @throws org.edu_sharing.service.InsufficientPermissionException unless an active scope exists and the user is
     *                                                                 org admin of its group
     */
    static void requireOrgAdminOfActiveScope() {
        ScopeContextResolver resolver = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext().getBean(ScopeContextResolver.class);
        if (resolver.resolve().map(ActiveScope::isOrgAdmin).orElse(false)) {
            return;
        }
        throw new org.edu_sharing.service.InsufficientPermissionException("only org admins of the active scope group can search without the scope");
    }
}
