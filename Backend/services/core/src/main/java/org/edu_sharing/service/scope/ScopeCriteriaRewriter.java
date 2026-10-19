package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.edu_sharing.service.search.ReadableWrapperQueryBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Rewrites a search criterion on a property to the scope entries (F2/F3, concept §6.3). Pure: the mode is decided by
 * {@link ScopeCriteria}.
 */
public final class ScopeCriteriaRewriter {

    public enum Mode {
        /** a regular property, the query stays as it is */
        ORIGINAL,
        /** a property of the own active scope: its entries only */
        OWN,
        /** a replacing property of the own active scope: the scope value if replaced, the original value otherwise */
        OWN_REPLACING,
        /** a scope property outside the own scope: only published entries where the property is public */
        FOREIGN,
        /** not allowed for the current user (e.g. an internal property for guests): matches nothing */
        NONE,
        /** criterion without effect for the current user (editorial work sets for guests): matches everything */
        IGNORE
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String NESTED_PATH = "scopes";
    private static final String PREFIX = NESTED_PATH + ".";

    private ScopeCriteriaRewriter() {
    }

    public static Query apply(Mode mode, String scopeId, String property, Query original) {
        return switch (mode) {
            case ORIGINAL -> original;
            case NONE -> Query.of(q -> q.matchNone(m -> m));
            case IGNORE -> Query.of(q -> q.matchAll(m -> m));
            case OWN -> own(scopeId, rewrite(original, property));
            case OWN_REPLACING -> Query.of(q -> q.bool(b -> b
                    .minimumShouldMatch("1")
                    .should(own(scopeId, rewrite(original, property)))
                    .should(s -> s.bool(o -> o
                            .must(original)
                            .mustNot(n -> n.term(t -> t.field(ScopeSearchFilterFields.SCOPE_OVERRIDES).value(scopeId + "|" + property)))))));
            case FOREIGN -> Query.of(q -> q.nested(n -> n
                    .path(NESTED_PATH)
                    .query(nq -> nq.bool(b -> b
                            .filter(f -> f.term(t -> t.field(PREFIX + "published").value(true)))
                            .filter(f -> f.term(t -> t.field(PREFIX + "publicFields").value(property)))
                            .must(rewrite(original, property))))));
        };
    }

    /**
     * full text criterion (e.g. ngsearchword over many fields): matches the original values or the replaced values of
     * the own scope entry
     *
     * @param properties    replacing properties the user may see in the own scope
     * @param publishedOnly readers: only published entries
     */
    public static Query fullText(String scopeId, Collection<String> properties, boolean publishedOnly, Query original) {
        if (scopeId == null || properties.isEmpty()) {
            return original;
        }
        Query replaced = rewrite(original, properties);
        return Query.of(q -> q.bool(b -> b
                .minimumShouldMatch("1")
                .should(original)
                .should(s -> s.nested(n -> n
                        .path(NESTED_PATH)
                        .query(nq -> nq.bool(own -> {
                            own.filter(f -> f.term(t -> t.field(PREFIX + "scopeId").value(scopeId)));
                            if (publishedOnly) {
                                own.filter(f -> f.term(t -> t.field(PREFIX + "published").value(true)));
                            }
                            return own.must(replaced);
                        }))))));
    }

    private static Query own(String scopeId, Query rewritten) {
        return Query.of(q -> q.nested(n -> n
                .path(NESTED_PATH)
                .query(nq -> nq.bool(b -> b
                        .filter(f -> f.term(t -> t.field(PREFIX + "scopeId").value(scopeId)))
                        .must(rewritten)))));
    }

    /**
     * @return the query with every field "properties.&lt;property&gt;[.suffix]" moved to "scopes.properties.&lt;property&gt;[.suffix]"
     */
    static Query rewrite(Query query, String property) {
        return rewrite(query, List.of(property));
    }

    /**
     * @return the query with every field of the given properties moved to the scope entries
     */
    static Query rewrite(Query query, Collection<String> properties) {
        try {
            JsonNode json = MAPPER.readTree(JsonpUtils.toJsonString(query, new JacksonJsonpMapper()));
            List<String> fields = properties.stream().map(property -> "properties." + property).toList();
            String rewritten = MAPPER.writeValueAsString(rewriteNode(json, fields));
            return Query.of(q -> q.wrapper(new ReadableWrapperQueryBuilder(rewritten).build()));
        } catch (Exception e) {
            throw new IllegalStateException("could not rewrite query for scope properties " + properties, e);
        }
    }

    private static boolean isField(String value, List<String> fields) {
        return fields.stream().anyMatch(field -> value.equals(field) || value.startsWith(field + "."));
    }

    private static JsonNode rewriteNode(JsonNode node, List<String> field) throws Exception {
        if (node instanceof ObjectNode object) {
            List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = object.fields(); it.hasNext(); ) {
                entries.add(it.next());
            }
            ObjectNode result = MAPPER.createObjectNode();
            for (Map.Entry<String, JsonNode> entry : entries) {
                // mds statements are wrapper queries: base64 encoded json
                if (entry.getKey().equals("wrapper") && entry.getValue().has("query") && entry.getValue().get("query").isTextual()) {
                    JsonNode statement = MAPPER.readTree(new String(Base64.getDecoder().decode(entry.getValue().get("query").asText()), StandardCharsets.UTF_8));
                    String rewritten = MAPPER.writeValueAsString(rewriteNode(statement, field));
                    ObjectNode wrapper = MAPPER.createObjectNode();
                    wrapper.put("query", Base64.getEncoder().encodeToString(rewritten.getBytes(StandardCharsets.UTF_8)));
                    result.set("wrapper", wrapper);
                    continue;
                }
                String key = isField(entry.getKey(), field) ? PREFIX + entry.getKey() : entry.getKey();
                result.set(key, rewriteNode(entry.getValue(), field));
            }
            return result;
        }
        if (node instanceof ArrayNode array) {
            ArrayNode result = MAPPER.createArrayNode();
            for (JsonNode child : array) {
                result.add(rewriteNode(child, field));
            }
            return result;
        }
        // field references as values, e.g. {"exists":{"field":"properties.x"}}, {"multi_match":{"fields":[...]}}
        if (node instanceof TextNode text && isField(text.asText(), field)) {
            return TextNode.valueOf(PREFIX + text.asText());
        }
        return node;
    }
}
