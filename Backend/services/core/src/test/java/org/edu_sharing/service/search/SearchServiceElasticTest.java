package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.impl.model.PermissionModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.cache.UserCache;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.when;

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
    void afterEach() {
        mcAlfrescoApiClientMockedStatic.close();
        authenticationUtilMockedStatic.close();
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

        Assertions.assertEquals("{\"bool\":{}}", underTest.getAuthorityCombinedQuery(AuthorityType.USER, null, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Map<String, String> groupType = new HashMap<>() {{
            put("groupType", "TEST");
        }};

        Map<String, String> personStatus = new HashMap<>() {{
            put("cm:espersonstatus", "TEST");
        }};
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"wildcard\":{\"properties.cm:espersonstatus.keyword\":{\"value\":\"TEST\"}}}]}}", underTest.getAuthorityCombinedQuery(AuthorityType.USER, personStatus, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{}}", underTest.getAuthorityCombinedQuery(AuthorityType.USER, groupType, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"wildcard\":{\"properties.groupType.keyword\":{\"value\":\"TEST\"}}}]}}", underTest.getAuthorityCombinedQuery(AuthorityType.GROUP, groupType, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"must\":[{\"wildcard\":{\"properties.groupType.keyword\":{\"value\":\"TEST\"}}}]}}", underTest.getAuthorityCombinedQuery(AuthorityType.GROUP, groupType, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
        Assertions.assertEquals("{\"bool\":{\"minimum_should_match\":\"1\",\"should\":[{\"bool\":{}},{\"bool\":{\"must\":[{\"wildcard\":{\"properties.groupType.keyword\":{\"value\":\"TEST\"}}}]}}]}}", underTest.getAuthorityCombinedQuery(null, groupType, QueryBuilders.bool(), QueryBuilders.bool()).build()._toQuery().toString().substring("Query: ".length()));
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
}
