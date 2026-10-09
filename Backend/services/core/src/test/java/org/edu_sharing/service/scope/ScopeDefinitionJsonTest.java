package org.edu_sharing.service.scope;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScopeDefinitionJsonTest {

    @Test
    void parsesFieldListAsStoredAtTheGroup() {
        List<ScopeField> fields = ScopeDefinitionJson.parseFields("""
                [
                  { "id": "ccm:editorial_release", "visibility": "internal", "publishedValues": ["released"] },
                  { "id": "ccm:editorial_inventory", "excludedValues": ["rejected"] },
                  { "id": "cclom:title", "visibility": "public", "replaces": true, "unknown": 1 }
                ]
                """);

        assertEquals(3, fields.size());
        assertEquals(ScopeFieldVisibility.INTERNAL, fields.get(0).getVisibility());
        assertEquals(List.of("released"), fields.get(0).getPublishedValues());
        // no visibility configured: only visible inside the scope group
        assertEquals(ScopeFieldVisibility.INTERNAL, fields.get(1).getVisibility());
        assertEquals(List.of("rejected"), fields.get(1).getExcludedValues());
        assertEquals(ScopeFieldVisibility.PUBLIC, fields.get(2).getVisibility());
        assertTrue(fields.get(2).isReplaces());
        assertFalse(fields.get(0).isReplaces());
    }

    @Test
    void emptyFieldList() {
        assertEquals(List.of(), ScopeDefinitionJson.parseFields(null));
        assertEquals(List.of(), ScopeDefinitionJson.parseFields(" "));
    }

    @Test
    void invalidFieldListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScopeDefinitionJson.parseFields("{"));
        assertThrows(IllegalArgumentException.class, () -> ScopeDefinitionJson.parseFields("[{\"visibility\": \"public\"}]"));
        assertThrows(IllegalArgumentException.class, () -> ScopeDefinitionJson.parseFields("[{\"id\": \"a\", \"visibility\": \"secret\"}]"));
    }

    @Test
    void baseQueryRoundTripOneValuePerQuery() {
        ScopeBaseQuery query = ScopeBaseQuery.builder()
                .version(3)
                .queries(List.of(
                        ScopeBaseQuery.Query.builder().mds("mds").query("ngsearch").criteria(Map.of("ccm:taxonid", List.of("080"))).build(),
                        ScopeBaseQuery.Query.builder().mds("mds").query("ngsearch").criteria(Map.of("ccm:educationalcontext", List.of("grundschule"))).build()))
                .build();

        List<String> values = ScopeDefinitionJson.writeBaseQueries(query.getQueries());
        assertEquals(2, values.size());

        assertEquals(query, ScopeDefinitionJson.parseBaseQuery(values, 3L));
        assertNull(ScopeDefinitionJson.parseBaseQuery(null, null));
        assertNull(ScopeDefinitionJson.parseBaseQuery(List.of(""), 1L));
        assertEquals(0, ScopeDefinitionJson.parseBaseQuery(values, null).getVersion());
    }

    @Test
    void visibilityIsWrittenLowercase() {
        String json = ScopeDefinitionJson.write(List.of(ScopeField.builder().id("a").visibility(ScopeFieldVisibility.PUBLIC).build()));
        assertTrue(json.contains("\"visibility\":\"public\""), json);
    }
}
