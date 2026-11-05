package org.edu_sharing.service.scope;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.guest.GuestConfig;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Active scope of the current request (ADR-4): context of the current domain with repository.scopes.group
 * -&gt; selection of the switcher (session) -&gt; none.
 * <p>
 * {@link #resolve()} is used by search and permission checks and therefore answers empty (instead of failing)
 * while the feature is disabled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScopeContextResolverImpl implements ScopeContextResolver {

    /** per request cache, resolve() is called on every permission check */
    static final String REQUEST_ATTRIBUTE = ScopeContextResolverImpl.class.getName() + ".activeScope";
    /** active scope of a thread without request (jobs), see {@link #runWithScope} */
    private static final ThreadLocal<ActiveScope> THREAD_SCOPE = new ThreadLocal<>();

    private final ScopesSettings scopesSettings;
    private final ScopeDefinitionService definitionService;
    private final GuestService guestService;
    private final org.edu_sharing.service.authority.AuthorityService authorityService;

    @jakarta.annotation.PostConstruct
    void registerQueryPlaceholder() {
        // ${scope} in mds queries (work set filters)
        org.edu_sharing.metadataset.v2.QueryUtils.setActiveScopeSupplier(() -> resolve().map(ActiveScope::getScopeId).orElse(null));
    }

    @Override
    public Optional<ActiveScope> resolve() {
        if (!scopesSettings.isEnabled()) {
            return Optional.empty();
        }
        if (THREAD_SCOPE.get() != null) {
            return Optional.of(THREAD_SCOPE.get());
        }
        HttpServletRequest request = getRequest();
        if (request != null && request.getAttribute(REQUEST_ATTRIBUTE) != null) {
            @SuppressWarnings("unchecked")
            Optional<ActiveScope> cached = (Optional<ActiveScope>) request.getAttribute(REQUEST_ATTRIBUTE);
            return cached;
        }
        Optional<ActiveScope> resolved = resolveUncached();
        if (request != null) {
            request.setAttribute(REQUEST_ATTRIBUTE, resolved);
        }
        return resolved;
    }

    private Optional<ActiveScope> resolveUncached() {
        // 1. domain first
        String contextId = getCurrentContextId();
        String contextScope = getContextScopeGroup(contextId);
        if (contextScope != null) {
            Optional<ScopeDefinition> definition = definitionService.getDefinition(contextScope);
            if (definition.isEmpty()) {
                log.warn("context {} references {} which is not a scope group", contextId, contextScope);
            } else {
                ScopeRole role = getRoleInContext(contextId, contextScope);
                return Optional.of(ActiveScope.builder()
                        .definition(definition.get())
                        .role(role)
                        .contextId(contextId)
                        .orgAdmin(role == ScopeRole.EDITOR && authorityService.hasAdminAccessToGroup(contextScope))
                        .build());
            }
        }
        // 2. selection of the switcher, only own scope groups
        String selected = getSelectedScope();
        if (selected != null) {
            Optional<ScopeDefinition> definition = definitionService.getDefinition(selected);
            if (definition.isPresent() && definitionService.isMember(selected)) {
                return Optional.of(ActiveScope.builder()
                        .definition(definition.get())
                        .role(ScopeRole.EDITOR)
                        .orgAdmin(authorityService.hasAdminAccessToGroup(selected))
                        .build());
            }
            // group deleted or membership revoked in the meantime
            clearSelectedScope();
        }
        // 3. global
        return Optional.empty();
    }

    private ScopeRole getRoleInContext(String contextId, String scopeId) {
        if (definitionService.isMember(scopeId)) {
            return ScopeRole.EDITOR;
        }
        GuestConfig guest = guestService.getConfig(contextId);
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        if (guest != null && guest.isEnabled() && StringUtils.equals(guest.getUsername(), user)) {
            return ScopeRole.GUEST;
        }
        return ScopeRole.TEMPORARY_READER;
    }

    @Override
    @ScopesFeature
    public List<ScopeInfo> getAvailableScopes() {
        List<ScopeInfo> result = new ArrayList<>();
        for (ScopeDefinition definition : definitionService.getScopesOfCurrentUser()) {
            result.add(ScopeInfo.builder()
                    .scopeId(definition.getScopeId())
                    .displayName(definition.getDisplayName())
                    .role(ScopeRole.EDITOR)
                    .temporary(false)
                    .build());
        }
        String contextId = getCurrentContextId();
        String contextScope = getContextScopeGroup(contextId);
        if (contextScope != null && result.stream().noneMatch(s -> s.getScopeId().equals(contextScope))) {
            definitionService.getDefinition(contextScope).ifPresent(definition -> result.add(ScopeInfo.builder()
                    .scopeId(definition.getScopeId())
                    .displayName(definition.getDisplayName())
                    .role(getRoleInContext(contextId, contextScope))
                    .temporary(true)
                    .build()));
        }
        return result;
    }

    @Override
    @ScopesFeature
    public void setSelectedScope(String scopeId) {
        HttpSession session = getSession(true);
        if (session == null) {
            throw new IllegalStateException("no session available to store the selected scope");
        }
        HttpServletRequest request = getRequest();
        if (request != null) {
            request.removeAttribute(REQUEST_ATTRIBUTE);
        }
        if (scopeId == null) {
            session.removeAttribute(CCConstants.SESSION_SCOPE_ACTIVE);
            return;
        }
        if (definitionService.getDefinition(scopeId).isEmpty()) {
            throw new ScopeNotFoundException("unknown scope " + scopeId);
        }
        if (!definitionService.isMember(scopeId)) {
            throw new InsufficientPermissionException("not a member of scope " + scopeId);
        }
        session.setAttribute(CCConstants.SESSION_SCOPE_ACTIVE, scopeId);
    }

    @Override
    public <T> T runWithScope(String scopeId, Callable<T> work) throws Exception {
        ScopeDefinition definition = definitionService.getDefinition(scopeId)
                .orElseThrow(() -> new ScopeNotFoundException("unknown scope " + scopeId));
        boolean member = definitionService.isMember(scopeId);
        ActiveScope previous = THREAD_SCOPE.get();
        THREAD_SCOPE.set(ActiveScope.builder()
                .definition(definition)
                .role(member ? ScopeRole.EDITOR : ScopeRole.TEMPORARY_READER)
                .orgAdmin(member && authorityService.hasAdminAccessToGroup(scopeId))
                .build());
        try {
            return work.call();
        } finally {
            if (previous == null) {
                THREAD_SCOPE.remove();
            } else {
                THREAD_SCOPE.set(previous);
            }
        }
    }

    // --- request access, overridable for tests

    protected HttpServletRequest getRequest() {
        Context context = Context.getCurrentInstance();
        return context == null ? null : context.getRequest();
    }

    protected HttpSession getSession(boolean create) {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getSession(create);
    }

    protected String getCurrentContextId() {
        return ConfigServiceFactory.getCurrentContextId();
    }

    /**
     * @return the scope group of the context (admin context editor), else
     * repository.context.&lt;contextId&gt;.repository.scopes.group, null if not configured
     */
    protected String getContextScopeGroup(String contextId) {
        if (StringUtils.isBlank(contextId)) {
            return null;
        }
        try {
            org.edu_sharing.alfresco.service.config.model.Context context = ConfigServiceFactory.getConfigService().getContextById(contextId);
            if (context != null && StringUtils.isNotBlank(context.scopeGroup)) {
                return context.scopeGroup.trim();
            }
        } catch (Exception e) {
            log.warn("could not read context {}: {}", contextId, e.getMessage());
        }
        Config config = LightbendConfigLoader.get();
        String path = ConfigUtil.joinPath("repository", "context", contextId) + "." + ScopesSettings.CONTEXT_GROUP_PATH;
        return config.hasPath(path) ? StringUtils.trimToNull(config.getString(path)) : null;
    }

    private String getSelectedScope() {
        HttpSession session = getSession(false);
        return session == null ? null : (String) session.getAttribute(CCConstants.SESSION_SCOPE_ACTIVE);
    }

    private void clearSelectedScope() {
        HttpSession session = getSession(false);
        if (session != null) {
            session.removeAttribute(CCConstants.SESSION_SCOPE_ACTIVE);
        }
    }
}
