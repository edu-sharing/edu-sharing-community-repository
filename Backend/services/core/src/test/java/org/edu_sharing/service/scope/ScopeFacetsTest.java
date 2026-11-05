package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScopeFacetsTest {

    static final String SCOPE = "GROUP_ORG_wlo";
    static final Query FACET_QUERY = Query.of(q -> q.matchAll(m -> m));

    private static StringTermsAggregate terms(Object... keyCount) {
        List<StringTermsBucket> buckets = new java.util.ArrayList<>();
        for (int i = 0; i < keyCount.length; i += 2) {
            String key = (String) keyCount[i];
            long count = ((Number) keyCount[i + 1]).longValue();
            buckets.add(StringTermsBucket.of(b -> b.key(key).docCount(count)));
        }
        return StringTermsAggregate.of(t -> t.buckets(b -> b.array(buckets)).sumOtherDocCount(0L).docCountErrorUpperBound(0L));
    }

    @Test
    void ownScopeFacetUsesTheEntriesOfTheScope() {
        Aggregation aggregation = ScopeFacets.aggregation(ScopeCriteriaRewriter.Mode.OWN, SCOPE, "ccm:editorial_release", FACET_QUERY, 50, 1);

        Aggregation nested = aggregation.aggregations().get("ccm:editorial_release");
        assertEquals("scopes", nested.nested().path());
        Aggregation entries = nested.aggregations().get("ccm:editorial_release_nested");
        assertEquals(SCOPE, entries.filter().term().value().stringValue());
        assertEquals("scopes.properties.ccm:editorial_release.keyword", entries.aggregations().get("ccm:editorial_release_terms").terms().field());
        assertNull(aggregation.aggregations().get("ccm:editorial_release" + ScopeFacets.ORIGINAL_POSTFIX));
    }

    @Test
    void foreignFacetOnlyPublishedPublicEntries() {
        Aggregation aggregation = ScopeFacets.aggregation(ScopeCriteriaRewriter.Mode.FOREIGN, null, "ccm:editorial_recommendation", FACET_QUERY, 50, 1);

        Query entries = aggregation.aggregations().get("ccm:editorial_recommendation").aggregations().get("ccm:editorial_recommendation_nested").filter();
        assertEquals("scopes.published", entries.bool().filter().get(0).term().field());
        assertEquals("ccm:editorial_recommendation", entries.bool().filter().get(1).term().value().stringValue());
    }

    @Test
    void replacingFacetCountsScopeValuesAndUnreplacedOriginals() {
        Aggregation aggregation = ScopeFacets.aggregation(ScopeCriteriaRewriter.Mode.OWN_REPLACING, SCOPE, "ccm:curriculum", FACET_QUERY, 50, 1);

        Aggregation original = aggregation.aggregations().get("ccm:curriculum" + ScopeFacets.ORIGINAL_POSTFIX);
        assertEquals(SCOPE + "|ccm:curriculum", original.filter().bool().mustNot().get(0).term().value().stringValue());
        assertEquals("properties.ccm:curriculum.keyword", original.aggregations().get("ccm:curriculum_terms").terms().field());
    }

    @Test
    void notAllowedFacetsAreEmpty() {
        assertTrue(ScopeFacets.aggregation(ScopeCriteriaRewriter.Mode.NONE, SCOPE, "ccm:editorial_release", FACET_QUERY, 50, 1).filter().isMatchNone());
    }

    @Test
    void mergeAddsDisjointCountsExactly() {
        StringTermsAggregate merged = ScopeFacets.merge(terms("a", 3, "b", 1), terms("b", 4, "c", 2));

        assertEquals(List.of("b", "a", "c"), merged.buckets().array().stream().map(b -> b.key().stringValue()).toList());
        assertEquals(List.of(5L, 3L, 2L), merged.buckets().array().stream().map(StringTermsBucket::docCount).toList());
    }

    @Test
    void termsAreReadFromTheResponse() {
        Aggregate scope = Aggregate.of(a -> a.nested(n -> n.docCount(4).aggregations(Map.of(
                "ccm:curriculum_nested", Aggregate.of(f -> f.filter(fa -> fa.docCount(2).aggregations(Map.of(
                        "ccm:curriculum_terms", Aggregate.of(t -> t.sterms(terms("x", 2)))))))))));
        Aggregate original = Aggregate.of(f -> f.filter(fa -> fa.docCount(5).aggregations(Map.of(
                "ccm:curriculum_terms", Aggregate.of(t -> t.sterms(terms("x", 1, "y", 4)))))));

        StringTermsAggregate result = ScopeFacets.getTerms("ccm:curriculum", Map.of(
                "ccm:curriculum", scope,
                "ccm:curriculum" + ScopeFacets.ORIGINAL_POSTFIX, original));

        assertEquals(Map.of("x", 3L, "y", 4L), result.buckets().array().stream()
                .collect(java.util.stream.Collectors.toMap(b -> b.key().stringValue(), StringTermsBucket::docCount)));
        // regular facets are not touched
        assertNull(ScopeFacets.getTerms("cclom:general_keyword", Map.of("cclom:general_keyword", Aggregate.of(t -> t.sterms(terms("a", 1))))));
    }
}
