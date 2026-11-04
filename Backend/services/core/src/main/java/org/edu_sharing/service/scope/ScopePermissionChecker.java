package org.edu_sharing.service.scope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Scope as source of permissions (D2, concept §5.3): inside the active scope, content with a published entry is
 * readable for every role (editor, guest, temporary reader), even without an ACL entry.
 * <p>
 * Only consumer permissions are granted ({@link CCConstants#getUsagePermissions()}, the same as for usages and
 * collections), never Write or CCPublish. Unpublished entries grant nothing, the ACLs alone apply there (also for
 * editors and own uploads). The original's ACLs are never changed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScopePermissionChecker {

    private final ScopesSettings scopesSettings;
    private final ScopeContextResolver scopeContextResolver;
    private final ScopeServiceFactory scopeServiceFactory;

    /**
     * @return the checker, or empty if the edu-sharing context is not available (e.g. alfresco internal calls)
     */
    public static Optional<ScopePermissionChecker> getInstance() {
        ApplicationContext context = ApplicationContextFactory.getApplicationContext();
        return context == null ? Optional.empty() : Optional.ofNullable(context.getBeanProvider(ScopePermissionChecker.class).getIfAvailable());
    }

    /**
     * @return the requested permissions the active scope grants on the node, empty if none
     */
    public List<String> getGrantedPermissions(String nodeId, Collection<String> permissions) {
        if (!scopesSettings.isEnabled() || nodeId == null) {
            return List.of();
        }
        List<String> requested = permissions.stream().filter(CCConstants.getUsagePermissions()::contains).toList();
        if (requested.isEmpty()) {
            return List.of();
        }
        Optional<ActiveScope> scope = scopeContextResolver.resolve();
        if (scope.isEmpty()) {
            return List.of();
        }
        return isPublished(scope.get().getScopeId(), nodeId) ? requested : List.of();
    }

    private boolean isPublished(String scopeId, String nodeId) {
        // references and published copies belong to the original. As system: the user may not read the node itself
        String originalId = AuthenticationUtil.runAsSystem(() -> NodeServiceFactory.getInstance().getLocalService().getOriginalNode(nodeId).getId());
        Boolean cached = ScopePublishedCache.get(scopeId, originalId);
        if (cached != null) {
            return cached;
        }
        boolean published = scopeServiceFactory.getLocalService().isPublishedInScope(scopeId, originalId);
        ScopePublishedCache.put(scopeId, originalId, published);
        return published;
    }
}
