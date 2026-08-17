package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.google.gson.JsonParser;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.impl.model.PermissionModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.cache.UserCache;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Covers the child query built for the "shares" tabs of the editorial page
 * ({@link SearchServiceElastic#getUserSharesQuery}).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceElasticUserSharesTest {

    private static final String USER = "tester";
    /** fixed so the recency decay script param does not make the query non-deterministic */
    private static final Instant ORIGIN_DATE = Instant.parse("2024-01-01T00:00:00Z");

    @Mock
    private PermissionModel permissionsModelDAO;
    @Mock
    private GuestService guestService;
    @Mock
    private PermissionService eduPermissionService;
    @Mock
    private org.edu_sharing.service.authority.AuthorityService eduAuthorityService;
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

    private SearchServiceElastic underTest;
    private MockedConstruction<MCAlfrescoAPIClient> mcAlfrescoApiClientMockedStatic;
    private MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic;
    private MockedStatic<AlfAppContextGate> alfAppContextGateMockedStatic;

    @BeforeEach
    void beforeEach() {
        mcAlfrescoApiClientMockedStatic = Mockito.mockConstruction(MCAlfrescoAPIClient.class);
        authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(USER);

        // getAllMemberships() resolves the authority service through the alfresco app context.
        // GROUP_EVERYONE is expected to be dropped from the result.
        Set<String> authorities = new LinkedHashSet<>();
        authorities.add("GROUP_class_a");
        authorities.add(CCConstants.AUTHORITY_GROUP_EVERYONE);
        when(authorityService.getAuthorities()).thenReturn(authorities);
        ServiceRegistry serviceRegistry = Mockito.mock(ServiceRegistry.class);
        when(serviceRegistry.getAuthorityService()).thenReturn(authorityService);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        when(applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY)).thenReturn(serviceRegistry);
        alfAppContextGateMockedStatic = Mockito.mockStatic(AlfAppContextGate.class);
        alfAppContextGateMockedStatic.when(AlfAppContextGate::getApplicationContext).thenReturn(applicationContext);

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

    @AfterEach
    void afterEach() {
        mcAlfrescoApiClientMockedStatic.close();
        authenticationUtilMockedStatic.close();
        alfAppContextGateMockedStatic.close();
    }

    /**
     * The elastic client serializes object keys in its own order, which says nothing about the
     * query semantics - so compare the parsed trees (order insensitive) instead of the strings.
     */
    private static void assertQuery(String expected, BoolQuery.Builder actual) {
        String actualJson = SearchServiceElasticTestUtils.indentJson(actual);
        assertEquals(JsonParser.parseString(expected), JsonParser.parseString(actualJson),
                () -> "actual query:\n" + actualJson);
    }

    /**
     * @param directionConditions the direction specific part of the SHARED has_child bool query
     * @param rejectedTab         whether the rejectedByUser tab is queried: its REJECTED has_child
     *                            flips from must_not to must
     */
    private static String expectedQuery(String directionConditions, boolean rejectedTab) {
        String rejectedByUser = """
                {
                  "has_child": {
                    "type": "share",
                    "query": {
                      "bool": {
                        "must": [
                          { "term": { "share.sharedWith": { "value": "tester" } } },
                          { "term": { "share.shareStatus": { "value": "REJECTED" } } }
                        ]
                      }
                    }
                  }
                }""";
        String shared = """
                {
                  "has_child": {
                    "type": "share",
                    "score_mode": "min",
                    "inner_hits": {
                      "name": "share",
                      "size": 1,
                      "sort": [ { "share.timestamp": { "order": "desc" } } ]
                    },
                    "query": {
                      "function_score": {
                        "boost_mode": "replace",
                        "functions": [
                          {
                            "script_score": {
                              "script": {
                                "source": "decayDateLinear(params.originDate, '1m', '0', 1.5, doc['share.timestamp'].value)",
                                "params": { "originDate": "2024-01-01T00:00:00Z" }
                              }
                            }
                          }
                        ],
                        "query": { "bool": { %s } }
                      }
                    }
                  }
                }""".formatted(directionConditions);
        return rejectedTab
                ? """
                { "bool": { "must": [ %s, %s ] } }""".formatted(shared, rejectedByUser)
                : """
                { "bool": { "must": [ %s ], "must_not": [ %s ] } }""".formatted(shared, rejectedByUser);
    }

    @Test
    void getUserSharesQueryFromUser() {
        assertQuery(
                expectedQuery("""
                        "must": [
                          { "term": { "share.shareStatus": { "value": "SHARED" } } },
                          { "term": { "share.sharedBy": { "value": "tester" } } }
                        ]""", false),
                underTest.getUserSharesQuery(UserShareDirection.fromUser, null, ORIGIN_DATE));
    }

    @Test
    void getUserSharesQueryToUserAppliesMaxAge() {
        assertQuery(
                expectedQuery("""
                        "must": [
                          { "term": { "share.shareStatus": { "value": "SHARED" } } },
                          { "range": { "share.timestamp": { "gte": "now-60s" } } },
                          { "term": { "share.sharedWith": { "value": "tester" } } }
                        ],
                        "must_not": [
                          { "term": { "share.sharedBy": { "value": "tester" } } }
                        ]""", false),
                underTest.getUserSharesQuery(UserShareDirection.toUser, 60L, ORIGIN_DATE));
    }

    @Test
    void getUserSharesQueryToUserGroupsSkipsEveryone() {
        assertQuery(
                expectedQuery("""
                        "must": [
                          { "term": { "share.shareStatus": { "value": "SHARED" } } }
                        ],
                        "minimum_should_match": "1",
                        "should": [
                          { "term": { "share.sharedWith": { "value": "GROUP_class_a" } } }
                        ]""", false),
                underTest.getUserSharesQuery(UserShareDirection.toUserGroups, null, ORIGIN_DATE));
    }

    /**
     * The rejected tab lists exactly what the other directions filter out: the REJECTED has_child
     * flips from must_not to must, and the SHARED child is matched for the user and their groups.
     */
    @Test
    void getUserSharesQueryRejectedByUser() {
        assertQuery(
                expectedQuery("""
                        "must": [
                          { "term": { "share.shareStatus": { "value": "SHARED" } } }
                        ],
                        "must_not": [
                          { "term": { "share.sharedBy": { "value": "tester" } } }
                        ],
                        "minimum_should_match": "1",
                        "should": [
                          { "term": { "share.sharedWith": { "value": "tester" } } },
                          { "term": { "share.sharedWith": { "value": "GROUP_class_a" } } }
                        ]""", true),
                underTest.getUserSharesQuery(UserShareDirection.rejectedByUser, null, ORIGIN_DATE));
    }
}
