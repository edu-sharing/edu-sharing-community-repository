package org.edu_sharing.metadataset.v2.tools;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.restservices.search.v1.model.SearchFacet;
import org.edu_sharing.service.search.ReadableWrapperQueryBuilder;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElasticTestUtils;
import org.edu_sharing.service.search.model.SearchToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class MetadataElasticSearchHelperTest {

    private MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic;
    private String basequery;
    private MetadataQuery query;
    private MetadataQueries queries;
    private MetadataSet mds;
    private AuthenticationToolAPI authenticationToolApi;
    private MockedConstruction<AuthenticationToolAPI> authenticationToolApiConstruction;
    private MockedStatic<AuthenticationToolAPI> authenticationToolApiMockedStatic;

    @BeforeEach
    void beforeEach() {

        authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");
        authenticationToolApi = Mockito.mock(AuthenticationToolAPI.class);
        authenticationToolApiConstruction = Mockito.mockConstruction(AuthenticationToolAPI.class);
        when(authenticationToolApi.getCurrentLocale()).thenReturn("en");

        authenticationToolApiMockedStatic = Mockito.mockStatic(AuthenticationToolAPI.class);
        authenticationToolApiMockedStatic.when(AuthenticationToolAPI::getInstance).thenReturn(authenticationToolApi);
        mds = new MetadataSet();
        query = new MetadataQuery();
        basequery = "{\"exists\":{\"field\": \"type\"}}";
        query.setBasequery(new HashMap<>() {{
            put(null, basequery);
        }});
        query.setId("ngsearch");
        query.setJoin("AND");
        query.setSyntax(MetadataReader.QUERY_SYNTAX_DSL);
        queries = new MetadataQueries();
        queries.setQueries(Collections.singletonList(query));
        mds.setQueries(new HashMap<>() {{
            put(MetadataReader.QUERY_SYNTAX_DSL, queries);
        }});
    }

    @AfterEach
    void afterEach() {
        authenticationUtilMockedStatic.close();
        authenticationToolApiConstruction.close();
        authenticationToolApiMockedStatic.close();
    }

    @Test
    void getElasticSearchQueryBasic() throws JsonProcessingException {
        SearchToken token = new SearchToken();
        BoolQuery.Builder result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, Collections.emptyMap());

        SearchServiceElasticTestUtils.assertQuery(String.format(
                "{\"bool\":{\"must\":[{\"wrapper\":{\"query\": \"%s\"}}]}}",
                Base64.getEncoder().encodeToString(basequery.getBytes())
        ), result);

        token.setContentType(SearchService.ContentType.FILES);
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, Collections.emptyMap());
        String expected = "{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"match\":{\"type\":{\"query\":\"ccm:io\"}}}]}}],\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"}}]}}";
        SearchServiceElasticTestUtils.assertQuery(expected, result);
    }


    @Test
    void getElasticSearchQueryMultipleParameter() {
        SearchToken token = new SearchToken();
        List<MetadataQueryParameter> parameters = new ArrayList<>();
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax(), null);
        parameter.setMultiple(true);
        parameter.setMultiplejoin(MetadataQueryParameter.ParameterJoinStrategy.AND);
        parameter.setName("parameter");
        parameter.setStatements(new HashMap<>() {{
            put(null, "{\"match\":{\"some_field\":\"{$value}\"}}");
        }});
        parameters.add(parameter);
        query.setParameters(parameters);
        BoolQuery.Builder result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
        }});
        SearchServiceElasticTestUtils.assertQuery(
                "{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"must\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            }\n          ]}\n      }\n    ]}\n}",
                result
        );

        // OR JOIN
        parameter.setMultiplejoin(MetadataQueryParameter.ParameterJoinStrategy.OR);
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
        }});
        SearchServiceElasticTestUtils.assertQuery(
                "{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            }\n          ]}\n      }\n    ]}\n}",
                result
        );



        // 2 Parameters AND combined
        MetadataQueryParameter parameter2 = new MetadataQueryParameter(query.getSyntax(), null);
        parameter2.setName("parameter2");
        parameter2.setMultiple(true);
        parameter2.setMultiplejoin(MetadataQueryParameter.ParameterJoinStrategy.OR);
        parameters.add(parameter2);
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
            put("parameter2", new String[]{"a", "b"});
        }});
        SearchServiceElasticTestUtils.assertQuery(
                "{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            }\n          ]}\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IiphKiJ9fX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IipiKiJ9fX0=\"\n              }\n            }\n          ]}\n      }\n    ]}\n}",
                result
        );
        // 2 Parameters or combined
        query.setJoin("OR");
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
            put("parameter2", new String[]{"a", "b"});
        }});
        SearchServiceElasticTestUtils.assertQuery(
                "{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n        }\n      }\n    ],\n    \"should\" : [\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0ifX0=\"\n              }\n            }\n          ]}\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IiphKiJ9fX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IipiKiJ9fX0=\"\n              }\n            }\n          ]}\n      }\n    ]}\n}",
                result
        );


        // INTERNAL join
        token = new SearchToken();
        parameter.setMultiplejoin(MetadataQueryParameter.ParameterJoinStrategy.INTERNAL);
        parameter.setStatements(new HashMap<>() {{
            put(null, "{\"regexp\":{\"some_field\":\"${value[0]}|${value[1]}\"}}");
        }});
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
        }});
        parameters.clear();
        parameters.add(parameter);
        SearchServiceElasticTestUtils.assertQuery(
                "{\n" +
                        "  \"bool\": {\n" +
                        "    \"must\": [\n" +
                        "      {\n" +
                        "        \"wrapper\": {\n" +
                        "          \"query\": \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9fQ==\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"should\": [\n" +
                        "      {\n" +
                        "        \"bool\": {\n" +
                        "          \"should\": [\n" +
                        "            {\n" +
                        "              \"wrapper\": {\n" +
                        "                \"query\": \"eyJib29sIjp7Im11c3QiOlt7IndyYXBwZXIiOnsicXVlcnkiOiJleUp5WldkbGVIQWlPbnNpYzI5dFpWOW1hV1ZzWkNJNkltRjhZaUo5ZlE9PSJ9fV19fQ==\"\n" +
                        "              }\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}",
                result
        );


    }

    @Test
    void getAggregations() {
        SearchToken token = new SearchToken();
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax(), null);
        parameter.setName("test_facet");

        query.setParameters(Collections.singletonList(parameter));

        Map<String, Aggregation> result = MetadataElasticSearchHelper.applyAggregations(new SearchRequest.Builder(), mds, query, Collections.emptyMap(),
                Collections.singletonList(new SearchFacet("test_facet", null)), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}}]}}}",
                result.get("test_facet")
        );

        // 2 facets
        MetadataQueryParameter parameter2 = new MetadataQueryParameter(query.getSyntax(), null);
        parameter2.setName("test_facet2");
        query.setParameters(Arrays.asList(parameter, parameter2));

        result = MetadataElasticSearchHelper.applyAggregations(new SearchRequest.Builder(), mds, query, Collections.emptyMap(),
                Arrays.asList(new SearchFacet("test_facet", null), new SearchFacet("test_facet2", null)), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(2, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}}]}}" +
                        "}",
                result.get("test_facet"));
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet2\":{\"terms\":{\"field\":\"properties.test_facet2.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}}]}}" +
                        "}",
                result.get("test_facet2")
        );

        // multi term facet
        parameter.setFacet(new MetadataQueryParameter.MetadataQueryFacet(
                MetadataQueryParameter.MetadataQueryFacet.Type.term,
                MetadataQueryParameter.MetadataQueryFacet.SortBy.count,
                MetadataQueryParameter.MetadataQueryFacet.SortOrder.asc,
                null,
                null,
                false,
                Arrays.asList(
                        new MetadataQueryParameter.MetadataQueryFacetItem("facet1", null),
                        new MetadataQueryParameter.MetadataQueryFacetItem("facet2", null)
                ))
        );
        query.setParameters(Collections.singletonList(parameter));

        result = MetadataElasticSearchHelper.applyAggregations(new SearchRequest.Builder(), mds, query, Collections.emptyMap(),
                Collections.singletonList(new SearchFacet("test_facet", null)), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"multi_terms\":{\"min_doc_count\":4,\"size\":250,\"terms\":[{\"field\":\"facet1\",\"missing\":\"\"},{\"field\":\"facet2\",\"missing\":\"\"}]}}}," +
                        "\"meta\":{\"type\":\"multi_terms\"}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}}]}}" +
                        "}",
                result.get("test_facet")
        );



        // geo test
        parameter.setFacet(new MetadataQueryParameter.MetadataQueryFacet(
                MetadataQueryParameter.MetadataQueryFacet.Type.geo_grid,
                MetadataQueryParameter.MetadataQueryFacet.SortBy.count,
                MetadataQueryParameter.MetadataQueryFacet.SortOrder.asc,
                null,
                null,
                false,
                Collections.emptyList())
        );
        query.setParameters(Collections.singletonList(parameter));
        SearchRequest.Builder builder = new SearchRequest.Builder();
        result = MetadataElasticSearchHelper.applyAggregations(builder, mds, query, Collections.emptyMap(),
                Collections.singletonList(new SearchFacet("test_facet", Map.of("precision", 7))), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertNotNull(builder.build().runtimeMappings().get(MetadataElasticSearchHelper.GEOPOINT_RUNTIME_FIELD).script());
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"geotile_grid\":{\"field\":\"geo_point_runtime\",\"precision\":7.0}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}}]}}" +
                        "}",
                result.get("test_facet")
        );
    }


    /** base64 of a statement, the form {@link ReadableWrapperQueryBuilder} puts into a wrapper query */
    private static String encoded(String statement) {
        return Base64.getEncoder().encodeToString(statement.getBytes());
    }

    private MetadataQueryParameter multiParameter(String name, MetadataQueryParameter.ParameterJoinStrategy join, String field) {
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax(), null);
        parameter.setName(name);
        parameter.setMultiple(true);
        parameter.setMultiplejoin(join);
        parameter.setStatements(new HashMap<>() {{
            put(null, "{\"match\":{\"" + field + "\":\"${value}\"}}");
        }});
        return parameter;
    }

    @Test
    void getAggregationsHoistsSharedQueryAndKeepsOnlyFacetDifference() {
        SearchToken token = new SearchToken();
        // an "AND" joined criterion is identical for every facet -> shared
        MetadataQueryParameter shared = multiParameter("shared_param", MetadataQueryParameter.ParameterJoinStrategy.AND, "shared_field");
        // an "OR" joined criterion is subject to excludeOwn -> differs per facet
        MetadataQueryParameter own = multiParameter("own_param", MetadataQueryParameter.ParameterJoinStrategy.OR, "own_field");
        query.setParameters(Arrays.asList(shared, own));

        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("shared_param", new String[]{"s"});
        parameters.put("own_param", new String[]{"o"});

        BoolQuery globalConditions = new BoolQuery.Builder()
                .must(must -> must.term(t -> t.field("permissions.read").value("GROUP_EVERYONE")))
                .build();

        SearchRequest.Builder builder = new SearchRequest.Builder().index("workspace");
        Map<String, Aggregation> result = MetadataElasticSearchHelper.applyAggregations(builder, mds, query,
                parameters,
                Arrays.asList(new SearchFacet("own_param", null), new SearchFacet("other_facet", null)),
                Collections.singleton(own),
                globalConditions._toQuery(),
                token);

        String sharedClause = encoded("{\"match\":{\"shared_field\":\"s\"}}");
        String ownClause = encoded("{\"match\":{\"own_field\":\"o\"}}");
        String baseQuery = encoded(basequery);

        // the facet independent part is evaluated exactly once, as the top level query
        String topLevel = JsonpUtils.toJsonString(builder.build().query(), new JacksonJsonpMapper());
        assertTrue(topLevel.contains(baseQuery), "base query belongs into the top level query, got: " + topLevel);
        assertTrue(topLevel.contains(sharedClause), "shared criterion belongs into the top level query, got: " + topLevel);
        assertTrue(topLevel.contains("permissions.read"), "globalConditions belong into the top level query, got: " + topLevel);
        // the excludeOwn criterion must not be hoisted, it differs per facet
        assertFalse(topLevel.contains(ownClause), "excludeOwn criterion must stay per facet, got: " + topLevel);

        // the facet that owns the criterion drops it (exclude own) and has nothing else left
        String ownFacet = JsonpUtils.toJsonString(result.get("own_param"), new JacksonJsonpMapper());
        assertFalse(ownFacet.contains(ownClause), "own_param must not filter by its own criterion, got: " + ownFacet);
        assertTrue(ownFacet.contains("match_all"), "no difference left, expected a match_all filter, got: " + ownFacet);

        // every other facet still applies it
        String otherFacet = JsonpUtils.toJsonString(result.get("other_facet"), new JacksonJsonpMapper());
        assertTrue(otherFacet.contains(ownClause), "other_facet must filter by own_param, got: " + otherFacet);

        // and neither of them repeats the shared part
        for (Map.Entry<String, Aggregation> agg : result.entrySet()) {
            String json = JsonpUtils.toJsonString(agg.getValue(), new JacksonJsonpMapper());
            assertFalse(json.contains(sharedClause), "facet " + agg.getKey() + " duplicates the shared criterion: " + json);
            assertFalse(json.contains("permissions.read"), "facet " + agg.getKey() + " duplicates globalConditions: " + json);
        }
    }

    @Test
    void getAggregationsDoesNotHoistWhenQueryIsOrJoined() {
        SearchToken token = new SearchToken();
        // tree(A + B) != tree(A) AND tree(B) for a "should" joined query, so the split must not happen
        query.setJoin("OR");
        MetadataQueryParameter shared = multiParameter("shared_param", MetadataQueryParameter.ParameterJoinStrategy.AND, "shared_field");
        query.setParameters(Collections.singletonList(shared));

        BoolQuery globalConditions = new BoolQuery.Builder()
                .must(must -> must.term(t -> t.field("permissions.read").value("GROUP_EVERYONE")))
                .build();

        SearchRequest.Builder builder = new SearchRequest.Builder().index("workspace");
        Map<String, Aggregation> result = MetadataElasticSearchHelper.applyAggregations(builder, mds, query,
                new HashMap<>() {{
                    put("shared_param", new String[]{"s"});
                }},
                Collections.singletonList(new SearchFacet("shared_param", null)), Collections.emptySet(),
                globalConditions._toQuery(), token);

        // only globalConditions are hoisted, the matching tree stays in the facet filter
        assertEquals(
                JsonpUtils.toJsonString(globalConditions._toQuery(), new JacksonJsonpMapper()),
                JsonpUtils.toJsonString(builder.build().query(), new JacksonJsonpMapper()));
        String facet = JsonpUtils.toJsonString(result.get("shared_param"), new JacksonJsonpMapper());
        assertTrue(facet.contains(encoded(basequery)), "expected the complete tree per facet, got: " + facet);
        assertTrue(facet.contains(encoded("{\"match\":{\"shared_field\":\"s\"}}")), "expected the criterion per facet, got: " + facet);
    }

    @Test
    void getAggregationsKeepsCallerTopLevelQueryWhenGlobalConditionsAreNull() {
        SearchToken token = new SearchToken();
        MetadataQueryParameter shared = multiParameter("shared_param", MetadataQueryParameter.ParameterJoinStrategy.AND, "shared_field");
        query.setParameters(Collections.singletonList(shared));

        SearchRequest.Builder builder = new SearchRequest.Builder().index("workspace");
        Map<String, Aggregation> result = MetadataElasticSearchHelper.applyAggregations(builder, mds, query,
                new HashMap<>() {{
                    put("shared_param", new String[]{"s"});
                }},
                Collections.singletonList(new SearchFacet("shared_param", null)), Collections.emptySet(),
                null, token);

        assertNull(builder.build().query(),
                "callers passing null set their own top level query, applyAggregations must not touch it");
        // ... which is why the facet filter has to keep carrying the complete tree here
        String facet = JsonpUtils.toJsonString(result.get("shared_param"), new JacksonJsonpMapper());
        assertTrue(facet.contains(encoded(basequery)), "expected the complete tree per facet, got: " + facet);
        assertTrue(facet.contains(encoded("{\"match\":{\"shared_field\":\"s\"}}")), "expected the criterion per facet, got: " + facet);
    }

    @Test
    void getAggregationsCombineWithSuggestions() {
        SearchToken token = new SearchToken();
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax(), null);
        parameter.setName("test_facet");
        parameter.setFacet(new MetadataQueryParameter.MetadataQueryFacet(
                MetadataQueryParameter.MetadataQueryFacet.Type.term,
                MetadataQueryParameter.MetadataQueryFacet.SortBy.count,
                MetadataQueryParameter.MetadataQueryFacet.SortOrder.asc,
                null,
                null,
                true,
                Collections.emptyList())
        );
        query.setParameters(Collections.singletonList(parameter));

        Map<String, Aggregation> result = MetadataElasticSearchHelper.applyAggregations(
                new SearchRequest.Builder(), mds, query, Collections.emptyMap(),
                Collections.singletonList(new SearchFacet("test_facet", null)), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());

        String json = JsonpUtils.toJsonString(result.get("test_facet"), new JacksonJsonpMapper());
        // when combineWithSuggestions is set the terms agg must be driven by the painless script,
        // not by a plain keyword field
        assertTrue(json.contains("\"lang\":\"painless\""), "expected painless script-based aggregation, got: " + json);
        assertTrue(json.contains("\"property\":\"test_facet\""), "expected property param wired to facet name, got: " + json);
        // suggestions must be scoped to the current user via the authority param
        assertTrue(json.contains("\"authority\":\"user\""), "expected current-user authority param, got: " + json);
        assertFalse(json.contains("\"field\":\"properties.test_facet.keyword\""), "expected no field-based terms agg, got: " + json);
    }


    @Test
    void getAggregationsSearchToken() {
        SearchToken token = new SearchToken();
        token.setQueryString("A B C");
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax(), null);
        parameter.setName("test_facet");
        MetadataWidget widget = new MetadataWidget();
        widget.setId("test_facet");
        mds.setWidgets(Collections.singletonList(widget));

        query.setParameters(Collections.singletonList(parameter));

        Map<String, Aggregation> result = MetadataElasticSearchHelper.applyAggregations(new SearchRequest.Builder(), mds, query, Collections.emptyMap(),
                Collections.singletonList(new SearchFacet("test_facet", null)), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"properties.test_facet\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}},{\"wildcard\":{\"properties.test_facet.keyword\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}]}}" +
                        "}",
                result.get("test_facet")
        );

        // with param
        query.setParameters(Collections.singletonList(parameter));
        result = MetadataElasticSearchHelper.applyAggregations(new SearchRequest.Builder(), mds, query, new HashMap<>() {{
                    put("test_facet", new String[]{"a"});
                }},
                Collections.singletonList(new SearchFacet("test_facet", null)), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(2, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"properties.test_facet\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}},{\"wildcard\":{\"properties.test_facet.keyword\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}]}}" +
                        "}",
                result.get("test_facet")
        );
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"include\":[\"a\"],\"min_doc_count\":1,\"size\":1}}}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"properties.test_facet\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}},{\"wildcard\":{\"properties.test_facet.keyword\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}]}}" +
                        "}",
                result.get("test_facet_selected")
        );

        // multi term facet
        parameter.setFacet(new MetadataQueryParameter.MetadataQueryFacet(
                MetadataQueryParameter.MetadataQueryFacet.Type.term,
                MetadataQueryParameter.MetadataQueryFacet.SortBy.count,
                MetadataQueryParameter.MetadataQueryFacet.SortOrder.asc,
                null,
                null,
                false,
                Arrays.asList(
                        new MetadataQueryParameter.MetadataQueryFacetItem("facet1", null),
                        new MetadataQueryParameter.MetadataQueryFacetItem("facet2", null)
                ))
        );
        query.setParameters(Collections.singletonList(parameter));

        result = MetadataElasticSearchHelper.applyAggregations(new SearchRequest.Builder(), mds, query, Collections.emptyMap(),
                Collections.singletonList(new SearchFacet("test_facet", null)), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{\"multi_terms\":{\"min_doc_count\":4,\"size\":250,\"terms\":[{\"field\":\"facet1\",\"missing\":\"\"},{\"field\":\"facet2\",\"missing\":\"\"}]}}}," +
                        "\"meta\":{\"type\":\"multi_terms\"}," +
                        "\"filter\":{\"bool\":{\"must\":[{\"match_all\":{}},{\"bool\":{\"should\":[{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"facet1\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}},{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"facet2\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}]}}]}}" +
                        "}",
                result.get("test_facet")
        );

        // nested facet
        parameter.setFacet(new MetadataQueryParameter.MetadataQueryFacet(
                MetadataQueryParameter.MetadataQueryFacet.Type.term,
                MetadataQueryParameter.MetadataQueryFacet.SortBy.count,
                MetadataQueryParameter.MetadataQueryFacet.SortOrder.asc,
                null,
                null,
                false,
                Arrays.asList(
                        new MetadataQueryParameter.MetadataQueryFacetItem("contributor.displayname.keyword", "contributor")
                ))
        );
        query.setParameters(Collections.singletonList(parameter));

        result = MetadataElasticSearchHelper.applyAggregations(new SearchRequest.Builder(), mds, query, Collections.emptyMap(),
                Collections.singletonList(new SearchFacet("test_facet", null)), Collections.emptySet(),
                new BoolQuery.Builder().build()._toQuery(),
                token
        );
        assertEquals(1, result.size());
        SearchServiceElasticTestUtils.assertFacet(
                "{" +
                        "\"aggregations\":{\"test_facet\":{" +
                        "\"aggregations\":{\"test_facet_nested\":{\"terms\":{\"field\":\"contributor.displayname.keyword\",\"min_doc_count\":4,\"size\":250}}}," +
                        "\"nested\":{\"path\":\"contributor\"}}}," +
                        "\"filter\":{\"bool\":{\"must\":[" +
                        "{\"bool\":{\"must\":[{\"match_all\":{}}]}}," +
                        "{\"nested\":{\"path\":\"contributor\",\"query\":{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"wildcard\":{\"contributor.displayname.keyword\":{\"case_insensitive\":true,\"value\":\"*A B C*\"}}}]}}}}" +
                        "]}}" +
                        "}",
                result.get("test_facet")
        );


    }


}
