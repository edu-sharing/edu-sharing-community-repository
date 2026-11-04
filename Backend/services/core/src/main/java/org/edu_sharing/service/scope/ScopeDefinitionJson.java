package org.edu_sharing.service.scope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * (De)serialization of the json properties of a scope group (ccm:scope_fields, ccm:scope_base_query).
 */
public final class ScopeDefinitionJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ScopeDefinitionJson() {
    }

    /**
     * @return the field list, empty if not set
     * @throws IllegalArgumentException if the json is invalid or a field has no id
     */
    public static List<ScopeField> parseFields(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            List<ScopeField> fields = MAPPER.readValue(json, new TypeReference<List<ScopeField>>() {
            });
            for (ScopeField field : fields) {
                if (StringUtils.isBlank(field.getId())) {
                    throw new IllegalArgumentException("scope field without id: " + json);
                }
                if (field.getVisibility() == null) {
                    // the safe default: only visible inside the scope group
                    field.setVisibility(ScopeFieldVisibility.INTERNAL);
                }
            }
            return fields;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid scope field list: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * @return the presentation settings, null if not set
     * @throws IllegalArgumentException if the json is invalid
     */
    public static ScopePresentation parsePresentation(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, ScopePresentation.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid scope settings: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * @param values  values of the multi value property ccm:scope_base_query, one json query each
     * @param version ccm:scope_base_query_version
     * @return the base query or null if no query is set
     */
    public static ScopeBaseQuery parseBaseQuery(List<String> values, Long version) {
        if (values == null || values.stream().allMatch(StringUtils::isBlank)) {
            return null;
        }
        List<ScopeBaseQuery.Query> queries = new java.util.ArrayList<>();
        for (String value : values) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            try {
                queries.add(MAPPER.readValue(value, ScopeBaseQuery.Query.class));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("invalid scope base query: " + e.getOriginalMessage(), e);
            }
        }
        return ScopeBaseQuery.builder()
                .version(version == null ? 0 : version)
                .queries(queries)
                .build();
    }

    /**
     * @return the values of the multi value property ccm:scope_base_query, one json query each
     */
    public static java.util.ArrayList<String> writeBaseQueries(List<ScopeBaseQuery.Query> queries) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (ScopeBaseQuery.Query query : queries) {
            values.add(write(query));
        }
        return values;
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getOriginalMessage(), e);
        }
    }
}
