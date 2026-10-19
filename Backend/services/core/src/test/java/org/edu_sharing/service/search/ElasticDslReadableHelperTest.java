package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ElasticDslReadableHelperTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void decodesNestedWrapperQueries() throws Exception {
        String inner = "{\"term\":{\"properties.ccm:title.keyword\":\"äöü\"}}";
        Query innerBool = Query.of(q -> q.bool(BoolQuery.of(b -> b.must(m -> m.wrapper(new ReadableWrapperQueryBuilder(inner).build())))));
        // a wrapper containing a bool which again contains a wrapper (like getStatementForValue does)
        String innerBoolJson = JsonpUtils.toJsonString(innerBool, new JacksonJsonpMapper());
        Query outer = Query.of(q -> q.wrapper(new ReadableWrapperQueryBuilder(innerBoolJson).build()));

        String readable = ElasticDslReadableHelper.toReadableJson(outer);

        assertFalse(readable.contains("wrapper"));
        JsonNode expected = mapper.readTree("{\"bool\":{\"must\":[{\"term\":{\"properties.ccm:title.keyword\":\"äöü\"}}]}}");
        assertEquals(expected, mapper.readTree(readable));
        assertEquals(expected, mapper.readTree(ElasticDslReadableHelper.toReadableJson(innerBool)));
    }

    @Test
    void keepsInvalidWrapperUntouched() throws Exception {
        String json = "{\"wrapper\":{\"query\":\"not base64!\"}}";
        assertEquals(mapper.readTree(json), mapper.readTree(ElasticDslReadableHelper.decodeWrapperQueries(json)));
    }
}
