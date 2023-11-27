package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchServiceElasticTestUtils
{
    static JacksonJsonpMapper mapper = new JacksonJsonpMapper();

    private static String indentJson(String json) {
        JsonParser parser = new JsonParser();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonElement el = parser.parse(json);
        return gson.toJson(el);
    }
    public static String indentJson(BoolQuery.Builder builder) {
        String json = JsonpUtils.toJsonString(builder.build()._toQuery(), mapper);

        return indentJson(json);
    }

    public static void assertQuery(String expected, BoolQuery.Builder actual) {
        assertEquals(indentJson(expected), indentJson(actual));
    }
    public static void assertFacet(String expected, Aggregation agg) {
        assertEquals(indentJson(expected), indentJson(JsonpUtils.toJsonString(agg, mapper)));

    }
}
