package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.List;
import java.util.function.Supplier;

/**
 * Mandatory search filter inside the active scope (F1, concept §5.2/§6.2). It replaces the regular ACL filter:
 * <pre>
 * guest / temporary reader: published
 * editor:                   (baseQuery AND EVERYONE) OR (entry AND ACL) OR published
 * </pre>
 * Guests never see base query candidates, editors never see unpublished entries they can't read via ACL.
 */
public final class ScopeSearchFilter {

    public static final String FIELD_SCOPE_IDS = "scope_ids";
    public static final String FIELD_SCOPE_IDS_PUBLISHED = "scope_ids_published";
    public static final String FIELD_SCOPE_IDS_EXCLUDED = "scope_ids_excluded";

    private ScopeSearchFilter() {
    }

    /**
     * @param scope       the active scope
     * @param aclFilter   the regular read permission filter of the current user
     * @param baseQueries the resolved base queries of the scope (OR-combined), only used for editors
     */
    public static Query build(ActiveScope scope, Supplier<Query> aclFilter, List<Query> baseQueries) {
        String scopeId = scope.getScopeId();
        Query published = Query.of(q -> q.term(t -> t.field(FIELD_SCOPE_IDS_PUBLISHED).value(scopeId)));
        if (scope.getRole() != ScopeRole.EDITOR) {
            return published;
        }
        return Query.of(q -> q.bool(b -> {
            b.minimumShouldMatch("1");
            if (baseQueries != null && !baseQueries.isEmpty()) {
                b.should(s -> s.bool(candidates -> candidates
                        .filter(f -> f.bool(any -> any.should(baseQueries).minimumShouldMatch("1")))
                        .filter(f -> f.term(t -> t.field("permissions.read").value(CCConstants.AUTHORITY_GROUP_EVERYONE)))));
            }
            b.should(s -> s.bool(entries -> entries
                    .filter(f -> f.term(t -> t.field(FIELD_SCOPE_IDS).value(scopeId)))
                    .filter(aclFilter.get())));
            b.should(published);
            return b;
        }));
    }
}
