package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ScopeBaseQueryTest {

    private MockedStatic<AuthenticationUtil> authenticationUtil;
    private MockedStatic<AuthenticationToolAPI> authenticationToolApi;
    private MetadataQuery query;
    private MetadataQueries queries;

    @BeforeEach
    void beforeEach() {
        authenticationUtil = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtil.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("user");
        AuthenticationToolAPI api = Mockito.mock(AuthenticationToolAPI.class);
        when(api.getCurrentLocale()).thenReturn("en");
        authenticationToolApi = Mockito.mockStatic(AuthenticationToolAPI.class);
        authenticationToolApi.when(AuthenticationToolAPI::getInstance).thenReturn(api);

        query = new MetadataQuery();
        query.setId("ngsearch");
        query.setJoin("AND");
        query.setSyntax(MetadataReader.QUERY_SYNTAX_DSL);
        query.setBasequery(new HashMap<>() {{
            put(null, "{\"exists\":{\"field\":\"mds_base\"}}");
        }});
        query.setParameters(List.of(
                parameter("ccm:filter_param", "{\"term\":{\"filter_field\":\"x\"}}", true),
                parameter("ngsearchword", "{\"match\":{\"text_field\":\"x\"}}", false)));
        queries = new MetadataQueries();
        queries.setQueries(List.of(query));
    }

    @AfterEach
    void afterEach() {
        authenticationUtil.close();
        authenticationToolApi.close();
    }

    private MetadataQueryParameter parameter(String name, String statement, boolean asFilter) {
        MetadataQueryParameter parameter = new MetadataQueryParameter(MetadataReader.QUERY_SYNTAX_DSL, null);
        parameter.setName(name);
        parameter.setAsFilter(asFilter);
        parameter.setStatements(new HashMap<>() {{
            put(null, statement);
        }});
        return parameter;
    }

    /** json of the query, wrapper queries decoded */
    private static String json(Query query) {
        String json = JsonpUtils.toJsonString(query, new JacksonJsonpMapper());
        Pattern wrapper = Pattern.compile("\"query\":\"([A-Za-z0-9+/=]{8,})\"");
        for (Matcher m = wrapper.matcher(json); m.find(); m = wrapper.matcher(json)) {
            StringBuilder sb = new StringBuilder();
            m.appendReplacement(sb, Matcher.quoteReplacement(new String(Base64.getDecoder().decode(m.group(1)), StandardCharsets.UTF_8)));
            m.appendTail(sb);
            json = sb.toString();
        }
        return json;
    }

    @Test
    void filterAndQueryParametersAndTheMdsBaseQueryApply() {
        Map<String, String[]> criteria = new HashMap<>();
        criteria.put("ccm:filter_param", new String[]{"a"});
        criteria.put("ngsearchword", new String[]{"b"});

        String json = json(SearchServiceElastic.toScopeBaseQuery(queries, query, criteria));

        assertTrue(json.contains("mds_base"), json);
        assertTrue(json.contains("filter_field"), json);
        assertTrue(json.contains("text_field"), json);
    }
}
