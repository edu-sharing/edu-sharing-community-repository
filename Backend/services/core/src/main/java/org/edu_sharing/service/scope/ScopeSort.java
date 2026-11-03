package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeField;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeFieldType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Sorting by the values a node has inside the active scope (F3, sort strategy "runtime"): a painless runtime field
 * takes the value of the scope entry and falls back to the original value. Editors sort by every replaced field,
 * readers only by the public ones of published entries (the same values {@link ScopeNodeView} shows them).
 * <p>
 * cm:modified sorts by the newer one of the node and the scope entry.
 */
@Component
@RequiredArgsConstructor
public class ScopeSort {

    static final String RUNTIME_FIELD_PREFIX = "scope_sort.";
    static final String MODIFIED = "cm:modified";

    /** scope value of the property (first value), else the original doc value */
    static final String KEYWORD_SCRIPT = """
            def scopes = params._source.scopes;
            if (scopes != null) {
              for (def s : scopes) {
                if (s.scopeId == params.scopeId && (!params.publishedOnly || s.published == true)) {
                  def p = s.properties == null ? null : s.properties[params.property];
                  if (p instanceof List) { p = p.isEmpty() ? null : p[0]; }
                  if (p != null) { String v = p.toString(); emit(params.lowercase ? v.toLowerCase() : v); return; }
                }
              }
            }
            if (doc[params.field].size() > 0) { emit(doc[params.field].value); }
            """;

    /** the newer one of the original and the modification of the scope entry */
    static final String MODIFIED_SCRIPT = """
            long result = doc[params.field].size() > 0 ? doc[params.field].value : 0L;
            def scopes = params._source.scopes;
            if (scopes != null) {
              for (def s : scopes) {
                if (s.scopeId == params.scopeId && (!params.publishedOnly || s.published == true) && s.modified != null) {
                  long modified = s.modified instanceof Number ? s.modified : ZonedDateTime.parse(s.modified).toInstant().toEpochMilli();
                  if (modified > result) { result = modified; }
                }
              }
            }
            emit(result);
            """;

    private final ScopesSettings scopesSettings;
    private final ScopeContextResolver scopeContextResolver;
    private final ScopeCriteria scopeCriteria;

    public static Optional<ScopeSort> getInstance() {
        ApplicationContext context = ApplicationContextFactory.getApplicationContext();
        return context == null ? Optional.empty() : Optional.ofNullable(context.getBeanProvider(ScopeSort.class).getIfAvailable());
    }

    /**
     * @param property local name of the sorted property, e.g. cclom:title
     * @param field    the es field the regular sort uses, e.g. properties.cclom:title.keyword
     * @return true if the sort was added to the builder, false if the regular sort applies
     */
    public static boolean apply(SearchRequest.Builder builder, String property, String field, SortOrder order) {
        return getInstance().map(sort -> sort.applyInternal(builder, property, field, order)).orElse(false);
    }

    boolean applyInternal(SearchRequest.Builder builder, String property, String field, SortOrder order) {
        if (!scopesSettings.isEnabled() || scopesSettings.getSortStrategyValue() != ScopesSettings.SortStrategy.RUNTIME) {
            return false;
        }
        Optional<ActiveScope> scope = scopeContextResolver.resolve();
        if (scope.isEmpty()) {
            return false;
        }
        Optional<RuntimeField> runtimeField = createRuntimeField(scope.get(), property, field);
        if (runtimeField.isEmpty()) {
            return false;
        }
        String name = RUNTIME_FIELD_PREFIX + property;
        builder.runtimeMappings(name, runtimeField.get());
        builder.sort(sort -> sort.field(f -> f.field(name).order(order)));
        return true;
    }

    Optional<RuntimeField> createRuntimeField(ActiveScope scope, String property, String field) {
        boolean publishedOnly = scope.getRole() != ScopeRole.EDITOR;
        Map<String, JsonData> params = Map.of(
                "scopeId", JsonData.of(scope.getScopeId()),
                "property", JsonData.of(property),
                "field", JsonData.of(field),
                "publishedOnly", JsonData.of(publishedOnly),
                "lowercase", JsonData.of(field.endsWith(".sort")));
        if (MODIFIED.equals(property) && field.endsWith(".number")) {
            return Optional.of(runtimeField(RuntimeFieldType.Long, MODIFIED_SCRIPT, params));
        }
        if (!scopeCriteria.getVisibleReplacingFields(scope).contains(property)
                || !(field.endsWith(".keyword") || field.endsWith(".sort"))) {
            return Optional.empty();
        }
        return Optional.of(runtimeField(RuntimeFieldType.Keyword, KEYWORD_SCRIPT, params));
    }

    private static RuntimeField runtimeField(RuntimeFieldType type, String source, Map<String, JsonData> params) {
        return RuntimeField.of(r -> r
                .type(type)
                .script(s -> s.lang("painless").source(source).params(params)));
    }
}
