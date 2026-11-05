package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.*;

/**
 * Facets on scope properties (F2) and on replacing properties (F3, concept §6.3).
 * <p>
 * Structure of the aggregation of a facet: filter(facet query) with the sub aggregations
 * <ul>
 *     <li>&lt;property&gt;: nested(scopes) -&gt; filter(scope entries) -&gt; terms(scopes.properties.&lt;property&gt;.keyword)</li>
 *     <li>&lt;property&gt;{@value #ORIGINAL_POSTFIX} (replacing only): filter(not replaced in the scope) -&gt; terms(properties.&lt;property&gt;.keyword)</li>
 * </ul>
 * Both sets of documents are disjoint, so the counts of the merged buckets are exact.
 */
public final class ScopeFacets {

    public static final String ORIGINAL_POSTFIX = "_scope_original";
    private static final String NESTED_POSTFIX = "_nested";
    private static final String TERMS_POSTFIX = "_terms";

    private ScopeFacets() {
    }

    public static boolean isScopeFacet(ScopeCriteriaRewriter.Mode mode) {
        return mode == ScopeCriteriaRewriter.Mode.OWN || mode == ScopeCriteriaRewriter.Mode.OWN_REPLACING
                || mode == ScopeCriteriaRewriter.Mode.FOREIGN || mode == ScopeCriteriaRewriter.Mode.NONE;
    }

    public static Aggregation aggregation(ScopeCriteriaRewriter.Mode mode, String scopeId, String property, Query facetQuery, int size, int minDocCount) {
        Aggregation.Builder.ContainerBuilder builder = new Aggregation.Builder().filter(mode == ScopeCriteriaRewriter.Mode.NONE
                ? Query.of(q -> q.matchNone(m -> m))
                : facetQuery);
        Query entries = mode == ScopeCriteriaRewriter.Mode.FOREIGN
                ? Query.of(q -> q.bool(b -> b
                .filter(f -> f.term(t -> t.field("scopes.published").value(true)))
                .filter(f -> f.term(t -> t.field("scopes.publicFields").value(property)))))
                : Query.of(q -> q.term(t -> t.field("scopes.scopeId").value(scopeId == null ? "" : scopeId)));
        builder.aggregations(property, new Aggregation.Builder()
                .nested(n -> n.path("scopes"))
                .aggregations(property + NESTED_POSTFIX, new Aggregation.Builder()
                        .filter(entries)
                        .aggregations(property + TERMS_POSTFIX, terms("scopes.properties." + property + ".keyword", size, minDocCount))
                        .build())
                .build());
        if (mode == ScopeCriteriaRewriter.Mode.OWN_REPLACING) {
            builder.aggregations(property + ORIGINAL_POSTFIX, new Aggregation.Builder()
                    .filter(f -> f.bool(b -> b.mustNot(n -> n.term(t -> t.field(ScopeSearchFilterFields.SCOPE_OVERRIDES).value(scopeId + "|" + property)))))
                    .aggregations(property + TERMS_POSTFIX, terms("properties." + property + ".keyword", size, minDocCount))
                    .build());
        }
        return builder.build();
    }

    private static Aggregation terms(String field, int size, int minDocCount) {
        return new Aggregation.Builder().terms(t -> t.field(field).size(size).minDocCount(minDocCount)).build();
    }

    /**
     * @return the terms of a scope facet (sub aggregations of its filter aggregation), null if it is no scope facet
     */
    public static StringTermsAggregate getTerms(String property, Map<String, Aggregate> subAggregations) {
        Aggregate scope = subAggregations.get(property);
        if (scope == null || !scope.isNested()) {
            return null;
        }
        Aggregate entries = scope.nested().aggregations().get(property + NESTED_POSTFIX);
        if (entries == null || !entries.isFilter()) {
            return null;
        }
        StringTermsAggregate scopeTerms = entries.filter().aggregations().get(property + TERMS_POSTFIX).sterms();
        Aggregate original = subAggregations.get(property + ORIGINAL_POSTFIX);
        if (original == null) {
            return scopeTerms;
        }
        return merge(scopeTerms, original.filter().aggregations().get(property + TERMS_POSTFIX).sterms());
    }

    /**
     * adds the counts per value, sorted by count (desc) and value like elastic does
     */
    static StringTermsAggregate merge(StringTermsAggregate a, StringTermsAggregate b) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (StringTermsAggregate terms : List.of(a, b)) {
            for (StringTermsBucket bucket : terms.buckets().array()) {
                counts.merge(bucket.key().stringValue(), bucket.docCount(), Long::sum);
            }
        }
        List<StringTermsBucket> buckets = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> StringTermsBucket.of(sb -> sb.key(e.getKey()).docCount(e.getValue())))
                .toList();
        return StringTermsAggregate.of(st -> st
                .buckets(bk -> bk.array(buckets))
                .sumOtherDocCount(Objects.requireNonNullElse(a.sumOtherDocCount(), 0L) + Objects.requireNonNullElse(b.sumOtherDocCount(), 0L))
                .docCountErrorUpperBound(Objects.requireNonNullElse(a.docCountErrorUpperBound(), 0L) + Objects.requireNonNullElse(b.docCountErrorUpperBound(), 0L)));
    }
}
