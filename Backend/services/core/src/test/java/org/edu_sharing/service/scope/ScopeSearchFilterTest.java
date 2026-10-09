package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScopeSearchFilterTest {

    static final String SCOPE = "GROUP_ORG_kunde_a";
    static final Query ACL = Query.of(q -> q.term(t -> t.field("permissions.read").value("GROUP_x")));
    static final Query BASE_QUERY = Query.of(q -> q.term(t -> t.field("properties.ccm:taxonid.keyword").value("080")));

    private static ActiveScope scope(ScopeRole role) {
        return ActiveScope.builder().definition(ScopeDefinition.builder().scopeId(SCOPE).build()).role(role).build();
    }

    @ParameterizedTest
    @EnumSource(value = ScopeRole.class, names = {"GUEST", "TEMPORARY_READER"})
    void guestsSeeOnlyPublishedEntries(ScopeRole role) {
        AtomicInteger aclCalls = new AtomicInteger();
        Query filter = ScopeSearchFilter.build(scope(role), () -> {
            aclCalls.incrementAndGet();
            return ACL;
        }, List.of(BASE_QUERY));

        assertTrue(filter.isTerm());
        assertEquals(ScopeSearchFilter.FIELD_SCOPE_IDS_PUBLISHED, filter.term().field());
        assertEquals(SCOPE, filter.term().value().stringValue());
        // neither base query candidates nor the acl
        assertEquals(0, aclCalls.get());
    }

    @Test
    void editorsSeeCandidatesReadableEntriesAndPublishedEntries() {
        BoolQuery filter = ScopeSearchFilter.build(scope(ScopeRole.EDITOR), () -> ACL, List.of(BASE_QUERY)).bool();

        assertEquals("1", filter.minimumShouldMatch());
        assertEquals(3, filter.should().size());

        // (baseQuery AND EVERYONE)
        BoolQuery candidates = filter.should().get(0).bool();
        assertEquals(List.of(BASE_QUERY), candidates.filter().get(0).bool().should());
        assertEquals(CCConstants.AUTHORITY_GROUP_EVERYONE, candidates.filter().get(1).term().value().stringValue());
        // (entry AND ACL)
        BoolQuery entries = filter.should().get(1).bool();
        assertEquals(ScopeSearchFilter.FIELD_SCOPE_IDS, entries.filter().get(0).term().field());
        assertSame(ACL, entries.filter().get(1));
        // published
        assertEquals(ScopeSearchFilter.FIELD_SCOPE_IDS_PUBLISHED, filter.should().get(2).term().field());
    }

    @Test
    void withoutBaseQueryEditorsSeeOnlyEntries() {
        BoolQuery filter = ScopeSearchFilter.build(scope(ScopeRole.EDITOR), () -> ACL, List.of()).bool();

        assertEquals(2, filter.should().size());
        assertEquals(ScopeSearchFilter.FIELD_SCOPE_IDS, filter.should().get(0).bool().filter().get(0).term().field());
    }
}
