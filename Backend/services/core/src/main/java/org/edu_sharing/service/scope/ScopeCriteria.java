package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Decides how a search criterion is applied with respect to scopes (F2/F3, concept §5.4/§6.3), see
 * {@link ScopeCriteriaRewriter}. Used by the mds query builder for every criterion.
 */
@Component
@RequiredArgsConstructor
public class ScopeCriteria {

    /** editorial work sets: only meaningful for editors, they would reveal internal states to others */
    static final Set<String> EDITOR_ONLY_CRITERIA = Set.of("virtual:scope_view");

    private final ScopesSettings scopesSettings;
    private final ScopeContextResolver scopeContextResolver;
    private final DictionaryService dictionaryService;
    private volatile Set<String> scopeOnlyProperties;

    public static Optional<ScopeCriteria> getInstance() {
        ApplicationContext context = ApplicationContextFactory.getApplicationContext();
        return context == null ? Optional.empty() : Optional.ofNullable(context.getBeanProvider(ScopeCriteria.class).getIfAvailable());
    }

    /**
     * @param criterion name of the mds query parameter, e.g. "ccm:editorial_inventory"
     * @param query     the query built from the mds statement
     */
    public static Query apply(String criterion, Query query) {
        return getInstance().map(criteria -> criteria.rewrite(criterion, query)).orElse(query);
    }

    public record Decision(ScopeCriteriaRewriter.Mode mode, String scopeId) {
    }

    /**
     * @return how the criterion / facet is applied in the current request, ORIGINAL if scopes are disabled
     */
    public static Decision decide(String criterion) {
        return getInstance().map(criteria -> criteria.decideInternal(criterion))
                .orElse(new Decision(ScopeCriteriaRewriter.Mode.ORIGINAL, null));
    }

    private Decision decideInternal(String criterion) {
        if (!scopesSettings.isEnabled()) {
            return new Decision(ScopeCriteriaRewriter.Mode.ORIGINAL, null);
        }
        Optional<ActiveScope> scope = scopeContextResolver.resolve();
        return new Decision(getMode(scope, criterion), scope.map(ActiveScope::getScopeId).orElse(null));
    }

    public Query rewrite(String criterion, Query query) {
        if (!scopesSettings.isEnabled()) {
            return query;
        }
        Optional<ActiveScope> scope = scopeContextResolver.resolve();
        ScopeCriteriaRewriter.Mode mode = getMode(scope, criterion);
        return ScopeCriteriaRewriter.apply(mode, scope.map(ActiveScope::getScopeId).orElse(null), criterion, query);
    }

    ScopeCriteriaRewriter.Mode getMode(Optional<ActiveScope> scope, String criterion) {
        boolean editor = scope.map(s -> s.getRole() == ScopeRole.EDITOR).orElse(false);
        if (EDITOR_ONLY_CRITERIA.contains(criterion)) {
            return editor ? ScopeCriteriaRewriter.Mode.ORIGINAL : ScopeCriteriaRewriter.Mode.IGNORE;
        }
        Optional<ScopeField> field = scope.flatMap(s -> s.getDefinition().getFields() == null ? Optional.empty()
                : s.getDefinition().getFields().stream().filter(f -> f.getId().equals(criterion)).findFirst());
        if (field.isPresent()) {
            if (!editor && field.get().getVisibility() != ScopeFieldVisibility.PUBLIC) {
                return ScopeCriteriaRewriter.Mode.NONE;
            }
            return field.get().isReplaces() ? ScopeCriteriaRewriter.Mode.OWN_REPLACING : ScopeCriteriaRewriter.Mode.OWN;
        }
        if (getScopeOnlyProperties().contains(criterion)) {
            return ScopeCriteriaRewriter.Mode.FOREIGN;
        }
        // regular property: original values only, replaced values are never visible outside the own scope
        return ScopeCriteriaRewriter.Mode.ORIGINAL;
    }

    /**
     * properties that only exist in scope entries: aspect ccm:editorialScopeProperties and all its sub aspects, so custom
     * models add their own editorial properties via an aspect with parent ccm:editorialScopeProperties
     */
    Set<String> getScopeOnlyProperties() {
        if (scopeOnlyProperties == null) {
            QName root = QName.createQName(CCConstants.CCM_ASPECT_EDITORIAL_SCOPE_PROPERTIES);
            scopeOnlyProperties = Stream.concat(Stream.of(root), dictionaryService.getSubAspects(root, true).stream())
                    .distinct()
                    .map(dictionaryService::getAspect)
                    .filter(Objects::nonNull)
                    .flatMap(aspect -> aspect.getProperties().keySet().stream())
                    .map(q -> CCConstants.getValidLocalName(q.toString()))
                    .collect(Collectors.toUnmodifiableSet());
        }
        return scopeOnlyProperties;
    }
}
