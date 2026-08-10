package org.edu_sharing.service.nodeservice;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceElasticTestUtils;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NodeFrontpageTest {

    //language=JSON
    private static final String QUERY_MATH = """
            {"match":{"properties.cclom:general_keyword":"Mathematik"}}""";
    //language=JSON
    private static final String QUERY_PHYSICS = """
            {"match":{"properties.cclom:general_keyword":"Physik"}}""";
    //language=JSON
    private static final String QUERY_CHEMISTRY = """
            {"match":{"properties.cclom:general_keyword":"Chemie"}}""";

    /**
     * the base restrictions every frontpage query carries. the read permission part is an empty bool
     * because {@link SearchServiceElastic#getReadPermissionsQuery(BoolQuery.Builder)} is mocked to a
     * no-op here - it is covered by SearchServiceElasticTest
     */
    //language=JSON
    private static final String BASE_CONDITIONS = """
            {
              "bool": {
                "must": [
                  { "bool": {} },
                  { "term": { "type": { "value": "ccm:io" } } },
                  { "term": { "nodeRef.storeRef.protocol": { "value": "workspace" } } }
                ],
                "must_not": [
                  { "term": { "aspects": { "value": "ccm:collection_io_reference" } } }
                ]
              }
            }""";

    @Mock
    private SearchServiceElastic searchServiceElastic;
    @Mock
    private CollectionService collectionService;
    @Mock
    private ToolPermissionService toolPermissionService;

    private NodeFrontpage underTest;
    private MockedStatic<ToolPermissionServiceFactory> toolPermissionServiceFactoryMockedStatic;

    @BeforeEach
    void beforeEach() {
        toolPermissionServiceFactoryMockedStatic = Mockito.mockStatic(ToolPermissionServiceFactory.class);
        toolPermissionServiceFactoryMockedStatic.when(ToolPermissionServiceFactory::getInstance).thenReturn(toolPermissionService);
        when(searchServiceElastic.getReadPermissionsQuery(any())).thenAnswer(invocation -> invocation.getArgument(0));

        underTest = new NodeFrontpage(searchServiceElastic, collectionService);
    }

    @AfterEach
    void afterEach() {
        toolPermissionServiceFactoryMockedStatic.close();
    }

    @Test
    void buildQueryWithoutAnyCustomQuery() {
        SearchServiceElasticTestUtils.assertQuery(
                BASE_CONDITIONS,
                underTest.buildQuery(config(RepositoryConfig.Frontpage.Mode.rating)));
    }

    @Test
    void buildQueryIgnoresBlankGlobalQuery() {
        RepositoryConfig.Frontpage config = config(RepositoryConfig.Frontpage.Mode.random);
        config.setGlobalQuery("   ");

        SearchServiceElasticTestUtils.assertQuery(BASE_CONDITIONS, underTest.buildQuery(config));
    }

    @Test
    void buildQueryAppendsGlobalQuery() {
        RepositoryConfig.Frontpage config = config(RepositoryConfig.Frontpage.Mode.random);
        config.setGlobalQuery(QUERY_MATH);

        SearchServiceElasticTestUtils.assertQuery(
                """
                        {
                          "bool": {
                            "must": [
                              { "bool": {} },
                              { "term": { "type": { "value": "ccm:io" } } },
                              { "term": { "nodeRef.storeRef.protocol": { "value": "workspace" } } },
                              { "wrapper": { "query": "%s" } }
                            ],
                            "must_not": [
                              { "term": { "aspects": { "value": "ccm:collection_io_reference" } } }
                            ]
                          }
                        }""".formatted(base64(QUERY_MATH)),
                underTest.buildQuery(config)
        );
    }

    /**
     * the global query is unconditional, the configured queries are only applied when their
     * toolpermission condition currently matches
     */
    @Test
    void buildQueryCombinesGlobalQueryWithMatchingConditionalQueriesOnly() {
        RepositoryConfig.Frontpage config = config(RepositoryConfig.Frontpage.Mode.random);
        config.setGlobalQuery(QUERY_MATH);
        config.setQueries(List.of(
                query("TOOLPERMISSION_GRANTED", false, QUERY_PHYSICS),
                query("TOOLPERMISSION_MISSING", false, QUERY_CHEMISTRY)
        ));
        when(toolPermissionService.hasToolPermission("TOOLPERMISSION_GRANTED")).thenReturn(true);
        when(toolPermissionService.hasToolPermission("TOOLPERMISSION_MISSING")).thenReturn(false);

        String actual = SearchServiceElasticTestUtils.indentJson(underTest.buildQuery(config));
        assertTrue(actual.contains(base64(QUERY_MATH)), actual);
        assertTrue(actual.contains(base64(QUERY_PHYSICS)), actual);
        assertFalse(actual.contains(base64(QUERY_CHEMISTRY)), actual);
    }

    /**
     * a negated condition inverts the toolpermission check
     */
    @Test
    void buildQueryAppliesNegatedCondition() {
        RepositoryConfig.Frontpage config = config(RepositoryConfig.Frontpage.Mode.rating);
        config.setQueries(List.of(query("TOOLPERMISSION_MISSING", true, QUERY_CHEMISTRY)));
        when(toolPermissionService.hasToolPermission("TOOLPERMISSION_MISSING")).thenReturn(false);

        String actual = SearchServiceElasticTestUtils.indentJson(underTest.buildQuery(config));
        assertTrue(actual.contains(base64(QUERY_CHEMISTRY)), actual);
    }

    @Test
    void buildSortOptionsForRandomMode() {
        SearchServiceElasticTestUtils.assertSort(
                //language=JSON
                """
                        {
                          "_script": {
                            "script": { "source": "Math.random()", "lang": "painless" },
                            "type": "number"
                          }
                        }""",
                underTest.buildSortOptions(config(RepositoryConfig.Frontpage.Mode.random))
        );
    }

    @Test
    void buildSortOptionsForStatisticModeUsesTheModeSpecificFields() {
        RepositoryConfig.Frontpage config = config(RepositoryConfig.Frontpage.Mode.views);
        config.setTimespanAll(true);

        String actual = SearchServiceElasticTestUtils.indentJson(underTest.buildSortOptions(config));
        assertTrue(actual.contains("statistic_VIEW_MATERIAL_null"), actual);
        assertTrue(actual.contains("\"order\": \"desc\""), actual);
        assertFalse(actual.contains("Math.random()"), actual);
    }

    private static String base64(String query) {
        return Base64.getEncoder().encodeToString(query.getBytes(StandardCharsets.UTF_8));
    }

    private static RepositoryConfig.Frontpage config(RepositoryConfig.Frontpage.Mode mode) {
        RepositoryConfig.Frontpage config = new RepositoryConfig.Frontpage();
        config.setMode(mode);
        return config;
    }

    private static RepositoryConfig.Frontpage.Query query(String toolpermission, boolean negate, String queryString) {
        RepositoryConfig.Frontpage.Query query = new RepositoryConfig.Frontpage.Query();
        query.getCondition().setType(RepositoryConfig.Condition.Type.TOOLPERMISSION);
        query.getCondition().setValue(toolpermission);
        query.getCondition().setNegate(negate);
        query.setQuery(queryString);
        return query;
    }
}
