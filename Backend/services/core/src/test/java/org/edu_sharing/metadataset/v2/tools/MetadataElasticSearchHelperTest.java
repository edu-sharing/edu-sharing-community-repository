package org.edu_sharing.metadataset.v2.tools;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.service.search.model.SearchToken;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MetadataElasticSearchHelperTest {

    private MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic;
    private String basequery;
    private MetadataQuery query;
    private MetadataQueries queries;

    @BeforeEach
    void beforeEach() {
        authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");

        query = new MetadataQuery();
        basequery = "{\"exists\":{\"field\": \"type\"}";
        query.setBasequery(new HashMap<>() {{
            put(null, basequery);
        }});
        query.setJoin("AND");
        query.setSyntax(MetadataReader.QUERY_SYNTAX_DSL);
        queries = new MetadataQueries();
        queries.setQueries(Collections.singletonList(query));
    }
    @AfterEach
    void afterEach() {
        authenticationUtilMockedStatic.close();
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
        MetadataQueryParameter parameter = new MetadataQueryParameter(query.getSyntax(), null);
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
        MetadataQueryParameter parameter2 = new MetadataQueryParameter(query.getSyntax(), null);
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
}