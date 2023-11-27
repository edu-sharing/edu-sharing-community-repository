package org.edu_sharing.metadataset.v2.tools;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.service.search.model.SearchToken;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

    @BeforeEach
    void beforeEach() {
        authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");
        authenticationToolApi = Mockito.mock(AuthenticationToolAPI.class);
        authenticationToolApiConstruction = Mockito.mockConstruction(AuthenticationToolAPI.class);
        when(authenticationToolApi.getCurrentLocale()).thenReturn("en");

        query = new MetadataQuery();
        basequery = "{\"exists\":{\"field\": \"type\"}";
        query.setBasequery(new HashMap<>() {{
            put(null, basequery);
        }});
        query.setJoin("AND");
        query.setSyntax(MetadataReader.QUERY_SYNTAX_DSL);
        queries = new MetadataQueries();
        queries.setQueries(Collections.singletonList(query));
        mds = new MetadataSet();
        mds.setQueries(new HashMap<>(){{
            put(MetadataReader.QUERY_SYNTAX_DSL, queries);
        }});
    }
    @AfterEach
    void afterEach() {
        authenticationUtilMockedStatic.close();
        authenticationToolApiConstruction.close();
    }

    @Test
    void getElasticSearchQueryBasic() {
        SearchToken token = new SearchToken();
        QueryBuilder result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, Collections.emptyMap());
        assertEquals("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"wrapper\" : {\n" +
                "          \"query\" : \""+ Base64.getEncoder().encodeToString(basequery.getBytes()) + "\"\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", result.toString());
    }

    @Test
    void getElasticSearchQueryMultipleParameter() {
        SearchToken token = new SearchToken();
        List<MetadataQueryParameter> parameters = new ArrayList<>();
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax(), mds);
        parameter.setMultiple(true);
        parameter.setMultiplejoin("AND");
        parameter.setName("parameter");
        parameter.setStatements(new HashMap<>() {{
            put(null, "{\"match\":{\"some_field\":\"{$value}\"");
        }});
        parameters.add(parameter);
        query.setParameters(parameters);
        QueryBuilder result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
        }});
        assertEquals("{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"must\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0i\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0i\"\n              }\n            }\n          ],\n          \"adjust_pure_negative\" : true,\n          \"boost\" : 1.0\n        }\n      }\n    ],\n    \"adjust_pure_negative\" : true,\n    \"boost\" : 1.0\n  }\n}", result.toString());

        // OR JOIN
        parameter.setMultiplejoin("OR");
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
        }});
        assertEquals("{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0i\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0i\"\n              }\n            }\n          ],\n          \"adjust_pure_negative\" : true,\n          \"boost\" : 1.0\n        }\n      }\n    ],\n    \"adjust_pure_negative\" : true,\n    \"boost\" : 1.0\n  }\n}", result.toString());


        // 2 Parameters AND combined
        MetadataQueryParameter parameter2 = new MetadataQueryParameter(query.getSyntax(), mds);
        parameter2.setMultiple(true);
        parameter2.setMultiplejoin("AND");
        parameter2.setName("parameter2");
        parameter2.setMultiplejoin("OR");
        parameters.add(parameter2);
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
            put("parameter2", new String[]{"a", "b"});
        }});
        assertEquals("{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9\"\n        }\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0i\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0i\"\n              }\n            }\n          ],\n          \"adjust_pure_negative\" : true,\n          \"boost\" : 1.0\n        }\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IiphKiJ9fX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IipiKiJ9fX0=\"\n              }\n            }\n          ],\n          \"adjust_pure_negative\" : true,\n          \"boost\" : 1.0\n        }\n      }\n    ],\n    \"adjust_pure_negative\" : true,\n    \"boost\" : 1.0\n  }\n}", result.toString());
        // 2 Parameters or combined
        query.setJoin("OR");
        result = MetadataElasticSearchHelper.getElasticSearchQuery(token, queries, query, new HashMap<>() {{
            put("parameter", new String[]{"a", "b"});
            put("parameter2", new String[]{"a", "b"});
        }});
        assertEquals("{\n  \"bool\" : {\n    \"must\" : [\n      {\n        \"wrapper\" : {\n          \"query\" : \"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9\"\n        }\n      }\n    ],\n    \"should\" : [\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0i\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJtYXRjaCI6eyJzb21lX2ZpZWxkIjoieyR2YWx1ZX0i\"\n              }\n            }\n          ],\n          \"adjust_pure_negative\" : true,\n          \"boost\" : 1.0\n        }\n      },\n      {\n        \"bool\" : {\n          \"should\" : [\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IiphKiJ9fX0=\"\n              }\n            },\n            {\n              \"wrapper\" : {\n                \"query\" : \"eyJ3aWxkY2FyZCI6eyJwcm9wZXJ0aWVzLnBhcmFtZXRlcjIua2V5d29yZCI6eyJjYXNlX2luc2Vuc2l0aXZlIjp0cnVlLCJ2YWx1ZSI6IipiKiJ9fX0=\"\n              }\n            }\n          ],\n          \"adjust_pure_negative\" : true,\n          \"boost\" : 1.0\n        }\n      }\n    ],\n    \"adjust_pure_negative\" : true,\n    \"boost\" : 1.0\n  }\n}", result.toString());
    }

    @Test
    void getAggregations() {
        SearchToken token = new SearchToken();
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax(), mds);
        parameter.setName("test_facet");

        query.setParameters(Collections.singletonList(parameter));

        List<AggregationBuilder> result = MetadataElasticSearchHelper.getAggregations(mds, query, Collections.emptyMap(),
                Collections.singletonList("test_facet"), Collections.emptySet(),
                new BoolQueryBuilder(),
                token
        );
        assertEquals(1, result.size());
        assertEquals("{\"test_facet\":{\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9\"}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"size\":250,\"min_doc_count\":4,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]}}}}}", result.get(0).toString());

        // 2 facets
        MetadataQueryParameter parameter2 = new MetadataQueryParameter(query.getSyntax(), mds);
        parameter2.setName("test_facet2");
        query.setParameters(Arrays.asList(parameter, parameter2));

        result = MetadataElasticSearchHelper.getAggregations(mds, query, Collections.emptyMap(),
                Arrays.asList("test_facet", "test_facet2"), Collections.emptySet(),
                new BoolQueryBuilder(),
                token
        );
        assertEquals(2, result.size());
        assertEquals("{\"test_facet\":{\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9\"}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"aggregations\":{\"test_facet\":{\"terms\":{\"field\":\"properties.test_facet.keyword\",\"size\":250,\"min_doc_count\":4,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]}}}}}", result.get(0).toString());
        assertEquals("{\"test_facet2\":{\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9\"}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"aggregations\":{\"test_facet2\":{\"terms\":{\"field\":\"properties.test_facet2.keyword\",\"size\":250,\"min_doc_count\":4,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]}}}}}", result.get(1).toString());

        // multi term facet
        parameter.setFacets(Arrays.asList("facet1", "facet2"));
        query.setParameters(Collections.singletonList(parameter));

        result = MetadataElasticSearchHelper.getAggregations(mds, query, Collections.emptyMap(),
                Collections.singletonList("test_facet"), Collections.emptySet(),
                new BoolQueryBuilder(),
                token
        );
        assertEquals(1, result.size());
        assertEquals("{\"test_facet\":{\"meta\":{\"type\":\"multi_terms\"},\"filter\":{\"bool\":{\"must\":[{\"bool\":{\"must\":[{\"wrapper\":{\"query\":\"eyJleGlzdHMiOnsiZmllbGQiOiAidHlwZSJ9\"}}],\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}},{\"bool\":{\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"aggregations\":{\"test_facet\":{\"multi_terms\":{\"min_doc_count\":4,\"size\":250,\"terms\":[{\"field\":\"facet1\",\"missing\":\"\"},{\"field\":\"facet2\",\"missing\":\"\"}]}}}}}", result.get(0).toString());
    }
}