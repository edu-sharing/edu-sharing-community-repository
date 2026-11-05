package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Relevance inside the active scope: entries recommended by the editorial team (ccm:editorial_inventory =
 * recommended) get an additional score of repository.scopes.relevance.recommendedBoost. Added as should clause, so
 * the hits stay the same and only their order changes. Readers only for published entries.
 */
@Component
@RequiredArgsConstructor
public class ScopeRelevance {

    static final String INVENTORY_FIELD = "scopes.properties." + CCConstants.getValidLocalName(CCConstants.CCM_PROP_EDITORIAL_INVENTORY) + ".keyword";
    static final String RECOMMENDED = "recommended";

    private final ScopesSettings scopesSettings;
    private final ScopeContextResolver scopeContextResolver;

    public static Optional<ScopeRelevance> getInstance() {
        ApplicationContext context = ApplicationContextFactory.getApplicationContext();
        return context == null ? Optional.empty() : Optional.ofNullable(context.getBeanProvider(ScopeRelevance.class).getIfAvailable());
    }

    /**
     * @return the should clause boosting the recommended entries of the active scope, empty without a scope or boost
     */
    public static Optional<Query> boost() {
        return getInstance().flatMap(ScopeRelevance::boostInternal);
    }

    Optional<Query> boostInternal() {
        float boost = scopesSettings.getRelevance() == null ? 0 : (float) scopesSettings.getRelevance().getRecommendedBoost();
        if (!scopesSettings.isEnabled() || boost <= 0) {
            return Optional.empty();
        }
        return scopeContextResolver.resolve().map(scope -> {
            boolean publishedOnly = scope.getRole() != ScopeRole.EDITOR;
            return Query.of(q -> q.nested(n -> n
                    .path("scopes")
                    .scoreMode(ChildScoreMode.Max)
                    .query(nq -> nq.constantScore(cs -> cs
                            .boost(boost)
                            .filter(f -> f.bool(b -> {
                                b.filter(t -> t.term(term -> term.field("scopes.scopeId").value(scope.getScopeId())));
                                b.filter(t -> t.term(term -> term.field(INVENTORY_FIELD).value(RECOMMENDED)));
                                if (publishedOnly) {
                                    b.filter(t -> t.term(term -> term.field("scopes.published").value(FieldValue.of(true))));
                                }
                                return b;
                            }))))));
        });
    }
}
