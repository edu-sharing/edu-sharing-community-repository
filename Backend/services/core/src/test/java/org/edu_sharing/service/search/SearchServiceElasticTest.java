package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonpDeserializer;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.impl.model.PermissionModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataElasticSearchHelper;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.cache.UserCache;
import org.edu_sharing.restservices.search.v1.model.SearchFacet;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.elasticsearch.client.RestClient;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceElasticTest {

    @Mock
    private PermissionModel permissionsModelDAO;
    @Mock
    private GuestService guestService;
    @Mock
    private PermissionService eduPermissionService;
    @Mock
    private org.alfresco.service.cmr.search.SearchService searchService;
    @Mock
    private RetryingTransactionHelper retryingTransactionHelper;
    @Mock
    private AuthorityService authorityService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private ToolPermissionService toolPermissionService;
    @Mock
    private NodeService nodeService;
    @Mock
    private AuthenticationToolAPI authTool;
    @Mock
    private Repository repositoryHelper;
    @Mock
    private UserCache cache;

    @Mock
    private org.edu_sharing.service.authority.AuthorityService eduAuthorityService;

    private SearchServiceElastic underTest;
    private MockedConstruction<MCAlfrescoAPIClient> mcAlfrescoApiClientMockedStatic;
    private MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic;
    /** static mocks / clients which are only set up by the facet tests */
    private final List<AutoCloseable> facetTestResources = new ArrayList<>();

    @BeforeEach()
    void beforeEach() {
        mcAlfrescoApiClientMockedStatic = Mockito.mockConstruction(MCAlfrescoAPIClient.class);
        authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class);



        underTest = new SearchServiceElastic(
                permissionsModelDAO,
                guestService,
                eduPermissionService,
                eduAuthorityService,
                searchService,
                retryingTransactionHelper,
                authorityService,
                authenticationService,
                toolPermissionService,
                nodeService,
                authTool,
                repositoryHelper,
                cache);
    }
//
    @AfterEach()
    void afterEach() throws Exception {
        mcAlfrescoApiClientMockedStatic.close();
        authenticationUtilMockedStatic.close();
        for (AutoCloseable resource : facetTestResources) {
            resource.close();
        }
    }


    @Test
    void getGlobalConditions() {

        when(authenticationService.getCurrentUserName()).thenReturn("tester");
        when(authorityService.getAuthorities()).thenReturn(new HashSet<>(Set.of("test_group1", "test_group2")));
        authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("tester");


        BoolQuery.Builder conditions = underTest.getGlobalConditions(Collections.singletonList("scope"), Collections.singletonList("read"), new MetadataQuery());

        SearchServiceElasticTestUtils.assertQuery(
                //language=JSON
                """
                        {
                          "bool": {
                            "must": [
                              {
                                "bool": {
                                  "must": [
                                    {
                                      "bool": {
                                        "minimum_should_match": "1",
                                        "should": [
                                          {
                                            "match": {
                                              "permissions.read": {
                                                "query": "scope"
                                              }
                                            }
                                          }
                                        ]
                                      }
                                    },
                                    {
                                      "match": {
                                        "nodeRef.storeRef.protocol": {
                                          "query": "workspace"
                                        }
                                      }
                                    }
                                  ]
                                }
                              }
                            ],
                            "must_not": [
                              {
                                "exists": {
                                  "field": "properties.ccm:eduscopename"
                                }
                              }
                            ],
                            "should": [
                              {
                                "match": {
                                  "owner": {
                                    "query": "tester"
                                  }
                                }
                              },
                              {
                                "bool": {
                                  "must": [
                                    {
                                      "bool": {
                                        "minimum_should_match": "1",
                                        "should": [
                                          {
                                            "match": {
                                              "permissions.read": {
                                                "query": "test_group1"
                                              }
                                            }
                                          },
                                          {
                                            "match": {
                                              "permissions.read": {
                                                "query": "GROUP_EVERYONE"
                                              }
                                            }
                                          },
                                          {
                                            "match": {
                                              "permissions.read": {
                                                "query": "tester"
                                              }
                                            }
                                          },
                                          {
                                            "match": {
                                              "permissions.read": {
                                                "query": "test_group2"
                                              }
                                            }
                                          }
                                        ]
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }""",
                conditions
        );
    }

    @Test
    void getAuthorityCombinedQuery() {

        Assertions.assertEquals("{\"bool\":{}}", SearchServiceElastic.getAuthorityCombinedQuery(AuthorityType.USER, null, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Map<String, String> groupType = new HashMap<>() {{
            put("groupType", "TEST");
        }};

        Map<String, String> personStatus = new HashMap<>() {{
            put("cm:espersonstatus", "TEST");
        }};
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"wildcard\":{\"properties.cm:espersonstatus.keyword\":{\"value\":\"TEST\"}}}]}}", SearchServiceElastic.getAuthorityCombinedQuery(AuthorityType.USER, personStatus, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{}}", SearchServiceElastic.getAuthorityCombinedQuery(AuthorityType.USER, groupType, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"wildcard\":{\"properties.groupType.keyword\":{\"value\":\"TEST\"}}}]}}", SearchServiceElastic.getAuthorityCombinedQuery(AuthorityType.GROUP, groupType, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"wildcard\":{\"properties.groupType.keyword\":{\"value\":\"TEST\"}}}]}}", SearchServiceElastic.getAuthorityCombinedQuery(AuthorityType.GROUP, groupType, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"bool\":{}},{\"bool\":{\"must\":[{\"wildcard\":{\"properties.groupType.keyword\":{\"value\":\"TEST\"}}}]}}]}}", SearchServiceElastic.getAuthorityCombinedQuery(null, groupType, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
    }

    @Test
    void getContentTypeQuery() {

        Assertions.assertEquals("{\"match_all\":{}}", underTest.getContentTypeQuery(SearchService.ContentType.ALL).toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"match\":{\"type\":{\"query\":\"ccm:io\"}}}]}}", underTest.getContentTypeQuery(SearchService.ContentType.FILES).toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"match\":{\"type\":{\"query\":\"ccm:map\"}}}],\"must_not\":[{\"match\":{\"aspects\":{\"query\":\"ccm:collection\"}}}]}}", underTest.getContentTypeQuery(SearchService.ContentType.FOLDERS).toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"match\":{\"type\":{\"query\":\"ccm:map\"}}},{\"match\":{\"aspects\":{\"query\":\"ccm:collection\"}}}]}}", underTest.getContentTypeQuery(SearchService.ContentType.COLLECTIONS).toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"match\":{\"type\":{\"query\":\"ccm:collection_proposal\"}}}]}}", underTest.getContentTypeQuery(SearchService.ContentType.COLLECTION_PROPOSALS).toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"match\":{\"type\":{\"query\":\"ccm:toolpermission\"}}}]}}", underTest.getContentTypeQuery(SearchService.ContentType.TOOLPERMISSIONS).toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"bool\":{\"must\":[{\"match\":{\"type\":{\"query\":\"ccm:map\"}}}],\"must_not\":[{\"match\":{\"aspects\":{\"query\":\"ccm:collection\"}}}]}},{\"match\":{\"type\":{\"query\":\"ccm:io\"}}}]}}", underTest.getContentTypeQuery(SearchService.ContentType.FILES_AND_FOLDERS).toString().substring("Query: ".length()));
    }

    // --- facets: request building (embedded vs. separate excludeOwn request) and response mapping ---

    private static final String QUERY_ID = "ngsearch";
    /** joined via AND: never excludes its own criterion, the aggregations stay part of the main request */
    private static final String AND_FACET = "ccm:and_facet";
    /** joined via OR: excludes its own criterion, which requires a separate aggregations request */
    private static final String OR_FACET = "ccm:or_facet";
    private static final JsonpMapper MAPPER = new JacksonJsonpMapper();

    private MetadataSet facetMds;

    private static MetadataQueryParameter facetParameter(String name, MetadataQueryParameter.ParameterJoinStrategy join) {
        MetadataQueryParameter parameter = new MetadataQueryParameter(MetadataReader.QUERY_SYNTAX_DSL, null);
        parameter.setName(name);
        parameter.setMultiple(true);
        parameter.setMultiplejoin(join);
        parameter.setStatements(new HashMap<>() {{
            put(null, "{\"term\":{\"properties." + name + ".keyword\":\"${value}\"}}");
        }});
        return parameter;
    }

    private static MetadataQueryParameter facetParameter(String name, MetadataQueryParameter.MetadataQueryFacet facet) {
        MetadataQueryParameter parameter = facetParameter(name, MetadataQueryParameter.ParameterJoinStrategy.AND);
        parameter.setFacet(facet);
        return parameter;
    }

    private static MetadataQueryParameter.MetadataQueryFacet facetDefinition(MetadataQueryParameter.MetadataQueryFacet.SortBy sortBy,
                                                                             MetadataQueryParameter.MetadataQueryFacet.SortOrder sortOrder,
                                                                             Integer maxBucketSize) {
        return new MetadataQueryParameter.MetadataQueryFacet(MetadataQueryParameter.MetadataQueryFacet.Type.term,
                sortBy, sortOrder, maxBucketSize, null, false, Collections.emptyList());
    }

    private static MetadataWidget widget(String id, String... keyCaptionPairs) {
        MetadataWidget widget = new MetadataWidget();
        widget.setId(id);
        List<MetadataKey> keys = new ArrayList<>();
        for (int i = 0; i < keyCaptionPairs.length; i += 2) {
            MetadataKey key = new MetadataKey();
            key.setKey(keyCaptionPairs[i]);
            key.setCaption(keyCaptionPairs[i + 1]);
            keys.add(key);
        }
        widget.setValues(keys);
        return widget;
    }

    /**
     * creates {@link #facetMds} with the query {@link #QUERY_ID} which contains the given parameters
     * and a widget for each of them (the facet sorting requires a widget per facet)
     */
    private MetadataQuery facetQuery(MetadataQueryParameter... parameters) {
        MetadataQuery query = new MetadataQuery();
        query.setId(QUERY_ID);
        query.setJoin("AND");
        query.setSyntax(MetadataReader.QUERY_SYNTAX_DSL);
        query.setBasequery(new HashMap<>() {{
            put(null, "{\"match_all\":{}}");
        }});
        query.setParameters(new ArrayList<>(Arrays.asList(parameters)));
        MetadataQueries queries = new MetadataQueries();
        queries.setQueries(Collections.singletonList(query));
        facetMds = new MetadataSet();
        facetMds.setQueries(new HashMap<>() {{
            put(MetadataReader.QUERY_SYNTAX_DSL, queries);
        }});
        facetMds.setWidgets(Arrays.stream(parameters).map(p -> widget(p.getName())).collect(Collectors.toCollection(ArrayList::new)));
        return query;
    }

    private static SearchToken facetToken(String... facets) {
        SearchToken token = new SearchToken();
        // an explicit scope keeps the global conditions independent of the current user
        token.setAuthorityScope(Collections.singletonList("scope"));
        token.setMaxResult(10);
        token.setFacets(Arrays.stream(facets).map(f -> new SearchFacet(f, null)).collect(Collectors.toList()));
        return token;
    }

    /** applyAggregations resolves the current locale */
    private void mockAuthenticationTool() {
        MockedStatic<AuthenticationToolAPI> authenticationToolApiMockedStatic = Mockito.mockStatic(AuthenticationToolAPI.class);
        authenticationToolApiMockedStatic.when(AuthenticationToolAPI::getInstance).thenReturn(authTool);
        facetTestResources.add(authenticationToolApiMockedStatic);
        lenient().when(authTool.getCurrentLocale()).thenReturn("en");
    }

    /**
     * replaces the static elastic client. The aggregations request (excludeOwn facets) is identified by its size 0,
     * every other request gets the main response
     */
    private ElasticsearchClient mockClient(SearchResponse<Map> mainResponse, SearchResponse<Map> aggregationsResponse) throws Exception {
        ElasticsearchClient previousClient = SearchServiceElastic.client;
        RestClient previousRestClient = SearchServiceElastic.restClient;
        facetTestResources.add(() -> {
            SearchServiceElastic.client = previousClient;
            SearchServiceElastic.restClient = previousRestClient;
        });
        RestClient restClient = mock(RestClient.class);
        when(restClient.isRunning()).thenReturn(true);
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.ping()).thenReturn(new BooleanResponse(true));
        doAnswer(invocation -> Integer.valueOf(0).equals(invocation.<SearchRequest>getArgument(0).size()) ? aggregationsResponse : mainResponse)
                .when(client).search(any(SearchRequest.class), eq(Map.class));
        SearchServiceElastic.client = client;
        SearchServiceElastic.restClient = restClient;

        MockedStatic<AuthorityServiceHelper> authorityServiceHelperMockedStatic = Mockito.mockStatic(AuthorityServiceHelper.class);
        authorityServiceHelperMockedStatic.when(AuthorityServiceHelper::isAdmin).thenReturn(false);
        facetTestResources.add(authorityServiceHelperMockedStatic);
        return client;
    }

    /** search response without hits, aggregations are given with typed keys (e.g. "filter#name") like elastic returns them */
    private static SearchResponse<Map> response(String... aggregations) {
        String json = "{\"took\":1,\"timed_out\":false,\"_shards\":{\"total\":1,\"successful\":1,\"failed\":0}," +
                "\"hits\":{\"total\":{\"value\":0,\"relation\":\"eq\"},\"hits\":[]}," +
                "\"aggregations\":{" + String.join(",", aggregations) + "}}";
        JsonpDeserializer<Map> documentDeserializer = JsonpDeserializer.of(Map.class);
        return SearchResponse.createSearchResponseDeserializer(documentDeserializer)
                .deserialize(MAPPER.jsonProvider().createParser(new StringReader(json)), MAPPER);
    }

    private static String filterAggregate(String name, String... subAggregations) {
        return "\"filter#" + name + "\":{\"doc_count\":10" +
                Arrays.stream(subAggregations).map(sub -> "," + sub).collect(Collectors.joining()) + "}";
    }

    /** @param keyCounts alternating bucket key (String) and doc count (Integer) */
    private static String stermsAggregate(String name, long sumOtherDocCount, Object... keyCounts) {
        List<String> buckets = new ArrayList<>();
        for (int i = 0; i < keyCounts.length; i += 2) {
            buckets.add("{\"key\":\"" + keyCounts[i] + "\",\"doc_count\":" + keyCounts[i + 1] + "}");
        }
        return "\"sterms#" + name + "\":{\"doc_count_error_upper_bound\":0,\"sum_other_doc_count\":" + sumOtherDocCount +
                ",\"buckets\":[" + String.join(",", buckets) + "]}";
    }

    /** base64 of a statement, the form {@link ReadableWrapperQueryBuilder} puts into a wrapper query */
    private static String encoded(String statement) {
        return Base64.getEncoder().encodeToString(statement.getBytes());
    }

    private static String termStatement(String property, String value) {
        return encoded("{\"term\":{\"properties." + property + ".keyword\":\"" + value + "\"}}");
    }

    private static String json(Object value) {
        if (value instanceof SearchRequest) {
            return JsonpUtils.toJsonString(value, MAPPER);
        }
        if (value instanceof co.elastic.clients.elasticsearch._types.aggregations.Aggregation) {
            return JsonpUtils.toJsonString(value, MAPPER);
        }
        return JsonpUtils.toJsonString(value, MAPPER);
    }

    private static Map<String, List<NodeSearch.Facet>> facetsByProperty(SearchResultNodeRef result) {
        return result.getFacets().stream().collect(Collectors.groupingBy(NodeSearch.Facet::getProperty, LinkedHashMap::new, Collectors.toList()));
    }

    private static List<String> values(NodeSearch.Facet facet) {
        return facet.getValues().stream().map(v -> v.getValue() + "=" + v.getCount()).collect(Collectors.toList());
    }

    @Test
    void buildSearchRequestsWithoutFacets() {
        MetadataQuery query = facetQuery(facetParameter(AND_FACET, MetadataQueryParameter.ParameterJoinStrategy.AND));
        SearchToken token = facetToken();
        token.setFacets(null);
        Map<String, String[]> criteria = Map.of(AND_FACET, new String[]{"a"});

        SearchServiceElastic.ElasticSearchRequests requests = underTest.buildSearchRequests(facetMds, query, criteria, token, true);

        assertNull(requests.getAggregationsRequest());
        assertNull(requests.getAggregations());
        assertNull(requests.getExcludedOwnAggregations());
        SearchRequest searchRequest = requests.getSearchRequest();
        assertTrue(searchRequest.aggregations().isEmpty());
        assertEquals(List.of(SearchServiceElastic.WORKSPACE_INDEX), searchRequest.index());
        assertEquals(0, searchRequest.from());
        assertEquals(10, searchRequest.size());
        assertTrue(searchRequest.trackTotalHits().enabled());
        assertTrue(searchRequest.source().filter().excludes().contains("content.fulltext"));
        String json = json(searchRequest);
        assertTrue(json.contains(termStatement(AND_FACET, "a")), json);
        assertTrue(json.contains("permissions.read"), json);
        assertTrue(json.contains("properties.ccm:eduscopename"), json);

        // the debugger may leave out the global conditions, the criteria stay untouched
        String withoutGlobalConditions = json(underTest.buildSearchRequests(facetMds, query, criteria, token, false).getSearchRequest());
        assertTrue(withoutGlobalConditions.contains(termStatement(AND_FACET, "a")), withoutGlobalConditions);
        assertFalse(withoutGlobalConditions.contains("permissions.read"), withoutGlobalConditions);
        assertFalse(withoutGlobalConditions.contains("properties.ccm:eduscopename"), withoutGlobalConditions);
    }

    @Test
    void buildSearchRequestsEmbedsFacetsWithoutExcludeOwn() {
        mockAuthenticationTool();
        MetadataQuery query = facetQuery(facetParameter(AND_FACET, MetadataQueryParameter.ParameterJoinStrategy.AND));
        SearchToken token = facetToken(AND_FACET);
        Map<String, String[]> criteria = Map.of(AND_FACET, new String[]{"a"});

        SearchServiceElastic.ElasticSearchRequests requests = underTest.buildSearchRequests(facetMds, query, criteria, token, true);

        assertNull(requests.getAggregationsRequest());
        assertNull(requests.getExcludedOwnAggregations());
        // the embedded facets read the selected values from searchToken.getParameters() instead of the criteria.
        // These are only set via SearchToken.setMetadataQuery (no caller) -> no "_selected" aggregation in this mode
        assertEquals(Set.of(AND_FACET), requests.getAggregations().keySet());
        assertEquals(Set.of(AND_FACET), requests.getSearchRequest().aggregations().keySet());
        // criteria and global conditions are part of the top level query only (which also scopes the aggregations),
        // the facet filters must not repeat them
        String topLevel = json(requests.getSearchRequest().query());
        assertTrue(topLevel.contains("permissions.read"), topLevel);
        assertTrue(topLevel.contains(termStatement(AND_FACET, "a")), topLevel);
        String facet = json(requests.getAggregations().get(AND_FACET));
        assertFalse(facet.contains("permissions.read"), facet);
        assertFalse(facet.contains(termStatement(AND_FACET, "a")), facet);
    }

    @Test
    void buildSearchRequestsUsesSeparateRequestForExcludeOwnFacets() {
        mockAuthenticationTool();
        MetadataQuery query = facetQuery(
                facetParameter(AND_FACET, MetadataQueryParameter.ParameterJoinStrategy.AND),
                facetParameter(OR_FACET, MetadataQueryParameter.ParameterJoinStrategy.OR));
        SearchToken token = facetToken(OR_FACET, AND_FACET);
        Map<String, String[]> criteria = Map.of(AND_FACET, new String[]{"a"}, OR_FACET, new String[]{"o"});

        SearchServiceElastic.ElasticSearchRequests requests = underTest.buildSearchRequests(facetMds, query, criteria, token, true);

        // the main request does not carry any aggregations, all facets are part of the separate request
        assertNull(requests.getAggregations());
        assertTrue(requests.getSearchRequest().aggregations().isEmpty());
        SearchRequest aggregationsRequest = requests.getAggregationsRequest();
        assertNotNull(aggregationsRequest);
        assertEquals(List.of(SearchServiceElastic.WORKSPACE_INDEX), aggregationsRequest.index());
        assertEquals(0, aggregationsRequest.from());
        assertEquals(0, aggregationsRequest.size());

        // the "_selected" aggregations of the selected values are dropped (duplicate facet entries)
        assertEquals(Set.of(OR_FACET, AND_FACET), requests.getExcludedOwnAggregations().keySet());
        assertEquals(Set.of(OR_FACET, AND_FACET), aggregationsRequest.aggregations().keySet());

        // the shared part (AND criterion + global conditions) is hoisted into the top level query
        String topLevel = json(aggregationsRequest.query());
        assertTrue(topLevel.contains("permissions.read"), topLevel);
        assertTrue(topLevel.contains(termStatement(AND_FACET, "a")), topLevel);
        assertFalse(topLevel.contains(termStatement(OR_FACET, "o")), topLevel);
        // the OR facet ignores its own criterion, every other facet applies it
        String orFacet = json(aggregationsRequest.aggregations().get(OR_FACET));
        assertFalse(orFacet.contains(termStatement(OR_FACET, "o")), orFacet);
        String andFacet = json(aggregationsRequest.aggregations().get(AND_FACET));
        assertTrue(andFacet.contains(termStatement(OR_FACET, "o")), andFacet);

        // without global conditions nothing is hoisted, every facet carries the complete criteria tree
        SearchServiceElastic.ElasticSearchRequests debugRequests = underTest.buildSearchRequests(facetMds, query, criteria, token, false);
        assertNull(debugRequests.getAggregationsRequest().query());
        String debugJson = json(debugRequests.getAggregationsRequest()) + json(debugRequests.getSearchRequest());
        assertFalse(debugJson.contains("permissions.read"), debugJson);
        String debugAndFacet = json(debugRequests.getAggregationsRequest().aggregations().get(AND_FACET));
        assertTrue(debugAndFacet.contains(termStatement(AND_FACET, "a")), debugAndFacet);
        assertTrue(debugAndFacet.contains(termStatement(OR_FACET, "o")), debugAndFacet);
    }

    @Test
    void buildSearchRequestsDecidesExcludeOwnByJoinStrategy() {
        mockAuthenticationTool();
        MetadataQuery query = facetQuery(
                facetParameter(AND_FACET, MetadataQueryParameter.ParameterJoinStrategy.AND),
                facetParameter(OR_FACET, MetadataQueryParameter.ParameterJoinStrategy.OR));

        // an OR joined facet always requires the separate request, even if nothing is selected
        SearchServiceElastic.ElasticSearchRequests requests = underTest.buildSearchRequests(facetMds, query, Collections.emptyMap(), facetToken(OR_FACET), true);
        assertNotNull(requests.getAggregationsRequest());
        assertEquals(Set.of(OR_FACET), requests.getExcludedOwnAggregations().keySet());

        // facets which are not a parameter of the query are never excludeOwn
        requests = underTest.buildSearchRequests(facetMds, query, Map.of(OR_FACET, new String[]{"o"}), facetToken("ccm:unknown"), true);
        assertNull(requests.getAggregationsRequest());
        assertEquals(Set.of("ccm:unknown"), requests.getAggregations().keySet());
    }

    @Test
    void searchMapsEmbeddedFacetsOfTheMainResponse() throws Throwable {
        mockAuthenticationTool();
        facetQuery(facetParameter(AND_FACET, MetadataQueryParameter.ParameterJoinStrategy.AND));
        ElasticsearchClient client = mockClient(
                response(filterAggregate(AND_FACET, stermsAggregate(AND_FACET, 7, "a", 5, "b", 3))),
                null);

        SearchResultNodeRef result = underTest.search(facetMds, QUERY_ID, Map.of(AND_FACET, new String[]{"a"}), facetToken(AND_FACET));

        verify(client, times(1)).search(any(SearchRequest.class), eq(Map.class));
        assertEquals(1, result.getFacets().size());
        NodeSearch.Facet facet = result.getFacets().get(0);
        assertEquals(AND_FACET, facet.getProperty());
        assertEquals(List.of("a=5", "b=3"), values(facet));
        assertEquals(7L, facet.getSumOtherDocCount());
    }

    @Test
    void searchMapsExcludeOwnFacetsOfTheAggregationsResponse() throws Throwable {
        mockAuthenticationTool();
        facetQuery(facetParameter(OR_FACET, MetadataQueryParameter.ParameterJoinStrategy.OR));
        ElasticsearchClient client = mockClient(
                // aggregations of the main response must be ignored once the separate request was used
                response(filterAggregate(OR_FACET, stermsAggregate(OR_FACET, 0, "main", 1))),
                response(filterAggregate(OR_FACET, stermsAggregate(OR_FACET, 0, "o", 2, "p", 1))));

        SearchResultNodeRef result = underTest.search(facetMds, QUERY_ID, Map.of(OR_FACET, new String[]{"o"}), facetToken(OR_FACET));

        ArgumentCaptor<SearchRequest> requests = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client, times(2)).search(requests.capture(), eq(Map.class));
        // the facets are requested first, then the main request
        assertEquals(0, requests.getAllValues().get(0).size());
        assertEquals(10, requests.getAllValues().get(1).size());
        assertEquals(List.of(OR_FACET), result.getFacets().stream().map(NodeSearch.Facet::getProperty).collect(Collectors.toList()));
        assertEquals(List.of("o=2", "p=1"), values(result.getFacets().get(0)));
    }

    @Test
    void searchFacetMappingSpecialCases() throws Throwable {
        mockAuthenticationTool();
        MetadataQueryParameter.MetadataQueryFacet.SortBy count = MetadataQueryParameter.MetadataQueryFacet.SortBy.count;
        MetadataQueryParameter.MetadataQueryFacet.SortBy caption = MetadataQueryParameter.MetadataQueryFacet.SortBy.caption;
        MetadataQueryParameter.MetadataQueryFacet.SortOrder asc = MetadataQueryParameter.MetadataQueryFacet.SortOrder.asc;
        MetadataQueryParameter.MetadataQueryFacet.SortOrder desc = MetadataQueryParameter.MetadataQueryFacet.SortOrder.desc;
        facetQuery(
                facetParameter("ccm:plain", MetadataQueryParameter.ParameterJoinStrategy.AND),
                facetParameter("ccm:limited", facetDefinition(count, desc, 2)),
                facetParameter("ccm:caption", facetDefinition(caption, asc, null)),
                facetParameter("ccm:caption_desc", facetDefinition(caption, desc, null)),
                facetParameter("ccm:count_asc", facetDefinition(count, asc, null)),
                facetParameter("ccm:nested", MetadataQueryParameter.ParameterJoinStrategy.AND),
                facetParameter("ccm:multi", MetadataQueryParameter.ParameterJoinStrategy.AND),
                facetParameter("ccm:geo", MetadataQueryParameter.ParameterJoinStrategy.AND),
                facetParameter("ccm:combined", MetadataQueryParameter.ParameterJoinStrategy.AND),
                facetParameter("ccm:top", MetadataQueryParameter.ParameterJoinStrategy.AND));
        facetMds.getWidgets().removeIf(w -> w.getId().startsWith("ccm:caption"));
        facetMds.getWidgets().add(widget("ccm:caption", "a", "Beta", "b", "Alpha"));
        facetMds.getWidgets().add(widget("ccm:caption_desc", "a", "Beta", "b", "Alpha"));

        mockClient(response(
                filterAggregate("ccm:plain", stermsAggregate("ccm:plain", 7, "a", 5, "b", 3)),
                // the selected values are returned as an additional facet with the same property
                filterAggregate("ccm:plain" + MetadataElasticSearchHelper.FACET_SELECTED_POSTFIX, stermsAggregate("ccm:plain", 0, "z", 1)),
                // a fixed maxBucketSize hides the remaining doc count
                filterAggregate("ccm:limited", stermsAggregate("ccm:limited", 9, "a", 1)),
                filterAggregate("ccm:caption", stermsAggregate("ccm:caption", 0, "a", 5, "b", 3)),
                filterAggregate("ccm:caption_desc", stermsAggregate("ccm:caption_desc", 0, "b", 5, "a", 3)),
                filterAggregate("ccm:count_asc", stermsAggregate("ccm:count_asc", 0, "y", 4, "x", 1)),
                filterAggregate("ccm:nested", "\"nested#ccm:nested\":{\"doc_count\":3," + stermsAggregate("ccm:nested_nested", 2, "n", 3) + "}"),
                filterAggregate("ccm:multi", "\"multi_terms#ccm:multi\":{\"doc_count_error_upper_bound\":0,\"sum_other_doc_count\":4,\"buckets\":[" +
                        "{\"key\":[\"a\",\"b\"],\"key_as_string\":\"a|b\",\"doc_count\":3}," +
                        "{\"key\":[\"b\",\"c\"],\"key_as_string\":\"b|c\",\"doc_count\":2}]}"),
                filterAggregate("ccm:geo", "\"geotile_grid#ccm:geo\":{\"buckets\":[{\"key\":\"7/1/2\",\"doc_count\":4}]}"),
                filterAggregate("ccm:combined", "\"filter#ccm:combined\":{\"doc_count\":10,\"meta\":{" +
                        "\"" + MetadataElasticSearchHelper.COMBINED_SUGGESTION_META_KIND + "\":\"" + MetadataElasticSearchHelper.COMBINED_SUGGESTION_META_KIND_VALUE + "\"," +
                        "\"" + MetadataElasticSearchHelper.COMBINED_SUGGESTION_META_SIZE + "\":2," +
                        "\"" + MetadataElasticSearchHelper.COMBINED_SUGGESTION_META_MIN_DOC_COUNT + "\":1}," +
                        filterAggregate(MetadataElasticSearchHelper.COMBINED_SUGGESTION_BRANCH_PLAIN,
                                stermsAggregate(MetadataElasticSearchHelper.COMBINED_SUGGESTION_BRANCH_VALUES, 0, "a", 2, "b", 1)) + "," +
                        filterAggregate(MetadataElasticSearchHelper.COMBINED_SUGGESTION_BRANCH_WITH_SUGGESTIONS,
                                stermsAggregate(MetadataElasticSearchHelper.COMBINED_SUGGESTION_BRANCH_VALUES, 0, "a", 1, "s", 5)) + "}"),
                // terms aggregations on the top level are supported as well
                stermsAggregate("ccm:top", 0, "t", 1),
                // everything else is skipped
                "\"avg#ccm:ignored\":{\"value\":1.0}"
        ), null);

        Map<String, List<NodeSearch.Facet>> facets = facetsByProperty(
                underTest.search(facetMds, QUERY_ID, Collections.emptyMap(), facetToken("ccm:plain")));

        assertEquals(Set.of("ccm:plain", "ccm:limited", "ccm:caption", "ccm:caption_desc", "ccm:count_asc", "ccm:nested",
                "ccm:multi", "ccm:geo", "ccm:combined", "ccm:top"), facets.keySet());

        List<NodeSearch.Facet> plain = facets.get("ccm:plain");
        assertEquals(2, plain.size());
        NodeSearch.Facet plainFacet = plain.stream().filter(f -> f.getValues().size() == 2).findFirst().orElseThrow();
        assertEquals(List.of("a=5", "b=3"), values(plainFacet));
        assertEquals(7L, plainFacet.getSumOtherDocCount());
        NodeSearch.Facet selectedFacet = plain.stream().filter(f -> f.getValues().size() == 1).findFirst().orElseThrow();
        assertEquals(List.of("z=1"), values(selectedFacet));

        assertEquals(0L, facets.get("ccm:limited").get(0).getSumOtherDocCount());
        assertEquals(List.of("b=3", "a=5"), values(facets.get("ccm:caption").get(0)));
        assertEquals(List.of("a=3", "b=5"), values(facets.get("ccm:caption_desc").get(0)));
        assertEquals(List.of("x=1", "y=4"), values(facets.get("ccm:count_asc").get(0)));

        NodeSearch.Facet nested = facets.get("ccm:nested").get(0);
        assertEquals(List.of("n=3"), values(nested));
        assertEquals(2L, nested.getSumOtherDocCount());

        // every key of a multi term bucket becomes a value, duplicate keys keep their first count
        NodeSearch.Facet multi = facets.get("ccm:multi").get(0);
        assertEquals(List.of("a=3", "b=3", "c=2"), values(multi));
        assertEquals(4L, multi.getSumOtherDocCount());

        NodeSearch.Facet geo = facets.get("ccm:geo").get(0);
        assertEquals(List.of("7/1/2=4"), values(geo));
        assertNull(geo.getSumOtherDocCount());

        // both branches are merged, sorted by count and cut to size, the cut buckets count as "other"
        NodeSearch.Facet combined = facets.get("ccm:combined").get(0);
        assertEquals(List.of("s=5", "a=3"), values(combined));
        assertEquals(1L, combined.getSumOtherDocCount());

        assertEquals(List.of("t=1"), values(facets.get("ccm:top").get(0)));
    }
}
