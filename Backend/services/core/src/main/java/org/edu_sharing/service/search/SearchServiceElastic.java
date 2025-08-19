package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.sourceforge.cardme.engine.VCardEngine;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.types.ExtendedType;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.model.PermissionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.guest.GuestConfig;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.generated.repository.backend.services.rest.client.model.ShareInfo;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.metadataset.v2.tools.MetadataElasticSearchHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LogTime;
import org.edu_sharing.repository.server.tools.StringTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.restservices.shared.Contributor;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.admin.SystemFolder;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.model.CollectionRef;
import org.edu_sharing.service.model.CollectionRefImpl;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.PropertiesGetInterceptor;
import org.edu_sharing.service.nodeservice.PropertiesInterceptorFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.search.model.*;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchServiceElastic extends SearchServiceImpl {
    public static final String WORKSPACE_INDEX = "workspace_11.0";
    public static final String AUTHORITIES_INDEX = "authorities_11.0";
    static RestClient restClient;
    static ElasticsearchClient client;
    static String rootHomeId;
    static String sysSystemNodeId;
    static String sysAuthoritiesNodeId;

    public SearchServiceElastic(String applicationId) {
        super(applicationId);
        if (SearchServiceElastic.sysSystemNodeId == null || SearchServiceElastic.sysAuthoritiesNodeId == null) {
            org.alfresco.service.cmr.repository.NodeRef rootHome = repositoryHelper.getRootHome();
            rootHomeId = rootHome.getId();
            NodeService nodeService = serviceRegistry.getNodeService();
            sysSystemNodeId = nodeService.getChildAssocs(
                    rootHome,
                    ContentModel.ASSOC_CHILDREN,
                    QName.createQName(ContentModel.ASSOC_CHILDREN.getNamespaceURI(), "system")
            ).get(0).getChildRef().getId();
            sysAuthoritiesNodeId = nodeService.getChildAssocs(
                    new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, sysSystemNodeId),
                    ContentModel.ASSOC_CHILDREN,
                    QName.createQName(ContentModel.ASSOC_CHILDREN.getNamespaceURI(), "authorities")
            ).get(0).getChildRef().getId();
        }
    }

    Logger logger = Logger.getLogger(SearchServiceElastic.class);

    ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
    Repository repositoryHelper = (Repository) alfApplicationContext.getBean("repositoryHelper");

    ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    PermissionModel permissionModel = (PermissionModel) alfApplicationContext.getBean("permissionsModelDAO");
    GuestService guestService = alfApplicationContext.getBean(GuestService.class);
    public static int MAX_RESPONSE_ENTITY_SIZE = -1;

    PermissionService eduPermissionService = PermissionServiceFactory.getLocalService();

    public static HttpHost[] getConfiguredHosts() {
        try {
            List<String> servers = LightbendConfigLoader.get().getStringList("elasticsearch.servers");
            List<HttpHost> hosts = new ArrayList<>();
            for (String server : servers) {
                hosts.add(new HttpHost(server.split(":")[0], Integer.parseInt(server.split(":")[1])));
            }
            return hosts.toArray(new HttpHost[0]);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static BoolQuery getFilesSharedToMeQuery(MetadataQueries queries, SharedToMeType type) {
        String username = AuthenticationUtil.getFullyAuthenticatedUser();
        Set<String> memberships = getAllMemberships(username);
        String basequery = queries.findQuery("sharedToMe").getPrimaryBasequery();

        BoolQuery.Builder builder = QueryBuilders.bool()
                .must(b -> b.bool(b2 -> b2.mustNot(
                                b3 -> b3.match(m -> m.field("properties.ccm:ph_users.keyword").query(username))
                        ).mustNot(
                                b3 -> b3.match(m -> m.field("properties.cm:creator.keyword").query(username))
                        )
                ));
        if (StringUtils.isNotBlank(basequery)) {
            builder.must(b -> b.wrapper(new ReadableWrapperQueryBuilder(basequery).build()));
        }

        builder.minimumShouldMatch("1");
        if (type.equals(SharedToMeType.All)) {
            memberships.forEach(m ->
                    builder.should(QueryBuilders.match().field("properties.ccm:ph_invited.keyword").query(m).build()._toQuery())
            );
        } else if (type.equals(SharedToMeType.Private)) {
            builder.should(QueryBuilders.match().field("properties.ccm:ph_invited.keyword").query(username).build()._toQuery());
        }
        return builder.build();
    }

    @NotNull
    private static Set<String> getAllMemberships(String username) {
        ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
        Set<String> memberships = new HashSet<>();
        memberships.add(username);
        memberships.addAll(serviceRegistry.getAuthorityService().getAuthorities());
        memberships.remove(CCConstants.AUTHORITY_GROUP_EVERYONE);
        return memberships;
    }

    public static BoolQuery getFilesSharedByMeQuery(MetadataQueries queries) {
        String username = AuthenticationUtil.getFullyAuthenticatedUser();
        String basequery = queries.findQuery("sharedByMe").getPrimaryBasequery();
        BoolQuery.Builder builder = QueryBuilders.bool()
                .must(b -> b.bool(b2 -> b2.must(
                        b3 -> b3.match(m -> m.field("properties.ccm:ph_users.keyword").query(username))
                )));
        if (StringUtils.isNotBlank(basequery)) {
            builder.must(b -> b.wrapper(new ReadableWrapperQueryBuilder(basequery).build()));
        }
        return builder.build();
    }

    public SearchResultNodeRefElastic searchDSL(String dsl, String index) throws Throwable {
        if (index == null || index.trim().isEmpty()) {
            index = WORKSPACE_INDEX;
        }
        checkClient();
        Request request = new Request("GET", index + "/_search");
        request.setJsonEntity(dsl);
        JSONObject response = new JSONObject(EntityUtils.toString(restClient.performRequest(request).getEntity()));
        SearchResultNodeRefElastic sr = new SearchResultNodeRefElastic();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);
        sr.setElasticResponse(response);
        JSONObject hits = response.getJSONObject("hits");
        int total = hits.getJSONObject("total").getInt("value");
        logger.info("result count: " + total);
        sr.setNodeCount(total);
        JSONArray hitsList = hits.getJSONArray("hits");
        Set<String> authorities = getUserAuthorities();
        boolean isAdmin = AuthorityServiceHelper.isAdmin();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
        for (int i = 0; i < hitsList.length(); i++) {
            Map hit = new ObjectMapper().readValue(hitsList.getJSONObject(i).getJSONObject("_source").toString(), Map.class);
            data.add(transformSearchHit(isAdmin, authorities, user, hit, false));
        }
        return sr;
    }

    private TransportOptions.Builder getRequestOptions(TransportOptions.Builder bld) {
        // add trace headers to elastic request
        Context context = Context.getCurrentInstance();
        if (context != null) {
            for (Map.Entry<String, String> header : context.getB3().getX3Headers().entrySet()) {
                bld.addHeader(header.getKey(), header.getValue());
            }
        }
        return bld;
    }

    public BoolQuery.Builder getPermissionsQuery(BoolQuery.Builder builder, String field) {
        Set<String> authorities = getUserAuthorities();
        return getPermissionsQuery(builder, field, authorities);
    }

    public BoolQuery.Builder getPermissionsQuery(BoolQuery.Builder audienceQueryBuilder, String field, Set<String> authorities) {
        BoolQuery.Builder bool = QueryBuilders.bool();
        bool.minimumShouldMatch("1");
        for (String a : authorities) {
            bool.should(should -> should.match(match -> match.field(field).query(a)));
        }
        audienceQueryBuilder.must(bool.build()._toQuery());
        return audienceQueryBuilder;
    }

    public BoolQuery.Builder getCollectionPermissionsQuery(BoolQuery.Builder builder) {
        if (AuthorityServiceHelper.isAdmin() || AuthenticationUtil.isRunAsUserTheSystemUser()) {
            return new BoolQuery.Builder().must(q -> q.matchAll(all -> all));
        }

        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
        CollectionPermissionQueries collectionPermissionQueries = getCollectionPermissionQueries(user);
        return new BoolQuery.Builder().minimumShouldMatch("1")
                .should(q -> q.nested(nested -> nested.path("collections").query(nq -> nq.bool(collectionPermissionQueries.collectionPermissions))))
                .should(q -> q.nested(nested -> nested.path("collections").query(nq -> nq.bool(collectionPermissionQueries.proposalPermissions))));
    }

    public BoolQuery.Builder getReadPermissionsQuery(BoolQuery.Builder builder) {
        if (AuthorityServiceHelper.isAdmin() || AuthenticationUtil.isRunAsUserTheSystemUser()) {
            return new BoolQuery.Builder().must(q -> q.matchAll(all -> all));
        }

        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();

        //enhance to collection permissions
        // @TODO: FIX after DESP-840
        CollectionPermissionQueries collectionPermissionQueries = getCollectionPermissionQueries(user);

        return new BoolQuery.Builder()
                .minimumShouldMatch("1")
                .should(getPermissionsQuery(builder, "permissions.read").build()._toQuery())
                .should(q -> q.match(m -> m.field("owner").query(user)))
                .should(audienceQueryBuilderCollections -> audienceQueryBuilderCollections
                        .bool(b -> b
                                .minimumShouldMatch("1")
                                .should(bs -> bs.bool(bb -> bb
                                        // restricted access is true -> do not inherit rights
                                        .mustNot(m -> m.term(t -> t.field("properties.ccm:restricted_access.keyword").value(true)))
                                        .must(m -> m.bool(subPermission -> subPermission
                                                .minimumShouldMatch("1")
                                                .should(q -> q.nested(nested -> nested.path("collections").query(nq -> nq.bool(collectionPermissionQueries.collectionPermissions))))
                                                .should(q -> q.nested(nested -> nested.path("collections").query(nq -> nq.bool(collectionPermissionQueries.proposalPermissions))))
                                        )))
                                ).should(bs -> bs.bool(bb -> bb
                                        // restricted access is "true" BUT "ReadAll" is given -> so behave like it would be false
                                        .must(m -> m.term(t -> t.field("properties.ccm:restricted_access.keyword").value(true)))
                                        .must(m -> m.term(t -> t.field("properties.ccm:restricted_access_permissions.keyword").value(CCConstants.PERMISSION_READ_ALL)))
                                        .must(m -> m.bool(subPermission -> subPermission
                                                .minimumShouldMatch("1")
                                                .should(q -> q.nested(nested -> nested.path("collections").query(nq -> nq.bool(collectionPermissionQueries.collectionPermissions))))
                                                .should(q -> q.nested(nested -> nested.path("collections").query(nq -> nq.bool(collectionPermissionQueries.proposalPermissions))))
                                        )))
                                )
                        )
                );
    }

    @NotNull
    private CollectionPermissionQueries getCollectionPermissionQueries(String user) {
        BoolQuery collectionPermissions = getPermissionsQuery(QueryBuilders.bool(), "collections.permissions.read")
                .should(s -> s.match(m -> m.field("collections.owner").query(user)))
                .must(must -> must.match(match -> match.field("collections.relation.type").query("ccm:usage")))
                .build();

        BoolQuery proposalPermissions = getPermissionsQuery(QueryBuilders.bool(), "collections.permissions.Coordinator", getUserAuthorities().stream().filter(a -> !a.equals(CCConstants.AUTHORITY_GROUP_EVERYONE)).collect(Collectors.toSet()))
                .should(s -> s.match(m -> m.field("collections.owner").query(user)))
                .must(must -> must.match(match -> match.field("collections.relation.type").query("ccm:collection_proposal")))
                .build();
        CollectionPermissionQueries result = new CollectionPermissionQueries(collectionPermissions, proposalPermissions);
        return result;
    }

    private static class CollectionPermissionQueries {
        public final BoolQuery collectionPermissions;
        public final BoolQuery proposalPermissions;

        public CollectionPermissionQueries(BoolQuery collectionPermissions, BoolQuery proposalPermissions) {
            this.collectionPermissions = collectionPermissions;
            this.proposalPermissions = proposalPermissions;
        }
    }

    public SearchResultNodeRef searchFacets(MetadataSet mds, String query, Map<String, String[]> criterias, SearchToken searchToken) throws Throwable {

        MetadataQuery queryData = mds.findQuery(query, MetadataReader.QUERY_SYNTAX_DSL);
        Query globalConditions = new Query.Builder()
                .bool(getGlobalConditions(searchToken.getAuthorityScope(), searchToken.getPermissions(), queryData).build())
                .build();

        Set<MetadataQueryParameter> excludeOwnFacets = MetadataElasticSearchHelper.getExcludeOwnFacets(queryData, new HashMap<>(), searchToken.getFacets());
        Map<String, Aggregation> aggregations = MetadataElasticSearchHelper.getAggregations(
                mds,
                queryData,
                criterias,
                searchToken.getFacets(),
                excludeOwnFacets,
                globalConditions,
                searchToken);

        SearchResultNodeRef result = parseAggregations(mds, queryData, searchToken, aggregations);
        result.setFacets(result.getFacets().stream().map(facet -> {
            facet.setValues(facet.getValues().stream().filter(s -> {
                // if one document has i.e. multiple keywords, they will be shown in the facet
                // so, we filter for values which actually contain the given string
                {
                    try {
                        String value = s.getValue();
                        try {
                            // map to i18n value if available
                            value = mds.findWidget(facet.getProperty()).getValuesAsMap().get(value).getCaption();
                        } catch (Throwable ignored) {
                        }
                        return value.toLowerCase().contains(searchToken.getQueryString().toLowerCase());
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).distinct().limit(searchToken.getFacetLimit()).collect(Collectors.toList()));
            return facet;
        }).collect(Collectors.toList()));
        return result;
    }

    @NotNull
    private SearchResultNodeRef parseAggregations(MetadataSet mds, MetadataQuery queryData, SearchToken searchToken, Map<String, Aggregation> aggregations) throws Exception {
//        logger.info("query aggs: "+searchSourceBuilderAggs.toString());
        checkClient();
        SearchResponse<Map> resp = LogTime.log("Searching elastic for facets", () -> client.search(req -> req
                        .index(WORKSPACE_INDEX)
                        .from(0)
                        .size(0)
                        .aggregations(aggregations)
                , Map.class));

        List<NodeSearch.Facet> facetsResult = getFacets(mds, queryData, aggregations, resp);

        SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
        searchResultNodeRef.setData(new ArrayList<>());
        searchResultNodeRef.setFacets(facetsResult);
        searchResultNodeRef.setStartIDX(searchToken.getFrom());
        searchResultNodeRef.setNodeCount(0);

        return searchResultNodeRef;
    }

    @NotNull
    private List<NodeSearch.Facet> getFacets(MetadataSet mds, MetadataQuery queryData, Map<String, Aggregation> aggregations, ResponseBody<Map> resp) {
        List<NodeSearch.Facet> facetsResult = new ArrayList<>();
        for (Map.Entry<String, Aggregate> a : resp.aggregations().entrySet()) {
            if (a.getValue().isFilter()) {
                FilterAggregate pf = a.getValue().filter();
                for (Map.Entry<String, Aggregate> aggregation : pf.aggregations().entrySet()) {
                    if (aggregation.getValue().isSterms()) {
                        Aggregation definition = aggregations.get(a.getKey());
                        StringTermsAggregate sterms = aggregation.getValue().sterms();
                        facetsResult.add(getFacet(mds, queryData, aggregation.getKey(), sterms, definition));
                    } else if (aggregation.getValue().isNested()) {
                        Aggregation definition = aggregations.get(a.getKey());
                        NestedAggregate nested = aggregation.getValue().nested();
                        StringTermsAggregate sterms = nested.aggregations().values().stream().findFirst().get().sterms();
                        facetsResult.add(getFacet(mds, queryData, aggregation.getKey(), sterms, definition));
                    } else if (aggregation.getValue().isMultiTerms()) {
                        Aggregation definition = aggregations.get(a.getKey());
                        MultiTermsAggregate multiTerm = aggregation.getValue().multiTerms();
                        facetsResult.add(getMultitermFacet(mds, queryData, aggregation.getKey(), multiTerm, definition));
                    }
                }
            }else if(a.getValue().isSterms()){
                if (a.getValue().isSterms()) {
                    Aggregation definition = aggregations.get(a.getKey());
                    StringTermsAggregate sterms = a.getValue().sterms();
                    facetsResult.add(getFacet(mds,queryData, a.getKey(), sterms, definition));
                }
            }else {
                logger.error("non supported aggregation " + a.getKey());
            }
        }
        return facetsResult;
    }

    private NodeSearch.Facet getMultitermFacet(MetadataSet mds, MetadataQuery queryData, String name, MultiTermsAggregate mta, Aggregation definition) {
        NodeSearch.Facet facet = new NodeSearch.Facet();
        facet.setProperty(name);
        List<NodeSearch.Facet.Value> values = new ArrayList<>();
        facet.setValues(values);

        for (MultiTermsBucket b : mta.buckets().array()) {
            for (FieldValue fv : b.key()) {
                long count = b.docCount();
                NodeSearch.Facet.Value value = new NodeSearch.Facet.Value();
                // skip duplicate entries
                if (values.stream().anyMatch(v -> v.getValue().equals(fv.stringValue()))) {
                    continue;
                }
                value.setValue(fv.stringValue());
                value.setCount((int) count);
                values.add(value);
            }
        }

        setSumOtherDocCount(queryData, name, mta, facet);
        sortFacetValues(mds, queryData, name, values);
        return facet;
    }

    private void setSumOtherDocCount(MetadataQuery queryData, String name, TermsAggregateBase<?> buckets, NodeSearch.Facet facet) {
        if(queryData == null) return;
        facet.setSumOtherDocCount(buckets.sumOtherDocCount());
        MetadataQueryParameter metadataQueryParameter = queryData.findParameterByName(name);
        if (metadataQueryParameter == null) {
            return;
        }

        MetadataQueryParameter.MetadataQueryFacet metadataQueryFacet = metadataQueryParameter.getFacet();
        if(metadataQueryFacet == null) {
            return;
        }

        Integer maxBucketSize = metadataQueryFacet.getMaxBucketSize();
        if(maxBucketSize == null) {
            return;
        }

        facet.setSumOtherDocCount(0L);
    }

    private void sortFacetValues(MetadataSet mds, MetadataQuery queryData, String name, List<NodeSearch.Facet.Value> values) {
        if(mds == null || queryData == null) return;

        MetadataWidget widget = mds.findWidget(name);
        Map<String, MetadataKey> valuesAsMap = widget.getValuesAsMap();
        MetadataQueryParameter metadataQueryParameter = queryData.findParameterByName(name);
        if (metadataQueryParameter == null) {
            return;
        }

        MetadataQueryParameter.MetadataQueryFacet metadataQueryFacet = metadataQueryParameter.getFacet();
        if (metadataQueryFacet == null) {
            return;
        }

        if (metadataQueryFacet.getSortBy() == MetadataQueryParameter.MetadataQueryFacet.SortBy.caption) {
            int order = metadataQueryFacet.getSortOrder() == MetadataQueryParameter.MetadataQueryFacet.SortOrder.asc ? 1 : -1;

            values.sort((lhs, rhs) -> {
                MetadataKey lhsMetadataKey = valuesAsMap.get(lhs.getValue());
                MetadataKey rhsMetadataKey = valuesAsMap.get(rhs.getValue());
                if (lhsMetadataKey == null || rhsMetadataKey == null) {
                    return 0;
                }
                return lhsMetadataKey.getCaption().compareToIgnoreCase(rhsMetadataKey.getCaption()) * order;
            });
        } else if(metadataQueryFacet.getSortOrder() == MetadataQueryParameter.MetadataQueryFacet.SortOrder.asc) {
            values.sort(Comparator.comparing(NodeSearch.Facet.Value::getCount));
        }
    }

    /**
     * fetches all nodes with the given query using the scroll api
     * ignores maxCount & skipCount set!
     * Does not evaluate any suggestions or facettes, only returns nodes
     *
     * @param mds
     * @param query
     * @param criterias
     * @param searchToken
     * @return
     * @throws Throwable
     */
    public List<NodeRef> searchAll(MetadataSet mds, String query, Map<String, String[]> criterias,
                                   SearchToken searchToken) throws Throwable {
        checkClient();
        MetadataQuery queryData = mds.findQuery(query, MetadataReader.QUERY_SYNTAX_DSL);

        BoolQuery.Builder metadataQueryBuilderFilter = MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criterias, true);
        BoolQuery.Builder metadataQueryBuilderAsQuery = MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criterias, false);
        BoolQuery.Builder queryBuilderGlobalConditions = getGlobalConditions(searchToken.getAuthorityScope(), searchToken.getPermissions(), queryData);

        SearchRequest.Builder searchRequest = new SearchRequest.Builder()
                .index(WORKSPACE_INDEX)
                .scroll(Time.of(time -> time.time("60s")))
                .source(src -> src
                        .filter(filter -> filter.excludes(appendDefaultExcludes(searchToken.getExcludes())))
                )
                .size(100)
                .query(q -> q
                        .bool(b -> b
                                .filter(filter -> filter
                                        .bool(fb -> fb
                                                .must(fq -> fq.bool(metadataQueryBuilderFilter.build()))
                                                .must(fq -> fq.bool(queryBuilderGlobalConditions.build()))))
                                .must(must -> must.bool(metadataQueryBuilderAsQuery.build()))));

        if (searchToken.getSortDefinition() != null) {
            searchToken.getSortDefinition().applyToSearchSourceBuilder(searchRequest);
        }

        return fetchAllFromRequest(mds,queryData,searchToken, searchRequest,null).getData();
    }

    private @NotNull SearchResultNodeRef fetchAllFromRequest(MetadataSet mds, MetadataQuery queryData, SearchToken searchToken, SearchRequest.Builder searchRequest,Map<String,Aggregation> aggregations) throws IOException {
        SearchResultNodeRef sr = new SearchResultNodeRef();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);
        Set<String> authorities = getUserAuthorities();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
        boolean isAdmin = AuthorityServiceHelper.isAdmin();
        try {
            String scrollId = null;
            while (true) {
                ResponseBody<Map> searchResponse;
                if (scrollId == null) {
                    searchResponse = client
                            .withTransportOptions(this::getRequestOptions)
                            .search(searchRequest.build(), Map.class);
                    if(aggregations != null) {
                        sr.setFacets(getFacets(mds,queryData,aggregations,searchResponse));
                    }
                } else {
                    final String usedScrollId = scrollId;
                    searchResponse = client
                            .withTransportOptions(this::getRequestOptions)
                            .scroll(scroll -> scroll.scrollId(usedScrollId).scroll(t -> t.time("60s")), Map.class);
                }
                scrollId = searchResponse.scrollId();

                HitsMetadata<Map> hits = searchResponse.hits();
                for (Hit<Map> hit : hits.hits()) {
                    data.add(transformSearchHit(isAdmin, authorities, user, hit.source(), searchToken.isResolveCollections()));
                }
                if (hits.hits().isEmpty()) {
                    break;
                }
            }
        } catch (ElasticsearchException e) {
            logger.error("Error running query. The query is logged below for debugging reasons");
            logger.error(e.getMessage(), e);
            logger.error(searchRequest.toString());
            throw e;
        }
        logger.info("result count: " + data.size());
        sr.setStartIDX(0);
        sr.setNodeCount(data.size());
        return sr;
    }

    private List<String> appendDefaultExcludes(List<String> excludes) {
        if (excludes == null) excludes = new ArrayList<>();
        else excludes = new ArrayList<>(excludes);

        if (!excludes.contains("content.fulltext")) {
            excludes.add("content.fulltext");
        }
        return excludes;
    }

    @Override
    public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criteria,
                                      SearchToken searchToken) throws Throwable {

        checkClient();
        MetadataQuery queryData;
        try {
            queryData = mds.findQuery(query, MetadataReader.QUERY_SYNTAX_DSL);
        } catch (IllegalArgumentException e) {
            logger.info("Query " + query + " is not defined within dsl language, switching to lucene...");
            return super.search(mds, query, criteria, searchToken);
        }
        Set<String> authorities;
        if (searchToken.getAuthorityScope() != null && !searchToken.getAuthorityScope().isEmpty()) {
            logger.debug("Searching elastic with authority scope: " + StringUtils.join(searchToken.getAuthorityScope()));
            authorities = new HashSet<>(searchToken.getAuthorityScope());
        } else {
            authorities = getUserAuthorities();
        }
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();


        SearchResultNodeRef sr = new SearchResultNodeRef();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);
        try {

            BoolQuery metadataQueryBuilderFilter = MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criteria, true).build();
            BoolQuery metadataQueryBuilderAsQuery = MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criteria, false).build();
            StoreRef storeRef = (searchToken.getStoreName() != null && searchToken.getStoreProtocol() != null) ? new StoreRef(searchToken.getStoreProtocol(), searchToken.getStoreName()) : null;
            BoolQuery queryBuilderGlobalConditions = getGlobalConditions(searchToken.getAuthorityScope(), searchToken.getPermissions(), queryData, storeRef,true).build();

            // add collapse builder
            // CollapseBuilder collapseBuilder = new CollapseBuilder("properties.ccm:original");
            // searchSourceBuilder.collapse(collapseBuilder);
            // cardinality aggregation to get correct total count
            // https://github.com/elastic/elasticsearch/issues/24130
            // CardinalityAggregationBuilder original_count = AggregationBuilders.cardinality("original_count").field("properties.ccm:original");
            // searchSourceBuilder.aggregation(original_count);


            SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                    .index(WORKSPACE_INDEX)
                    .source(src -> src.filter(filter -> filter.excludes(appendDefaultExcludes(searchToken.getExcludes()))));

            List<NodeSearch.Facet> facetsResult = null;
            SearchResponse<Map> searchResponseAggregations = null;
            Map<String, Aggregation> aggregations;
            if (searchToken.getFacets() != null) {
                Set<MetadataQueryParameter> excludeOwnFacets = MetadataElasticSearchHelper.getExcludeOwnFacets(queryData, criteria, searchToken.getFacets());
                if (!excludeOwnFacets.isEmpty()) {
                    Map<String, Aggregation> excludedOwnAggregations = MetadataElasticSearchHelper.getAggregations(
                            mds,
                            queryData,
                            criteria,
                            searchToken.getFacets(),
                            excludeOwnFacets,
                            queryBuilderGlobalConditions._toQuery(),
                            searchToken);

                    // remove duplicate facet entries
                    excludedOwnAggregations.entrySet().removeIf(e -> e.getKey().endsWith(MetadataElasticSearchHelper.FACET_SELECTED_POSTFIX));

                    SearchRequest searchSourceAggs = SearchRequest.of(req -> req
                            .index(WORKSPACE_INDEX)
                            .from(0)
                            .size(0)
                            .aggregations(excludedOwnAggregations));

                    logger.info("query aggs: " + JsonpUtils.toJsonString(searchSourceAggs, new JacksonJsonpMapper()));
                    searchResponseAggregations = LogTime.log("Searching elastic for facets", () -> client.search(searchSourceAggs, Map.class));
                    facetsResult = getFacets(mds, queryData, excludedOwnAggregations, searchResponseAggregations);
                    aggregations = null;
                } else {
                    aggregations = MetadataElasticSearchHelper.getAggregations(mds, queryData, searchToken.getParameters(), searchToken.getFacets(), Collections.emptySet(), queryBuilderGlobalConditions._toQuery(), searchToken);
                    for (Map.Entry<String, Aggregation> agg : aggregations.entrySet()) {
                        // we use a higher facet limit since the facets will be filtered for the containing string!
                        searchRequestBuilder.aggregations(agg.getKey(), agg.getValue());
                    }
                }
            } else {
                aggregations = null;
            }

            if (searchToken.isReturnSuggestion()) {
                String[] ngsearches = criteria.get("ngsearchword");
                if (ngsearches != null) {
                    searchRequestBuilder.suggest(suggest -> suggest
                            .text(ngsearches[0])
                            .suggesters("ngsearchword", s -> s
                                    .phrase(p -> p
                                            .field("properties.cclom:title.trigram")
                                            .gramSize(3)
                                            .confidence(0.9)
                                            .highlight(high -> high.preTag("<em>").postTag("</em>"))
                                            .directGenerator(x -> x
                                                    .field("properties.cclom:title.trigram")
                                                    .suggestMode(SuggestMode.Popular))
                                            .smoothing(smooth -> smooth.laplace(l -> l.alpha(0.5))))));
                }
            }


            searchRequestBuilder.query(q -> {
                BoolQuery boolQuery = BoolQuery.of(b -> b
                        .filter(filter -> filter
                                .bool(bool -> bool
                                        .must(must -> must.bool(metadataQueryBuilderFilter))
                                        .must(must -> must.bool(queryBuilderGlobalConditions))))
                        .must(must -> must.bool(metadataQueryBuilderAsQuery)));
                if(!queryData.getFunctions().isEmpty()) {
                    return q.functionScore(f ->
                            f.query(q2 -> q2.bool(boolQuery)).
                                    functions(queryData.getFunctions().stream().map(f2 -> FunctionScore.of(
                                            f3 -> f3
                                                    .filter(f4 -> f4.wrapper(new ReadableWrapperQueryBuilder(
                                                                    QueryUtils.replaceCommonQueryParams(f2.getFilter(), QueryUtils.replacerFromSyntax(MetadataReader.QUERY_SYNTAX_DSL, true))
                                                            ).build()
                                                    ))
                                                    .weight(f2.getWeight()))
                                    ).collect(Collectors.toList()))
                    );
                } else {
                    return q
                            .bool(boolQuery);
                }
            });
            searchRequestBuilder.from(searchToken.getFrom());
            searchRequestBuilder.size(searchToken.getMaxResult());
            searchRequestBuilder.trackTotalHits(new TrackHits.Builder().enabled(true).build());
            if (searchToken.getSortDefinition() != null) {
                searchToken.getSortDefinition().applyToSearchSourceBuilder(searchRequestBuilder);
            }

            // logger.info("query: "+searchSourceBuilder.toString());
            SearchRequest searchRequest = searchRequestBuilder.build();
            try {
                logger.info("query: " + JsonpUtils.toJsonString(searchRequest, new JacksonJsonpMapper()));
                SearchResponse<Map> searchResponse = LogTime.log("Searching elastic", () -> client.search(searchRequest, Map.class));

                HitsMetadata<Map> hits = searchResponse.hits();
                logger.info("result count: " + hits.total());

                long millisPerm = System.currentTimeMillis();
                boolean isAdmin = AuthorityServiceHelper.isAdmin();
                for (Hit<Map> hit : hits.hits()) {
                    data.add(transformSearchHit(isAdmin, authorities, user, hit.source(), searchToken.isResolveCollections()));
                }
                logger.info("permission stuff took:" + (System.currentTimeMillis() - millisPerm));


                Long total = null;

                if (!searchResponse.suggest().isEmpty()) {
                    List<co.elastic.clients.elasticsearch.core.search.Suggestion<Map>> phraseSuggestion = searchResponse.suggest().get("ngsearchword");
                    if (!phraseSuggestion.isEmpty()) {
                        List<NodeSearch.Suggest> suggests = phraseSuggestion.stream()
                                .filter(co.elastic.clients.elasticsearch.core.search.Suggestion::isPhrase)
                                .map(co.elastic.clients.elasticsearch.core.search.Suggestion::phrase)
                                .map(PhraseSuggest::options)
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .map(x -> {
                                    NodeSearch.Suggest suggest = new NodeSearch.Suggest();
                                    suggest.setText(x.text());
                                    suggest.setHighlighted(x.highlighted());
                                    suggest.setScore(x.score());
                                    return suggest;
                                })
                                .collect(Collectors.toList());
                        suggests.forEach(x -> logger.info("SUGGEST:" + x.getText() + " " + x.getScore() + " " + x.getHighlighted()));
                        sr.setSuggests(suggests);
                    }
                }

                if (total == null) {
                    total = Optional.of(hits).map(HitsMetadata::total).map(TotalHits::value).orElse(0L);
                }
                if (facetsResult == null) {
                    facetsResult = getFacets(mds, queryData, aggregations, searchResponse);
                }
                sr.setFacets(facetsResult);
                sr.setStartIDX(searchToken.getFrom());
                sr.setNodeCount((int) total.longValue());
                //client.close();
            } catch (ElasticsearchException e) {
                logger.error("Error running query. The query is logged below for debugging reasons");
                logger.error(e.getMessage(), e);
                logger.error(JsonpUtils.toJsonString(searchRequest, new JacksonJsonpMapper()));
                throw e;
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }


        logger.info("returning");
        return sr;
    }

    private NodeSearch.Facet getFacet(MetadataSet mds, MetadataQuery queryData, String name, StringTermsAggregate pst, Aggregation builder) {
        NodeSearch.Facet facet = new NodeSearch.Facet();
        facet.setProperty(name);
        List<NodeSearch.Facet.Value> values = new ArrayList<>();
        facet.setValues(values);
//        widget.getValues().stream().filter(x->x.getIdentifiableValue().equals("efwef") )

        for (StringTermsBucket b : pst.buckets().array()) {
            if (builder != null && Aggregation.Kind.MultiTerms == builder._kind()) {
                String[] key = b.key().stringValue().split("\\|");
                for (String k : key) {
                    long count = b.docCount();
                    NodeSearch.Facet.Value value = new NodeSearch.Facet.Value();
                    // skip duplicate entries
                    if (values.stream().anyMatch(v -> v.getValue().equals(k))) {
                        continue;
                    }
                    value.setValue(k);
                    value.setCount((int) count);
                    values.add(value);
                }
            } else {
                String key = b.key().stringValue();
                long count = b.docCount();
                NodeSearch.Facet.Value value = new NodeSearch.Facet.Value();
                value.setValue(key);
                value.setCount((int) count);
                values.add(value);
            }
        }
        setSumOtherDocCount(queryData, name, pst, facet);
        sortFacetValues(mds, queryData, name, values);
        return facet;
    }

    /**
     * permissions, scope ...
     *
     * @param authorityScope
     * @param permissions
     * @param query
     * @return
     */
    BoolQuery.Builder getGlobalConditions(List<String> authorityScope, List<String> permissions, MetadataQuery query) {
        return getGlobalConditions(authorityScope, permissions, query, null, true);
    }

    BoolQuery.Builder getGlobalConditions(List<String> authorityScope, List<String> permissions, MetadataQuery query, StoreRef storeRef, boolean scoped) {

        String storeRefProtocol = (storeRef == null) ? "workspace" : storeRef.getProtocol();
        Function<BoolQuery.Builder, BoolQuery.Builder> queryGlobalConditionsFactory = (builder) ->
                ((authorityScope != null && !authorityScope.isEmpty())
                        ? getPermissionsQuery(builder, "permissions.read", new HashSet<>(authorityScope))
                        : getReadPermissionsQuery(builder))
                        .must(must -> must
                                .match(match -> match
                                        .field("nodeRef.storeRef.protocol")
                                        .query(storeRefProtocol)));


        BoolQuery.Builder queryBuilderGlobalConditions = queryGlobalConditionsFactory.apply(QueryBuilders.bool());
        if (permissions != null) {
            BoolQuery.Builder permissionsFilter = QueryBuilders.bool().must(must -> must.bool(queryGlobalConditionsFactory::apply));
            String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
            permissionsFilter.should(should -> should.match(match -> match.field("owner").query(user)));
            for (String permission : permissions) {
                permissionsFilter.should(s -> s.bool(bool -> getPermissionsQuery(bool, "permissions." + permission)));
            }
            queryBuilderGlobalConditions = permissionsFilter;
        }

        if(scoped){
            if (NodeServiceInterceptor.getEduSharingScope() == null) {
                queryBuilderGlobalConditions.mustNot(mustNot -> mustNot.exists(exist -> exist.field("properties.ccm:eduscopename")));
            } else {
                queryBuilderGlobalConditions.must(must -> must.term(term -> term.field("properties.ccm:eduscopename.keyword").value(NodeServiceInterceptor.getEduSharingScope())));
            }
        }
        // mds specialFilter processing on per-query basis
        if (query != null) {
            for (MetadataQuery.SpecialFilter filter : query.getSpecialFilter()) {
                if (MetadataQuery.SpecialFilter.exclude_system_folder.equals(filter)) {
                    queryBuilderGlobalConditions.mustNot(mustNot -> mustNot.wildcard(wild -> wild.field("fullpath").value("*/" + SystemFolder.getSystemFolderBase().getId() + "*")));
                } else if (MetadataQuery.SpecialFilter.exclude_sites_folder.equals(filter)) {
                    queryBuilderGlobalConditions.mustNot(mustNot -> mustNot.wildcard(wild -> wild.field("fullpath").value("*/" + SystemFolder.getSitesFolder().getId() + "*")));
                } else if (MetadataQuery.SpecialFilter.exclude_people_folder.equals(filter)) {
                    org.alfresco.service.cmr.repository.NodeRef personFolder = SystemFolder.getPersonFolder();
                    if (personFolder != null) {
                        queryBuilderGlobalConditions.mustNot(mustNot -> mustNot.wildcard(wild -> wild.field("fullpath").value("*/" + SystemFolder.getPersonFolder().getId() + "*")));
                    } else {
                        logger.warn("People folder unknown, elastic query is skipping special filter");
                    }
                }
            }
        }
        return queryBuilderGlobalConditions;
    }

    public Set<String> getUserAuthorities() {
        Set<String> authorities = serviceRegistry.getAuthorityService().getAuthorities();
        authorities.add(CCConstants.AUTHORITY_GROUP_EVERYONE);
        if (!AuthenticationUtil.isRunAsUserTheSystemUser()) {
            authorities.add(AuthenticationUtil.getFullyAuthenticatedUser());
        }
        return authorities;
    }

    public List<String> hasPermissions(String nodeId, List<String> permissions) {
        List<String> result = hasCollectionPermissionsOnNode(nodeId, permissions);
        if (!result.isEmpty()) return result;

//        BoolQueryBuilder checkIsChildObjectQuery = QueryBuilders.boolQuery()
//                .must(QueryBuilders.termQuery("properties.sys:node-uuid", nodeId))
//                .must(QueryBuilders.termQuery("aspects", "ccm:io_childobject"));
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.query(checkIsChildObjectQuery);
//        SearchRequest request = new SearchRequest("workspace");
//        request.source(searchSourceBuilder);
        try {
            SearchResponse<Map> searchResult = client
                    .withTransportOptions(this::getRequestOptions)
                    .search(req -> req
                            .index(WORKSPACE_INDEX)
                            .trackTotalHits(t -> t.enabled(true))
                            .query(query -> query
                                    .bool(checkIsChildObjectQuery -> checkIsChildObjectQuery
                                            .must(must -> must.term(term -> term.field("properties.sys:node-uuid").value(nodeId)))
                                            .must(must -> must.term(term -> term.field("aspects").value("ccm:io_childobject"))))), Map.class);

            if (searchResult.hits().total().value() == 0) {
                return Collections.emptyList();
            }

            Map source = searchResult.hits().hits().get(0).source();
            if (source == null) {
                return Collections.emptyList();
            }

            Map parentRef = (Map) source.get("parentRef");
            String parentId = (String) parentRef.get("id");
            return hasCollectionPermissionsOnNode(parentId, permissions);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * returns true if the user has the given permission on the node via indirect access inside a collection
     *
     * @return
     */
    private List<String> hasCollectionPermissionsOnNode(String nodeId, List<String> permissions) {
        try {
            checkClient();
            // fetch the node
            SearchResponse<Map> searchResult = client
                    .withTransportOptions(this::getRequestOptions)
                    .search(request -> request
                            .index(WORKSPACE_INDEX)
                            .size(1)
                            .trackTotalHits(t -> t.count(2))
                            .query(query -> query
                                    .bool(bool -> bool
                                            .must(must -> must.bool(this::getCollectionPermissionsQuery))
                                            .must(must -> must.term(term -> term.field("properties.sys:node-uuid").value(nodeId))))), Map.class);
            if (searchResult.hits().total().value() == 1) {
                // check & handle restricted access
                Map data = (Map) searchResult.hits().hits().get(0).source().get("properties");
                String restrictedAccess = (String) data.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_RESTRICTED_ACCESS));
                List<String> restrictedAccessPermissions = (List<String>) data.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_RESTRICTED_ACCESS_PERMISSIONS));
                return PermissionServiceHelper.getEffectivePermissions(
                        restrictedAccessPermissions,
                        Boolean.parseBoolean(restrictedAccess)
                ).stream().filter(permissions::contains).collect(Collectors.toList());
            } else {
                logger.warn("Permission query matched more than one node " + nodeId + " " + StringUtils.join(permissions));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    public NodeRef transformSearchHit(boolean isAdmin, Set<String> authorities, String user, Map hit, boolean resolveCollections) {
        try {
            return this.transform(NodeRefImpl.class, isAdmin, authorities, user, hit, resolveCollections);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends NodeRefImpl> T transform(Class<T> clazz, boolean isAdmin, Set<String> authorities, String user, Map<String, Object> sourceAsMap, boolean resolveCollections) throws IllegalAccessException, InstantiationException {
        Map<String, MetadataSet> mdsCache = new HashMap<>();
        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();

        Map<String, Serializable> properties = (Map) sourceAsMap.get("properties");

        Map nodeRef = (Map) sourceAsMap.get("nodeRef");
        String nodeId = (String) nodeRef.get("id");

        Map parentRef = (Map) sourceAsMap.get("parentRef");
        String parentId = (parentRef != null) ? (String) parentRef.get("id") : null;


        Map storeRef = (Map) nodeRef.get("storeRef");
        String protocol = (String) storeRef.get("protocol");
        String identifier = (String) storeRef.get("identifier");

        Map<String, Object> props = new HashMap<>();

        for (Map.Entry<String, Serializable> entry : properties.entrySet()) {

            Serializable value = null;
            /**
             * @TODO: transform to ValueTool.toMultivalue
             */
            if (entry.getValue() instanceof ArrayList) {
                ArrayList<?> list = (ArrayList<?>) entry.getValue();
                if (list.size() > 1 && list.get(0) instanceof String) {
                    value = ValueTool.toMultivalue(list.toArray(new String[0]));
                } else if (list.size() == 1) {
                    value = (Serializable) ((ArrayList<?>) entry.getValue()).get(0);
                }
            } else {
                value = entry.getValue();
            }
            if (entry.getKey().equals("ccm:mediacenter")) {
                List<Map<String, Object>> mediacenterStatus = (List<Map<String, Object>>) entry.getValue();
                ArrayList<String> result = new ArrayList<>();
                for (Map<String, Object> mcSt : mediacenterStatus) {
                    Gson gson = new Gson();
                    String json = gson.toJson(mcSt);
                    result.add(json);
                }
                value = ValueTool.toMultivalue(result.toArray(new String[result.size()]));
            }
            if (entry.getKey().equals("cm:created") || entry.getKey().equals("cm:modified") && value != null) {
                props.put(CCConstants.getValidGlobalName(entry.getKey()) + CCConstants.LONG_DATE_SUFFIX, ((Long) value).toString());
            }
            props.put(CCConstants.getValidGlobalName(entry.getKey()), value);

            /**
             * metadataset translation
             */
            Map<String, Serializable> i18n = (Map<String, Serializable>) sourceAsMap.get("i18n");
            if (i18n != null) {
                Map<String, Serializable> i18nProps = (Map<String, Serializable>) i18n.get(currentLocale);
                if (i18nProps != null) {
                    List<String> displayNames = (List<String>) i18nProps.get(entry.getKey());
                    if (displayNames != null) {
                        props.put(CCConstants.getValidGlobalName(entry.getKey()) + CCConstants.DISPLAYNAME_SUFFIX, String.join(CCConstants.MULTIVALUE_SEPARATOR, displayNames));
                    }
                }
            } else {
                try {
                    String mdsId = (String) properties.getOrDefault(
                            CCConstants.getValidLocalName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET),
                            CCConstants.metadatasetdefault_id);
                    MetadataSet mds = mdsCache.get(mdsId);
                    if (mds == null) {
                        mds = MetadataHelper.getMetadataset(
                                ApplicationInfoList.getHomeRepository(),
                                mdsId
                        );
                        mdsCache.put(mdsId, mds);
                    }

                    MetadataHelper.addVirtualDisplaynameProperties(mds, props);
                } catch (Throwable t) {
                    logger.info("Could not resolve displaynames: " + t.getMessage());
                }
            }
        }
        props.put(CCConstants.NODETYPE, sourceAsMap.get("type"));

        List<Map<String, Serializable>> children = (List) sourceAsMap.get("children");
        int childIOCount = 0;
        int usageCount = 0;
        int commentCount = 0;
        if (children != null) {
            for (Map<String, Serializable> child : children) {
                String type = (String) child.get("type");
                List<String> aspects = (List<String>) child.get("aspects");
                if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO).equals(type)
                        && aspects.contains(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT))) {
                    childIOCount++;
                }
                if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_USAGE).equals(type)) {
                    usageCount++;
                }
                if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_COMMENT).equals(type)) {
                    commentCount++;
                }
            }
        }
        if (childIOCount > 0) {
            props.put(CCConstants.VIRT_PROP_CHILDOBJECTCOUNT, childIOCount);
        }
        if (usageCount > 0) {
            props.put(CCConstants.VIRT_PROP_USAGECOUNT, usageCount);
        }
        if (commentCount > 0) {
            props.put(CCConstants.VIRT_PROP_COMMENTCOUNT, commentCount);
        }


        org.alfresco.service.cmr.repository.NodeRef alfNodeRef = new org.alfresco.service.cmr.repository.NodeRef(new StoreRef(protocol, identifier), nodeId);
        String contentUrl = URLHelper.getNgRenderNodeUrl(nodeId, null);
        contentUrl = URLTool.addOAuthAccessToken(contentUrl);
        props.put(CCConstants.CONTENTURL, contentUrl);

        if (sourceAsMap.get("content") != null) {
            props.put(CCConstants.DOWNLOADURL, URLTool.getDownloadServletUrl(alfNodeRef.getId(), null, true));
        }

        if (parentId != null) {
            props.put(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID, parentId);
        }

        T eduNodeRef = clazz.newInstance();
        eduNodeRef.setOrigin(NodeRef.Origin.Elasticsearch);
        eduNodeRef.setRepositoryId(ApplicationInfoList.getHomeRepository().getAppId());
        ;
        eduNodeRef.setStoreProtocol(protocol);
        eduNodeRef.setStoreId(identifier);
        eduNodeRef.setNodeId(nodeId);

        eduNodeRef.setAspects(((List<String>) sourceAsMap.get("aspects")).
                stream().map(CCConstants::getValidGlobalName).filter(Objects::nonNull).collect(Collectors.toList()));

        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put(CCConstants.PERMISSION_READ, true);
        GuestConfig guestConfig = guestService.getCurrentGuestConfig();
        long millis = System.currentTimeMillis();
        eduNodeRef.setPublic(false);
        Map<String, List<String>> permissionsElastic = (Map) sourceAsMap.get("permissions");
        String owner = (String) sourceAsMap.get("owner");
        if (permissionsElastic != null) {
            for (Map.Entry<String, List<String>> entry : permissionsElastic.entrySet()) {
                if ("read".equals(entry.getKey())) {
                    continue;
                }
                if (!eduNodeRef.getPublic() && guestConfig != null && guestConfig.isEnabled() && entry.getValue().contains(CCConstants.AUTHORITY_GROUP_EVERYONE)) {
                    PermissionReference pr = permissionModel.getPermissionReference(null, entry.getKey());
                    Set<PermissionReference> granteePermissions = permissionModel.getGranteePermissions(pr);
                    eduNodeRef.setPublic(granteePermissions.stream().anyMatch(p -> p.getName().equals(CCConstants.PERMISSION_READ_ALL)));
                }
                if (isAdmin || authorities.stream().anyMatch(s -> entry.getValue().contains(s))
                        || entry.getValue().contains(user)) {
                    //get fine grained permissions
                    PermissionReference pr = permissionModel.getPermissionReference(null, entry.getKey());
                    Set<PermissionReference> granteePermissions = permissionModel.getGranteePermissions(pr);
                    for (String perm : PermissionServiceHelper.PERMISSIONS) {
                        for (PermissionReference pRef : granteePermissions) {
                            if (pRef.getName().equals(perm)) {
                                permissions.put(perm, true);
                            }
                        }
                    }
                }
            }
        } else {
            logger.warn("permissionsElastic is null for " + identifier);
        }

        // @TODO: remove all of this from/to multivalue
        ValueTool.getMultivalue(props);
        PropertiesGetInterceptor.PropertiesContext propertiesContext = PropertiesInterceptorFactory.getPropertiesContext(
                alfNodeRef, props, eduNodeRef.getAspects(),
                permissions,
                sourceAsMap
        );
        for (PropertiesGetInterceptor i : PropertiesInterceptorFactory.getPropertiesGetInterceptors()) {
            props = new HashMap<>(i.beforeDeliverProperties(propertiesContext));
        }
        // @TODO: remove all of this from/to multivalue
        ValueTool.toMultivalue(props);
        eduNodeRef.setProperties(props);

        eduNodeRef.setOwner((String) sourceAsMap.get("owner"));

        Map preview = (Map) sourceAsMap.get("preview");
        if (preview != null && preview.get("small") != null) {
            eduNodeRef.setPreview(
                    new NodeRefImpl.PreviewImpl(
                            (String) preview.get("mimetype"),
                            Base64.getDecoder().decode((String) preview.get("small")),
                            (String) preview.get("type"),
                            (Boolean) preview.get("icon")
                    )
            );
        }

        List<Contributor> contributorsResult = new ArrayList<>();
        List contributors = (List) sourceAsMap.get("contributor");
        if (contributors != null) {
            for (Object contributor : contributors) {
                Map c = (Map) contributor;
                Contributor contributorResult = new Contributor();
                contributorResult.setProperty((String) c.get("property"));
                contributorResult.setEmail((String) c.get("email"));
                contributorResult.setFirstname((String) c.get("firstname"));
                contributorResult.setLastname((String) c.get("lastname"));
                contributorResult.setOrg((String) c.get("org"));
                contributorResult.setVcard((String) c.get("vcard"));
                contributorsResult.add(contributorResult);
            }
        }
        eduNodeRef.setContributors(contributorsResult);


        if (isAdmin || user.equals(owner)) {
            permissions.put(CCConstants.PERMISSION_CC_PUBLISH, true);
            PermissionReference pr = permissionModel.getPermissionReference(null, "FullControl");
            Set<PermissionReference> granteePermissions = permissionModel.getGranteePermissions(pr);
            for (String perm : PermissionServiceHelper.PERMISSIONS) {
                for (PermissionReference pRef : granteePermissions) {
                    if (pRef.getName().equals(perm)) {
                        permissions.put(perm, true);
                    }
                }
            }

            //Set<PermissionReference> granteePermissions = permissionModel.getGranteePermissions(pr);
            //Set<PermissionReference> immediateGranteePermissions = permissionModel.getImmediateGranteePermissions(pr);

        }
        // check if user has access via any collection and grant him all usage permissions
        processCollectionUsagePermissions(authorities, user, sourceAsMap, permissions);

        eduNodeRef.setPermissions(permissions);
        boolean isProposal = sourceAsMap.get("type").equals(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_COLLECTION_PROPOSAL));
        if (resolveCollections) {
            List<Map<String, Object>> collections = (List) sourceAsMap.get("collections");
            if (collections != null) {
                for (Map<String, Object> collection : collections) {
                    String colOwner = (String) collection.get("owner");
                    boolean hasPermission = user.equals(colOwner) || isAdmin;
                    if (!hasPermission) {
                        Map<String, List<String>> colPermissionsElastic = (Map) collection.get("permissions");
                        for (Map.Entry<String, List<String>> entry : colPermissionsElastic.entrySet()) {
                            if ("read".equals(entry.getKey())) {
                                hasPermission = entry.getValue().stream().anyMatch(s -> authorities.contains(s) || s.equals(user));
                                break;
                            }
                        }
                    }
                    if (hasPermission) {
                        CollectionRefImpl transform = transform(CollectionRefImpl.class, isAdmin, authorities, user, collection, false);
                        if (isProposal) {
                            transform.setRelationType(CollectionRef.RelationType.Proposal);
                        }
                        eduNodeRef.getUsedInCollections().add(transform);
                    }
                }
            }
        }
        if (isProposal && sourceAsMap.containsKey("original")) {
            eduNodeRef.getRelations().put(
                    NodeRefImpl.Relation.Original,
                    transform(NodeRefImpl.class, isAdmin, authorities, user, (Map) sourceAsMap.get("original"), false)
            );
        }
        if (eduNodeRef instanceof CollectionRefImpl) {
            CollectionRefImpl collectionRef = (CollectionRefImpl) eduNodeRef;
            Map<String, Object> relation = (Map) sourceAsMap.get("relation");
            if (relation != null) {
                // @TODO: transform relation type
                Map<String, Object> relationProps = (Map) relation.get("properties");
                if (relationProps.containsKey(CCConstants.getValidLocalName(CCConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS))) {
                    collectionRef.setRelationType(CollectionRef.RelationType.Proposal);
                } else {
                    collectionRef.setRelationType(CollectionRef.RelationType.Usage);
                }
                collectionRef.setRelationNode(transform(NodeRefImpl.class, isAdmin, authorities, user, relation, false));
            }
        }
        long permMillisSingle = (System.currentTimeMillis() - millis);
        return eduNodeRef;
    }

    /**
     * check if the user has permissions on this element via a collection and give him all permissions as it is an usage access
     */
    private static void processCollectionUsagePermissions(Set<String> authorities, String user, Map<String, Object> sourceAsMap, Map<String, Boolean> permissions) {
        if (permissions.size() == 1) {
            List<Map<String, Object>> collections = (List<Map<String, Object>>) sourceAsMap.get("collections");
            for (Map<String, Object> collection : Optional.ofNullable(collections).orElse(Collections.emptyList())) {
                Map<String, List<String>> collectionPermissions = (Map<String, List<String>>) collection.get("permissions");
                if (Optional.ofNullable(collectionPermissions).orElse(Collections.emptyMap()).entrySet().stream().filter(p ->
                        // check the consumer, collaborator or coordinator lists
                        Arrays.asList(CCConstants.PERMISSION_CONSUMER, CCConstants.PERMISSION_COLLABORATOR, CCConstants.PERMISSION_COORDINATOR).contains(p.getKey())
                ).anyMatch(
                        // and if the user has one of this rights
                        (entry) ->
                                authorities.stream().anyMatch(s -> entry.getValue().contains(s))
                                        || entry.getValue().contains(user))) {
                    permissions.putAll(
                            CCConstants.getUsagePermissions().stream().collect(
                                    Collectors.toMap(o -> o, (o) -> true)
                            ));
                    break;
                }
            }
        }
    }

    enum CONTRIBUTOR_PROP {firstname, lastname, email, url, uid}

    ;

    @Override
    public Set<SearchVCard> searchContributors(String suggest, List<String> fields, List<String> contributorProperties, ContributorKind contributorKind) throws IOException {
        checkClient();

        List<String> searchFields = new ArrayList<>();
        if (fields == null || fields.size() == 0) {
            for (CONTRIBUTOR_PROP att : CONTRIBUTOR_PROP.values()) {
                searchFields.add("contributor." + att.name());
            }
        } else {
            for (String f : fields) {
                if (Stream.of(CONTRIBUTOR_PROP.values()).anyMatch(v -> v.name().equals(f))) {
                    searchFields.add("contributor." + f);
                }
            }
        }
        final BoolQuery.Builder contributorQuery = QueryBuilders.bool();
        for (String searchField : searchFields) {
            final String search = suggest.contains("*") ? suggest : String.format("*%s*", suggest);
            contributorQuery.should(should -> should.wildcard(wc -> wc.field(searchField).value(search)));
        }

        if (!contributorProperties.isEmpty()) {
            contributorQuery.must(must -> must.bool(bool -> bool
                    .minimumShouldMatch("1")
                    .should(should -> {
                        contributorProperties.forEach(prop -> should.term(term -> term.field("contributor.property").value(prop)));
                        return should;
                    })));
        }

        if (contributorKind == ContributorKind.ORGANIZATION) {
            contributorQuery.must(must -> must.bool(bool -> bool
                    .should(should -> should.exists(exists -> exists.field("contributor.X-ROR")))
                    .should(should -> should.exists(exists -> exists.field("contributor.X-Wikidata")))
                    .minimumShouldMatch("1")));
        } else {
            contributorQuery.must(must -> must.bool(bool -> bool
                    .should(should -> should.exists(exists -> exists.field("contributor.X-ORCID")))
                    .should(should -> should.exists(exists -> exists.field("contributor.X-GND-URI")))
                    .minimumShouldMatch("1")));
        }

        SearchRequest searchRequest = SearchRequest.of(req -> req
                .index(WORKSPACE_INDEX)
                .from(0)
                .size(0)
                .trackTotalHits(track -> track.enabled(true))
                .sort(sort -> sort.score(score -> score.order(SortOrder.Desc)))
                .aggregations("contributor", aggr -> aggr
                        .nested(nes -> nes.path("contributor"))
                        .aggregations("vcard", vcardAggr -> vcardAggr
                                .terms(term -> term
                                        .field("contributor.vcard")
                                        .size(100))))
                .query(query -> query
                        .nested(nested -> nested
                                .path("contributor")
                                .query(nq -> nq
                                        .bool(contributorQuery.build())))));

        SearchResponse<Map> searchResponse = client
                .withTransportOptions(this::getRequestOptions)
                .search(searchRequest, Map.class);

        Aggregate aggregation = searchResponse.aggregations()
                .get("contributor")
                .nested()
                .aggregations()
                .get("vcard");

        VCardEngine engine = new VCardEngine();
        return aggregation.sterms().buckets().array().stream().
                map(StringTermsBucket::key)
                // this would be nicer via elastic "include" feature, however, it seems to be a pain with the java library
                .filter(k -> Arrays.stream(suggest.toLowerCase().split(" ")).allMatch(t -> k.stringValue().toLowerCase().contains(t)))
                .filter(k -> {
                    try {
                        VCard vcard = engine.parse(k.stringValue());
                        if (contributorKind == ContributorKind.ORGANIZATION) {
                            return vcard.getExtendedTypes().stream().map(ExtendedType::getExtendedName).anyMatch(
                                    (e) -> e.equals("X-ROR") || e.equals("X-Wikidata")
                            );
                        } else {
                            return vcard.getExtendedTypes().stream().map(ExtendedType::getExtendedName).anyMatch(
                                    (e) -> e.equals("X-ORCID") || e.equals("X-GND-URI")
                            );
                        }
                    } catch (Exception ignored) {
                        return false;
                    }
                })
                .map((k) -> new SearchVCard(k.stringValue())).
                collect(Collectors.toCollection(HashSet::new));
    }

//
//    public RestHighLevelClient getClient() throws IOException {
//        checkClient();
//        return client;
//    }


    // TODO should we generalize this? Just a dirty hack
    public void deleteUserActivitiesByUsername(String username) {
        try {
            deleteByQuery(DeleteByQueryRequest.of(x -> x
                    .query(q -> q
                            .bool(b -> b
                                    .should(s -> s.match(m -> m.field("userEvent.initiator").query(username)))
                                    .should(s -> s.match(m -> m.field("userEvent.receiver").query(username)))
                            ))));
        } catch (IOException e) {
            logger.error("Could not delete activities for user " + username, e);
            throw new RuntimeException(e);
        }

    }

    public DeleteResponse deleteNative(DeleteRequest deleteRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).delete(deleteRequest);
    }

    public DeleteByQueryResponse deleteByQuery(DeleteByQueryRequest deleteByQueryRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).deleteByQuery(deleteByQueryRequest);
    }

    public SearchResponse<Map> searchNative(SearchRequest searchRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).search(searchRequest, Map.class);
    }

    public UpdateResponse<Map> updateNative(UpdateRequest updateRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).update(updateRequest, Map.class);
    }

    public ScrollResponse<Map> scrollNative(ScrollRequest searchScrollRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).scroll(searchScrollRequest, Map.class);
    }

    public ClearScrollResponse clearScrollNative(ClearScrollRequest clearScrollRequest) throws IOException {
        checkClient();
        return client.withTransportOptions(this::getRequestOptions).clearScroll(clearScrollRequest);
    }

    public void checkClient() throws IOException {
        if (client == null || !client.ping().value()) {
            if (client != null) {
                try {
                    restClient.close();
                } catch (Exception e) {
                    logger.error("ping failed, close failed:" + e.getMessage() + " creating new");
                }
            }
            if (MAX_RESPONSE_ENTITY_SIZE == -1) {
                if (LightbendConfigLoader.get().hasPath("elasticsearch.max_response_entity_size")) {
                    MAX_RESPONSE_ENTITY_SIZE = LightbendConfigLoader.get().getInt("elasticsearch.max_response_entity_size");
                } else {
                    //100 MB
                    MAX_RESPONSE_ENTITY_SIZE = 100 * 1048576;
                }
            }

            // Create the low-level client
            restClient = RestClient
                    .builder(getConfiguredHosts())
                    .setDefaultHeaders(new Header[]{
                            // new BasicHeader("Authorization", "ApiKey " + apiKey)
                    })
                    .build();

            RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
            options.setHttpAsyncResponseConsumerFactory(
                    new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(MAX_RESPONSE_ENTITY_SIZE)
            );
            ElasticsearchTransport transport = new RestClientTransport(
                    restClient, new JacksonJsonpMapper(), new RestClientOptions.Builder(options).build());
            client = new ElasticsearchClient(transport);
        }
    }


    public SearchResultNodeRef getMetadata(List<String> nodeIds) throws IOException {

        SearchResultNodeRef sr = new SearchResultNodeRef();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);

        SearchRequest searchRequest = SearchRequest.of(req -> req
                .index(WORKSPACE_INDEX)
                .from(0)
                .size(nodeIds.size())
                .trackTotalHits(track -> track.enabled(true))
                .query(root -> root
                        .bool(getGlobalConditions(null, null, null)
                                .must(must -> must
                                        .bool(queryNodeIds -> queryNodeIds.minimumShouldMatch("1")
                                                .should(should -> {
                                                    nodeIds.forEach(x -> should.term(t -> t.field("nodeRef.id").value(x)));
                                                    return should;
                                                })))
                                .build())));
        SearchResponse<Map> searchResponse = client.search(searchRequest, Map.class);

        logger.info("query: " + JsonpUtils.toJsonString(searchRequest, new JacksonJsonpMapper()));
        HitsMetadata<Map> hits = searchResponse.hits();
        logger.info("result count: " + hits.total().value());
        boolean isAdmin = AuthorityServiceHelper.isAdmin();

        for (Hit<Map> hit : hits.hits()) {
            data.add(transformSearchHit(isAdmin, getUserAuthorities(), AuthenticationUtil.getFullyAuthenticatedUser(), hit.source(), true));
        }
        sr.setStartIDX(0);
        sr.setNodeCount((int) hits.total().value());
        return sr;
    }

    @Override
    public List<? extends Suggestion> getSuggestions(MetadataSet mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criterias) {
        Map<String, String[]> criteriasMap = MetadataSearchHelper.convertCriterias(criterias);
        SearchToken token = new SearchToken();
        token.setFacets(Collections.singletonList(parameterId));
        token.setFrom(0);
        token.setMaxResult(0);
        token.setFacetLimit(50);
        token.setFacetsMinCount(1);
        token.setQueryString(value);
        try {
            Map<String, MetadataKey> captions = mds.findWidget(parameterId).getValuesAsMap();
            SearchResultNodeRef search = searchFacets(
                    mds, queryId, criteriasMap, token
            );
            if (search.getFacets().size() != 1) {
                return Collections.emptyList();
            }
            return search.getFacets().get(0).getValues().stream().filter(s ->
                    // if one document has i.e. multiple keywords, they will be shown in the facet
                    // so, we filter for values which actually contain the given string
                    s.getValue().toLowerCase().contains(value.toLowerCase())
            ).map(s -> {
                Suggestion suggestion = new Suggestion();
                suggestion.setKey(s.getValue());
                suggestion.setDisplayString(
                        captions.containsKey(s.getValue()) ? captions.get(s.getValue()).getCaption() : s.getValue()
                );
                return suggestion;
            }).distinct().limit(token.getFacetLimit()).collect(Collectors.toList());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public org.edu_sharing.repository.server.SearchResult<SearchUserEvent> getRecentUserEvents(List<ActivityOnNodeEventType> filterByEvent, Map<String, String[]> searchCriteria, SearchToken searchToken) throws Exception {
        String username = AuthenticationUtil.getFullyAuthenticatedUser();
        Query childQuery = Query.of(q2 -> q2.hasChild(hc -> hc
                .type("userEvent")
                .scoreMode(ChildScoreMode.Min)
                .query(cq -> cq.functionScore(fs -> fs
                        .query(q3 -> q3.bool(b -> {
                            b = b.must(m -> m.term(t -> t
                                    .field("userEvent.initiator")
                                    .value(username)
                            ));
                            if (filterByEvent != null && !filterByEvent.isEmpty()) {
                                b.minimumShouldMatch("1");
                                for (ActivityOnNodeEventType activityOnNodeEventType : filterByEvent) {
                                    b = b.should(s -> s.term(t -> t
                                            .field("userEvent.type")
                                            .value(activityOnNodeEventType.name())
                                    ));
                                }
                            }
                            return b;
                        }))
                        // TODO: Filter by event if present!
                        .functions(f -> f.scriptScore(ss -> ss
                                .script(script -> script
                                        .source("decayDateLinear(params.originDate, '1m', '0', 1.5, doc['userEvent.timestamp'].value)")
                                        .params(Map.of("originDate", JsonData.of(Instant.now().toString())
                                                )
                                        )
                                )))
                        .boostMode(FunctionBoostMode.Replace)
                ))
                .innerHits(ih -> ih
                        .name("userEvent")
                        .size(1)
                        .sort(so -> so
                                .field(sf -> sf
                                        .field("userEvent.timestamp")
                                        .order(SortOrder.Desc)
                                )
                        )
                )
        ));
        return searchInternalWithChildQuery("recentUserEvents", searchCriteria, searchToken, childQuery, (data) -> {
            Map userEvent = (Map) data.getInnerHits().get("userEvent").hits().hits().get(0).source().to(Map.class).get("userEvent");
            return new SearchUserEvent(
                    data.getNodeRef(),
                    userEvent.get("initiator").toString(),
                    new Date((Long) userEvent.get("timestamp")),
                    ActivityOnNodeEventType.valueOf(userEvent.get("type").toString())
            );
        });
    }
    @Override
    public org.edu_sharing.repository.server.SearchResult<SearchInviteEvent> getUserShares(UserShareDirection direction, Map<String, String[]> searchCriteria, SearchToken searchToken) throws Exception {
        String username = AuthenticationUtil.getFullyAuthenticatedUser();
        Query childQuery = BoolQuery.of(bool -> bool.must(
                Query.of(q2 -> q2.hasChild(hc -> hc
                        .type("share")
                        .scoreMode(ChildScoreMode.Min)
                        .query(cq -> cq.functionScore(fs -> fs
                                .query(q3 -> q3.bool(b -> {
                                    b = b.must(m -> m.term(t -> t
                                            .field("share.shareStatus")
                                            .value("SHARED")
                                    ));
                                    if (direction.equals(UserShareDirection.fromUser)) {
                                        b = b.must(m -> m.term(t -> t
                                                .field("share.sharedBy")
                                                .value(username)
                                        ));
                                    } else if(direction.equals(UserShareDirection.toUser)) {
                                        b = b.must(m -> m.term(t -> t
                                                .field("share.sharedWith")
                                                .value(username)
                                        ));
                                    } else if(direction.equals(UserShareDirection.toUserOrGroups)) {
                                        b.minimumShouldMatch("1");
                                        for(String group: getAllMemberships(username)) {
                                            b = b.should(m -> m.term(t -> t
                                                    .field("share.sharedWith")
                                                    .value(group)
                                            ));
                                        }
                                    }
                                    return b;
                                }))
                                // TODO: Filter by event if present!
                                .functions(f -> f.scriptScore(ss -> ss
                                        .script(script -> script
                                                .source("decayDateLinear(params.originDate, '1m', '0', 1.5, doc['share.timestamp'].value)")
                                                .params(Map.of("originDate", JsonData.of(Instant.now().toString())
                                                        )
                                                )
                                        )))
                                .boostMode(FunctionBoostMode.Replace)
                        ))
                        .innerHits(ih -> ih
                                .name("share")
                                .size(1)
                                .sort(so -> so
                                        .field(sf -> sf
                                                .field("share.timestamp")
                                                .order(SortOrder.Desc)
                                        )
                                )
                        )
                ))).must(
                // filter rejected
                Query.of(q2 -> q2.hasChild(hc -> hc
                        .type("share")
                        .query(cq -> cq.bool(qcb -> qcb.mustNot(
                                        mn -> mn.bool(mnb -> mnb.must(
                                                mnbm -> mnbm.term(t -> t
                                                        .field("share.sharedWidth")
                                                        .value(username)
                                                )).must(
                                                mnbm -> mnbm.term(t -> t
                                                        .field("share.shareStatus")
                                                        .value("REJECTED")
                                                )
                                        ))
                                ))
                        )
                ))
        ))._toQuery();
        return searchInternalWithChildQuery("shared", searchCriteria, searchToken, childQuery, (data) -> {
            Map share = (Map) data.getInnerHits().get("share").hits().hits().get(0).source().to(Map.class).get("share");
            return new SearchInviteEvent(
                    data.getNodeRef(),
                    share.get("sharedBy").toString(),
                    share.get("sharedWith").toString(),
                    new Date((Long) share.get("timestamp")),
                    ShareInfo.ShareTypeEnum.valueOf(share.get("shareType").toString()),
                    ShareInfo.ShareStatusEnum.valueOf(share.get("shareStatus").toString())
            );
        });
    }
    private<T> org.edu_sharing.repository.server.SearchResult<T> searchInternalWithChildQuery(
            String queryId,
            Map<String, String[]> searchCriteria,
            SearchToken searchToken,
            Query childQuery,
            Function<ResultData,T> mapResult
    ) throws Exception {
        MetadataSet mds = MetadataHelper.getLocalDefaultMetadataset();
        MetadataQueries queries = mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL);
        MetadataQuery queryData = queries.findQuery(queryId);
        String basequery = queryData.getPrimaryBasequery();
        BoolQuery.Builder builder = QueryBuilders.bool()
                .filter(
                        filter -> filter.bool(MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, searchCriteria, true).build())
                )
                .must(
                        must -> must.bool(MetadataElasticSearchHelper.getElasticSearchQuery(searchToken, mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, searchCriteria, false).build())
                ).must(
                        b -> b.wrapper(new ReadableWrapperQueryBuilder(basequery).build())
                ).must(
                        getContentTypeQuery(searchToken.getContentType())
                ).must(
                        must -> must.bool(getGlobalConditions(null, null, null).build())
                ).must(
                        childQuery
                );

        searchToken.setElasticQuery(builder.build());
        searchToken.setSortDefinition(SortDefinition.SORT_DEFINITION_SCORE_ASC);
        SearchResultNodeRef queryResult = search(searchToken, true);
        org.edu_sharing.repository.server.SearchResult<T> result = new org.edu_sharing.repository.server.SearchResult<>();
        ArrayList<T> list = new ArrayList<>();
        int i = 0;
        for (Hit<Map> elasticHit : queryResult.getElasticHits()) {
            Map<String, InnerHitsResult> innerHits = elasticHit.innerHits();
            NodeRef data = queryResult.getData().get(i++);
            list.add(mapResult.apply(new ResultData(innerHits, data)));
        }
        result.setFacets(queryResult.getFacets());

        Map<String, Aggregation> aggregations;
        if (searchToken.getFacets() != null) {
            BoolQuery.Builder queryBuilderGlobalConditions = getGlobalConditions(searchToken.getAuthorityScope(), searchToken.getPermissions(), queryData, null, true);
            queryBuilderGlobalConditions.must(
                    childQuery
            );
            List<NodeSearch.Facet> facetsResult = null;
            SearchResponse<Map> searchResponseAggregations = null;
            Set<MetadataQueryParameter> excludeOwnFacets = MetadataElasticSearchHelper.getExcludeOwnFacets(queryData, searchCriteria, searchToken.getFacets());
            if (!excludeOwnFacets.isEmpty()) {
                Map<String, Aggregation> excludedOwnAggregations = MetadataElasticSearchHelper.getAggregations(
                        mds,
                        queryData,
                        searchCriteria,
                        searchToken.getFacets(),
                        excludeOwnFacets,
                        queryBuilderGlobalConditions.build()._toQuery(),
                        searchToken);

                // remove duplicate facet entries
                excludedOwnAggregations.entrySet().removeIf(e -> e.getKey().endsWith(MetadataElasticSearchHelper.FACET_SELECTED_POSTFIX));

                SearchRequest searchSourceAggs = SearchRequest.of(req -> req
                        .index(WORKSPACE_INDEX)
                        .from(0)
                        .size(0)
                        .aggregations(excludedOwnAggregations));
                searchResponseAggregations = client.search(searchSourceAggs, Map.class);
                facetsResult = getFacets(mds, queryData, excludedOwnAggregations, searchResponseAggregations);
                result.setFacets(facetsResult);
                aggregations = null;
            }
        }

        result.setData(list);
        result.setStartIDX(queryResult.getStartIDX());
        result.setNodeCount(queryResult.getNodeCount());
        return result;
    }

    @Override
    public SearchResultNodeRef getFilesSharedByMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
        BoolQuery.Builder query = QueryBuilders.bool()
                .must(
                        SearchServiceElastic.getFilesSharedByMeQuery(MetadataHelper.getLocalDefaultMetadataset().getQueries(MetadataReader.QUERY_SYNTAX_DSL))._toQuery()
                ).must(
                        getContentTypeQuery(contentType)
                ).must(
                        must -> must.bool(getGlobalConditions(null, null, null).build())
                );
        return searchByQuery(query.build(), skipCount, maxItems, sortDefinition);
    }

    @Override
    public SearchResultNodeRef getFilesSharedToMe(SharedToMeType type, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
        BoolQuery.Builder query = QueryBuilders.bool()
                .must(
                        SearchServiceElastic.getFilesSharedToMeQuery(MetadataHelper.getLocalDefaultMetadataset().getQueries(MetadataReader.QUERY_SYNTAX_DSL), type)._toQuery()
                ).must(
                        getContentTypeQuery(contentType)
                ).must(
                        must -> must.bool(getGlobalConditions(null, null, null).build())
                );
        return searchByQuery(query.build(), skipCount, maxItems, sortDefinition);
    }

    @Override
    public SearchResultNodeRef getWorkflowReceive(String user, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
        BoolQuery.Builder builder = QueryBuilders.bool();
        builder.minimumShouldMatch("1");
        getAllMemberships(user).forEach(authority -> builder.should(b -> b.match(m -> m.field(
                "properties.ccm:wf_receiver.keyword").query(authority)))
        );
        builder.mustNot(b -> b.exists(e -> e.field(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL))));
        builder.mustNot(b -> b.match(m -> m.field("aspects").query(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))));
        builder.must(getContentTypeQuery(contentType));
        builder.must(
                must -> must.bool(getGlobalConditions(null, null, null).build())
        );
        MetadataQueries queries = MetadataHelper.getLocalDefaultMetadataset().getQueries(MetadataReader.QUERY_SYNTAX_DSL);
        String basequery = queries.findQuery("workflowReceive").getPrimaryBasequery();
        if (StringUtils.isNotBlank(basequery)) {
            builder.must(b -> b.wrapper(new ReadableWrapperQueryBuilder(basequery).build()));
        }
        return searchByQuery(builder.build(), skipCount, maxItems, sortDefinition);
    }

    Query getContentTypeQuery(ContentType contentType) {
        if (contentType == null || contentType.equals(ContentType.ALL)) {
            return QueryBuilders.matchAll().build()._toQuery();
        }
        SearchToken token = new SearchToken();
        token.setContentType(contentType);
        BoolQuery.Builder builder = QueryBuilders.bool();
        if(contentType.equals(ContentType.FILES)) {
            builder.must(m -> m.match(match -> match.field("type").query(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO))));
        } else if(contentType.equals(ContentType.FOLDERS)) {
            builder.must(m -> m.match(match -> match.field("type").query(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP))));
            builder.mustNot(m -> m.match(match -> match.field("aspects").query(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION))));
        } else if(contentType.equals(ContentType.COLLECTIONS)) {
            builder.must(m -> m.match(match -> match.field("type").query(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP))));
            builder.must(m -> m.match(match -> match.field("aspects").query(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION))));
        } else if(contentType.equals(ContentType.FILES_AND_FOLDERS)) {
            builder.minimumShouldMatch("1");
            builder.should(
                    s -> s.bool(
                            b -> b.must(m -> m.match(match -> match.field("type").query(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP)))).
                                    mustNot(m -> m.match(match -> match.field("aspects").query(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION))))
                    )).should(s -> s.match(match -> match.field("type").query(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO)))
            );
        } else if(contentType.equals(ContentType.COLLECTION_PROPOSALS)) {
            builder.must(m -> m.match(match -> match.field("type").query(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_COLLECTION_PROPOSAL))));
        } else if(contentType.equals(ContentType.TOOLPERMISSIONS)) {
            builder.must(m -> m.match(match -> match.field("type").query(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_TOOLPERMISSION))));
        } else {
            logger.warn("Unsupported/Unknown content type: " + contentType);
        }
        return builder.build()._toQuery();
    }

    private SearchResultNodeRef searchByQuery(QueryVariant query, int skipCount, int maxItems, SortDefinition sortDefinition) throws IOException {
        return searchByQuery(query, skipCount, maxItems, sortDefinition, null,null);
    }

    private SearchResultNodeRef searchByQuery(QueryVariant query, int skipCount, int maxItems, SortDefinition sortDefinition, String index, Map<String,Aggregation> aggregations) throws IOException {
        if(index == null) index = WORKSPACE_INDEX;

        if((maxItems - skipCount) > 10000){
            return  searchAllByQuery(query, sortDefinition, index,aggregations);
        }
        checkClient();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder().index(index);
        searchRequestBuilder.query(query._toQuery());
        searchRequestBuilder.from(skipCount);
        searchRequestBuilder.size(maxItems);
        searchRequestBuilder.trackTotalHits(new TrackHits.Builder().enabled(true).build());
        searchRequestBuilder.source(src -> src
                .filter(filter -> filter.excludes(appendDefaultExcludes(new ArrayList<>())))
        );
        if(aggregations != null){
            searchRequestBuilder.aggregations(aggregations);
        }
        if (sortDefinition != null) {
            sortDefinition.applyToSearchSourceBuilder(searchRequestBuilder);
        }
        SearchRequest searchRequest = searchRequestBuilder.build();
        SearchResponse<Map> searchResponse = client.search(searchRequest, Map.class);
        HitsMetadata<Map> hits = searchResponse.hits();
        Set<String> authorities = getUserAuthorities();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
        boolean isAdmin = AuthorityServiceHelper.isAdmin();
        SearchResultNodeRef sr = new SearchResultNodeRef();
        sr.setElasticHits(hits.hits());
        sr.setData(hits.hits().stream().map(h -> transformSearchHit(isAdmin, authorities, user, h.source(), false)).collect(Collectors.toList()));
        sr.setStartIDX(skipCount);
        sr.setNodeCount((int) hits.total().value());

        if(aggregations != null){
            sr.setFacets(getFacets(null,null, aggregations,searchResponse));
        }

        return sr;
    }

    @Override
    public SearchResult<String> findAuthorities(AuthorityType type, String searchWord, boolean globalContext, int from, int nrOfResults, SortDefinition sort, Map<String, String> customProperties) throws Exception {
        String signupMethod = customProperties == null ? null : customProperties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD));
        boolean searchingSignupGroups = ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_SIGNUP_GROUP) &&
                AuthorityType.GROUP.equals(type) &&
                signupMethod != null &&
                !signupMethod.isEmpty();
        if (globalContext && !searchingSignupGroups) {
            checkGlobalSearchPermission();
        }

        // fields to search in - also using username as admin (6.0 or later)

        //org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(null);

        BoolQuery.Builder findUsersQuery = getFindUsersSearch(searchWord, AuthorityServiceHelper.getDefaultAuthoritySearchFields(), globalContext);
        // we're skipping TP checks when the search requested signup groups -> it's possible to see them even without GLOBAL_AUTHORITY_SEARCH permissions
        //StringBuffer findGroupsQuery = permissionService.getFindGroupsSearchString(searchWord, globalContext, searchingSignupGroups);
        BoolQuery.Builder findGroupsQuery = getFindGroupsSearch(searchWord, globalContext, searchingSignupGroups);

        if (findUsersQuery == null && findGroupsQuery == null) {
            return new SearchResult<String>(new ArrayList<>(), 0, 0);
        }

        /**
         * don't find groups of scopes when no scope is provided
         */
        if (NodeServiceInterceptor.getEduSharingScope() == null && findGroupsQuery != null) {

            /**
             * groups arent initialized with eduscope aspect and eduscopename
             * null
             */
            findGroupsQuery.mustNot(mn -> mn.exists(e -> e.field("properties.ccm:eduscopename")));
        }

        BoolQuery.Builder finalQuery = getAuthorityCombinedQuery(type, customProperties, findUsersQuery, findGroupsQuery);
        if (!finalQuery.hasClauses())
            return new SearchResult<String>();


        //logger.debug("finalQuery:" + finalQuery.build().toString());

        List<String> result = new ArrayList<>();


        try {


            SearchResultNodeRef searchResultNodeRef = this.searchByQuery(finalQuery.build(), from, nrOfResults, sort, AUTHORITIES_INDEX,null);

            searchResultNodeRef.getData().stream().forEach(c -> {
                String authorityName = (String) c.getProperties().get(CCConstants.CM_PROP_AUTHORITY_NAME);
                if (authorityName == null) {
                    authorityName = (String) c.getProperties().get(CCConstants.CM_PROP_PERSON_USERNAME);
                }
                result.add(authorityName);
            });
            return new SearchResult<String>(result, from, searchResultNodeRef.getNodeCount());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static BoolQuery.Builder getAuthorityCombinedQuery(AuthorityType type, Map<String, String> customProperties, BoolQuery.Builder findUsersQuery, BoolQuery.Builder findGroupsQuery) {
        BoolQuery.Builder finalQuery = QueryBuilders.bool().minimumShouldMatch("1");
        if (findUsersQuery != null && customProperties != null) {
            for(Map.Entry<String, String> entry : customProperties.entrySet().stream().filter(k -> Objects.equals(CCConstants.getValidGlobalName(k.getKey()), CCConstants.CM_PROP_PERSON_ESPERSONSTATUS)).collect(Collectors.toList())){
                findUsersQuery.must(m -> m.wildcard(t -> t
                        .field("properties." + entry.getKey() + ".keyword")
                        .value(entry.getValue())
                ));
            }
        }
        if (findGroupsQuery != null && customProperties !=null) {
            for(Map.Entry<String, String> entry : customProperties.entrySet()){
                findGroupsQuery.must(m -> m.wildcard(t -> t
                        .field("properties." + entry.getKey() + ".keyword")
                        .value(entry.getValue())
                ));
            }
        }

        if (type == null) {
            if (findUsersQuery != null) {
                finalQuery.should(s -> s.bool(findUsersQuery.build()));
            }
            if (findGroupsQuery != null) {
                finalQuery.should(s -> s.bool(findGroupsQuery.build()));
            }
        } else if (type.equals(AuthorityType.USER)) {
            finalQuery = findUsersQuery;

        } else if (type.equals(AuthorityType.GROUP)) {
            if (findGroupsQuery != null)
                finalQuery = findGroupsQuery;
        } else {
            throw new IllegalArgumentException("Unsupported authority type " + type);
        }
        return finalQuery;
    }


    BoolQuery.Builder getFindUsersSearch(String query, Map<String, Double> searchFields, boolean globalContext) {
        ToolPermissionService toolPermissionService = ToolPermissionServiceFactory.getInstance();
        boolean fuzzyUserSearch = !globalContext || toolPermissionService
                .hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);


        BoolQuery.Builder subQuery = QueryBuilders.bool();

        if (fuzzyUserSearch) {
            if (query != null) {
                for (String token : StringTool.getPhrases(query)) {

                    boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

                    if (isPhrase) {

                        token = (token.length() > 2) ? token.substring(1, token.length() - 1) : "";

                    } else {

                        if (!(token.startsWith("*") || token.startsWith("?"))) {
                            token = "*" + token;
                        }

                        if (!(token.endsWith("*") || token.endsWith("?"))) {
                            token = token + "*";
                        }
                    }
                    BoolQuery.Builder fieldQuery = QueryBuilders.bool().minimumShouldMatch("1");
                    for (Map.Entry<String, Double> field : searchFields.entrySet()) {
                        String fToken = token;
                        fieldQuery.should(s -> s.wildcard(w -> w
                                .field("properties.cm:" + field.getKey()+".keyword")
                                .value(fToken)
                                .caseInsensitive(true))
                        );
                        if (field.getValue() > 1) {
                            fieldQuery.should(s -> s.wildcard(w -> w
                                    .field("properties.cm:" + field.getKey()+".keyword")
                                    .value(StringUtils.strip(fToken, "*"))
                                    .boost(field.getValue().floatValue())
                                    .caseInsensitive(true)));
                        }
                    }
                    subQuery.must(m -> m.bool(fieldQuery.build()));
                }
            }
        } else {

            // when no fuzzy search remove "*" from searchstring and remove all params
            // except email

            String emailValue = query;

            // remove wildcards (*,?)
            if (emailValue != null) {
                emailValue = emailValue.replaceAll("[*?]", "");
            }

            for (String token : StringTool.getPhrases(emailValue)) {

                boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

                if (isPhrase) {
                    token = (token.length() > 2) ? token.substring(1, token.length() - 1) : "";
                }

                if (!token.isEmpty()) {
                    String ftoken = token;
                    subQuery.must(m -> m.match(ma -> ma.field("properties.cm:email.keyword").query(ftoken)));
                }
            }

            // if not fuzzy and no value for email return empty result
            if (!subQuery.hasClauses()) {
                return null;
            }
        }

        /**
         * global / groupcontext search
         */
        BoolQuery.Builder searchQuery = QueryBuilders.bool()
                .must(m -> m.term(t -> t.field("type").value("cm:person")));

        boolean hasToolPermission = toolPermissionService
                .hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH);
        boolean hasFuzzyToolPermission = toolPermissionService
                .hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);

        if (globalContext) {

            if (!hasToolPermission) {
                return null;
            }
            addGlobalAuthoritySearchQuery(searchQuery);

        } else {

            List<String> eduGroupAuthorityNames = eduPermissionService.getOrganizationsOfUser();

            /**
             * if there are no edugroups you you are not allowed to search global return
             * nothing
             */
            if (eduGroupAuthorityNames.isEmpty()) {
                if (!hasToolPermission || !hasFuzzyToolPermission) {
                    return null;
                }
                return getFindUsersSearch(query, searchFields, true);
            }

            BoolQuery.Builder groupPathQuery = QueryBuilders.bool();
            for (String eduGroup : eduGroupAuthorityNames) {
                addAuthorityFullPathQuery(eduGroup, groupPathQuery);
            }

            if (groupPathQuery.hasClauses()) {
                searchQuery.must(m -> m.bool(groupPathQuery.build()));
            }
        }
        if (!AuthorityServiceHelper.isAdmin()) {
            // allow the access to the guest user for admin
            filterGuestAuthority(searchQuery);
        }

        if (subQuery.hasClauses()) {
            searchQuery.must(m -> m.bool(subQuery.build()));
        }

        //cm:espersonstatus
        if (!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")
                && !AuthorityServiceFactory.getLocalService().isGlobalAdmin()) {
            String personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
            searchQuery.must(m -> m.term(t -> t.field("properties.cm:espersonstatus.keyword").value(personActiveStatus)));
        }

        /**
         * filter out remote users
         */
        String homeRepo = ApplicationInfoList.getHomeRepository().getAppId();
        searchQuery.must(m -> m.bool(b -> b
                .minimumShouldMatch("1")
                .should(s -> s.bool(b2 -> b2.mustNot(mn -> mn.exists(e -> e.field("properties.cm:repositoryid")))))
                // @TODO check if needed
                //.should(s -> s.term(t -> t.field("properties.cm:repositoryid").value((String)null)))
                .should(s -> s.term(t -> t.field("properties.cm:repositoryid").value(homeRepo)))
        ));

        //logger.info("findUsers: " + searchQuery.build().toString());

        return searchQuery;
    }


    private void addGlobalAuthoritySearchQuery(BoolQuery.Builder searchQuery) {
        if (NodeServiceInterceptor.getEduSharingScope() == null)
            return;
        try {
            // fetch all groups which are allowed to acces confidential and
            String nodeId = ToolPermissionServiceFactory.getInstance().getToolPermissionNodeId(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL, true);
            BoolQuery.Builder groupPathQuery = QueryBuilders.bool();
            // user may not has ReadPermissions on ToolPermission, so fetch as admin
            ACL permissions = AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<ACL>() {
                @Override
                public ACL doWork() throws Exception {
                    return PermissionServiceFactory.getLocalService().getPermissions(nodeId);
                }
            });
            for (ACE ace : permissions.getAces()) {

                //@TODO get sys:system, sys:authorities nodeID's and use them instead of *
                addAuthorityFullPathQuery(ace.getAuthority(), groupPathQuery);
            }
            if (!groupPathQuery.hasClauses()) {
                throw new IllegalArgumentException("Global search failed for scope, there were no groups found on the toolpermission " + CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL);
            }
            searchQuery.must(m -> m.bool(groupPathQuery.build()));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void addAuthorityFullPathQuery(String authorityName, BoolQuery.Builder groupPathQuery) {
        org.alfresco.service.cmr.repository.NodeRef authorityNodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(authorityName);
        // /sys:system/sys:authorities/cm:GROUP_testGruppe/*
        //groupPathQuery.should(s -> s.wildcard(w -> w.field("fullpath").value("*/*/"+authorityNodeRef.getId()+"/*")));
        groupPathQuery.should(s -> s.wildcard(w -> w
                .field("fullpaths")
                .value(rootHomeId + "/" + sysSystemNodeId + "/" + sysAuthoritiesNodeId + "/" + authorityNodeRef.getId() + "*")
        ));
    }

    private void filterGuestAuthority(BoolQuery.Builder searchQuery) {
        for (String guest : guestService.getAllGuestAuthorities()) {
            searchQuery.mustNot(mn -> mn.term(t -> t
                    .field("properties.cm:userName.keyword")
                    .value(guest)));
        }
    }


    public BoolQuery.Builder getFindGroupsSearch(String searchWord, boolean globalContext, boolean skipTpCheck) {
        boolean fuzzyGroupSearch = skipTpCheck || !globalContext || ToolPermissionServiceFactory.getInstance()
                .hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);

        //StringBuffer searchQuery = new StringBuffer("TYPE:cm\\:authorityContainer AND NOT @ccm\\:scopetype:system");
        BoolQuery.Builder searchQuery = QueryBuilders.bool()
                .must(m -> m.term(t -> t.field("type").value("cm:authorityContainer")))
                .mustNot(mn -> mn.term(t -> t.field("properties.ccm:scopetype.keyword:").value("system")));

        searchWord = searchWord != null ? searchWord.trim() : "";

        BoolQuery.Builder subQuery = QueryBuilders.bool();
        if (fuzzyGroupSearch) {
            if (("*").equals(searchWord)) {
                searchWord = "";
            }
            if (!searchWord.isEmpty()) {


                for (String token : StringTool.getPhrases(searchWord)) {

                    boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

                    if (isPhrase) {

                        token = (token.length() > 2) ? token.substring(1, token.length() - 1) : "";

                    } else {

                        if (!(token.startsWith("*") || token.startsWith("?"))) {
                            token = "*" + token;
                        }

                        if (!(token.endsWith("*") || token.endsWith("?"))) {
                            token = token + "*";
                        }
                    }

                    if (!token.isEmpty()) {
                        String ftoken = token;
                        BoolQuery.Builder fieldQuery = QueryBuilders.bool().minimumShouldMatch("1");
                        fieldQuery.should(s -> s.wildcard(w -> w
                                .field("properties.cm:authorityDisplayName.keyword")
                                .value(StringUtils.strip(ftoken, "*"))
                                .boost((float) 10.0)
                                .caseInsensitive(true)
                        )).should(s -> s.wildcard(w -> w
                                .field("properties.cm:authorityDisplayName.keyword")
                                .value(ftoken)
                                .caseInsensitive(true)
                        )).should(s -> s.wildcard(w -> w
                                .field("properties.ccm:groupEmail.keyword")
                                .value(ftoken)
                                .caseInsensitive(true)
                        ));

                        if (eduPermissionService.isAdminOrSystem()) {
                            fieldQuery.should(s -> s.wildcard(w -> w
                                    .field("properties.cm:authorityName.keyword")
                                    .value(ftoken)
                                    .caseInsensitive(true)
                            ));
                        }
                        subQuery.must(f -> f.bool(fieldQuery.build()));
                    }
                }
            }
        } else {

            // remove wildcards (*,?)
            searchWord = searchWord.replaceAll("[*?]", "");

            String token = searchWord;
            boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

            if (isPhrase) {
                token = (token.length() > 2) ? token.substring(1, token.length() - 1) : "";
            }

            if (!token.isEmpty()) {
                String nonFuzzyField = LightbendConfigLoader.get().getString("repository.search.groups.nonFuzzyField");
                if (nonFuzzyField.startsWith("=@")) {
                    nonFuzzyField = nonFuzzyField.replace("=@", "");
                }
                String fnonFuzzyField = "properties." + nonFuzzyField+".keyword";
                String ftoken = token;
                subQuery.must(m -> m.wildcard(w -> w.
                        field(fnonFuzzyField)
                        .value(ftoken)
                        .caseInsensitive(true)
                ));
            }

            // if not fuzzy and no value for email return empty result
            if (!subQuery.hasClauses()) {
                return null;
            }
        }
        if (subQuery.hasClauses()) {
            searchQuery.must(m -> m.bool(subQuery.build()));
        }

        ToolPermissionService toolPermission = ToolPermissionServiceFactory.getInstance();
        boolean hasToolPermission = skipTpCheck || toolPermission
                .hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH);
        boolean hasFuzzyToolPermission = skipTpCheck || toolPermission
                .hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);

        if (globalContext) {
            if (!hasToolPermission) {
                return null;
            }
            addGlobalAuthoritySearchQuery(searchQuery);
        } else {

            List<String> eduGroupAuthorityNames = eduPermissionService.getOrganizationsOfUser();

            /**
             * if there are no edugroups you you are not allowed to search global return
             * nothing
             */
            if (eduGroupAuthorityNames.isEmpty()) {
                if (!hasToolPermission || !hasFuzzyToolPermission) {
                    return null;
                }
            }

            BoolQuery.Builder groupPathQuery = QueryBuilders.bool();
            for (String eduGroup : eduGroupAuthorityNames) {
                addAuthorityFullPathQuery(eduGroup, groupPathQuery);
            }

            if (groupPathQuery.hasClauses()) {
                searchQuery.must(m -> m.bool(groupPathQuery.build()));
            }
        }
        if (!eduPermissionService.isAdminOrSystem()) {
            searchQuery.mustNot(mn -> mn.bool(b -> b
                    .should(s -> s.term(t -> t.field("properties.cm:authorityName.keyword").value(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)))
                    .should(s -> s.term(t -> t.field("properties.cm:authorityName.keyword").value(CCConstants.AUTHORITY_GROUP_EMAIL_CONTRIBUTORS)))
            ));
        }

        //logger.info("findGroups: " + searchQuery);

        return searchQuery;
    }

    /**
     * @param membershipsOnly (only for admin/system) when true, behave like regular users and show only groups
     *                        false will show all mz of the system
     * @return
     * @throws Exception
     */
    @Override
    public List<NodeRef> getAllMediacentersNodeRef(boolean membershipsOnly) throws Exception {


        Set<String> memberships = serviceRegistry.getAuthorityService().getAuthorities();
        boolean isSystemUser = AuthenticationUtil.isRunAsUserTheSystemUser();
        boolean isAdmin = ((memberships != null && memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS))
                || "admin".equals(AuthenticationUtil.getFullAuthentication().getName())
                || isSystemUser) ? true : false;


        if (isAdmin && !membershipsOnly) {
            BoolQuery.Builder finalQuery = QueryBuilders.bool()
                    .must(must -> must
                            .match(match -> match
                                    .field("nodeRef.storeRef.protocol")
                                    .query("workspace")))
                    .must(must -> must
                            .match(match -> match
                                    .field("properties.ccm:groupType.keyword")
                                    .query(org.edu_sharing.alfresco.service.AuthorityService.MEDIA_CENTER_GROUP_TYPE)));

            SortDefinition sort = new SortDefinition();
            sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME), true));
            return this.searchAllByQuery(
                    finalQuery.build(),
                    sort,
                    AUTHORITIES_INDEX,null).getData();
        } else {
            assert memberships != null;
            return memberships.stream().map(m -> {
                org.alfresco.service.cmr.repository.NodeRef nodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(m);
                if (nodeRef != null && serviceRegistry.getNodeService().hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_MEDIACENTER))) {
                    NodeRef ref = new NodeRefImpl(nodeRef);
                    ref.setProperties(new HashMap<>() {{
                        put(CCConstants.CM_PROP_AUTHORITY_NAME, m);
                    }});
                    return ref;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }

    }

    private SearchResultNodeRef searchAllByQuery(QueryVariant query, SortDefinition sortDefinition, String index, Map<String,Aggregation> aggregations) throws IOException {
        checkClient();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder().index(index)
                .query(query._toQuery())
                .scroll(Time.of(time -> time.time("60s")))
                .trackTotalHits(new TrackHits.Builder().enabled(true).build())
                .source(src -> src
                        .filter(filter -> filter.excludes(appendDefaultExcludes(new ArrayList<>())))
                );
        if(aggregations != null){
            searchRequestBuilder.aggregations(aggregations);
        }
        if (sortDefinition != null) {
            sortDefinition.applyToSearchSourceBuilder(searchRequestBuilder);
        }
        SearchToken token = new SearchToken();
        SearchResultNodeRef sr  = fetchAllFromRequest(null,null,token, searchRequestBuilder,aggregations);

        return sr;
    }

    public List<NodeRef> getAllPinnedCollections() throws IOException {
        BoolQuery.Builder b = QueryBuilders.bool();
        b.must(m -> m.term(t -> t.field("aspects").value(CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION_PINNED))));
        b.must(m -> m.term(t -> t.field("nodeRef.storeRef.protocol").value("workspace")));
        return searchAllByQuery(b.build(), null, WORKSPACE_INDEX,null).getData();
    }

    @Override
    public List<NodeRef> getReferenceObjects(String nodeId) throws IOException {
        //"ASPECT:\"ccm:collection_io_reference\" AND @ccm\\:original:" + QueryParser.escape(nodeId) + " AND NOT @sys\\:node-uuid:" + QueryParser.escape(nodeId)
        BoolQuery.Builder globalConditions = getGlobalConditions(null, null, null);
        BoolQuery.Builder b = QueryBuilders.bool();
        b.must(m -> m.term(
                t -> t.field("aspects").value("ccm:collection_io_reference"))
        ).must(m -> m.term(
                t -> t.field("properties.ccm:original").value(nodeId)
        )).mustNot(mn -> mn.term(
                t -> t.field("properties.sys:node-uuid").value(nodeId)
        ));
        globalConditions.must(m -> m.bool(b.build()));
        return searchAllByQuery(globalConditions.build(), null, WORKSPACE_INDEX,null).getData();
    }

    @Override
    public SearchResultNodeRef getRelevantNodes(int skipCount, int maxItems) throws Throwable {
        MetadataSet mds = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id);
        Map<String, String[]> criteria = SearchRelevancyTool.getCriteria();
        if (criteria.isEmpty()) {
            return new SearchResultNodeRefElastic();
        }
        SearchToken token = new SearchToken();
        token.setFrom(skipCount);
        token.setMaxResult(maxItems);
        return this.search(mds, "stream_relevant", criteria, token);
    }


    @Override
    public SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort, boolean scoped, boolean onlyMemberShips)
            throws Exception {
        try {
            String searchPattern = pattern == null ? "" : pattern;


            Set<String> memberships;
            {
                Set<String> m = serviceRegistry.getAuthorityService().getAuthorities();
                if(m == null) {
                    m = Collections.emptySet();
                }
                memberships = m;
            }
            boolean isAdmin = (memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)
                    || "admin".equals(AuthenticationUtil.getFullAuthentication().getName())) ? true : false;

            return AuthenticationUtil.runAsSystem(() -> {
                try {
                    BoolQuery.Builder finalQuery = QueryBuilders.bool()
                            .minimumShouldMatch("1")
                            .should(should -> should
                                    .wildcard(wildcard -> wildcard
                                            .caseInsensitive(true)
                                            .field("properties.cm:authorityName.keyword")
                                            .value("*" + searchPattern + "*")))
                            .should(should -> should
                                    .wildcard(wildcard -> wildcard
                                            .caseInsensitive(true)
                                            .field("properties.cm:authorityDisplayName.keyword")
                                            .value("*" + searchPattern + "*")))
                            .must(must -> must
                                    .match(match -> match
                                            .field("nodeRef.storeRef.protocol")
                                            .query("workspace")))
                            .must(must -> must
                                    .wildcard(exists -> exists
                                            .field("properties.ccm:edu_homedir.keyword")
                                            .value("workspace://*")
                                    ));

                    //only search organisations the curren user is in,except: its adminuser and onlyMemberShips == true
                    if (onlyMemberShips) {
                        BoolQuery.Builder memberQuery = QueryBuilders.bool().minimumShouldMatch("1");
                        if (!memberships.isEmpty()) {
                            for (String membershib : memberships) {
                                org.alfresco.service.cmr.repository.NodeRef authorityNodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(membershib);
                                if (authorityNodeRef != null) {
                                    if (serviceRegistry.getNodeService().hasAspect(authorityNodeRef,
                                            QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {
                                        memberQuery.should(should -> should
                                                .match(match -> match
                                                        .field("properties.cm:authorityName.keyword")
                                                        .query(membershib)
                                                )
                                        );

                                    }
                                }
                            }
                            if(!memberQuery.hasClauses()){
                                return new SearchResult<EduGroup>();
                            }
                            finalQuery.must(must -> must.bool(memberQuery.build()));

                        }
                    } else if (!isAdmin) {
                        // seems not necessary since we filter by user groups anyway
                        // + this will also hide any groups in the user manager for org admins
                        // additionalQuery.append(" AND NOT ISNULL:\"ccm:group_signup_method\"");
                    }

                    SearchResultNodeRef eduGroups = this.searchByQuery(
                            finalQuery.build(),
                            skipCount,
                            maxValues,
                            sort,
                            AUTHORITIES_INDEX,null);
                    // do in transaction for better performance of getProperty
                    return serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
                        List<EduGroup> result = new ArrayList<>();
                        for (NodeRef row : eduGroups.getData()) {
                            Map<String, Object> entry = row.getProperties();
                            String nodeRef = (String) entry.get(CCConstants.CCM_PROP_AUTHORITYCONTAINER_EDUHOMEDIR);
                            // when a group folder relation is removed the noderef can be null cause of async solr refresh
                            if (nodeRef != null) {
                                String nodeId = nodeRef.replace("workspace://SpacesStore/", "");
                                EduGroup eduGroup = new EduGroup();
                                eduGroup.setGroupId((String) entry.get(CCConstants.SYS_PROP_NODE_UID));
                                eduGroup.setGroupname((String) entry.get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
                                eduGroup.setGroupDisplayName(
                                        (String) entry.get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));

                                eduGroup.setGroupDisplayName(
                                        (String) entry.get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));

                                eduGroup.setFolderId(nodeId);
                                try {
                                    org.alfresco.service.cmr.repository.NodeRef folderRef = new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
                                    eduGroup.setFolderName(NodeServiceHelper.getProperty(folderRef, CCConstants.CM_NAME));
                                    eduGroup.setScope(NodeServiceHelper.getProperty(folderRef, CCConstants.CCM_PROP_EDUSCOPE_NAME));
                                }catch(Throwable t) {
                                    logger.warn("Exception while fetching edu organization folder for " + eduGroup.getGroupId() + "(folder: " + nodeId + ")", t);
                                }
                                boolean add = false;
                                for (String group : memberships) {
                                    if (group.equals(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)
                                            || group.equals(eduGroup.getGroupname())) {
                                        add = true;
                                        break;
                                    }
                                }
                                if (scoped) {
                                    String currentScope = NodeServiceInterceptor.getEduSharingScope();
                                    if (eduGroup.getScope() == null && currentScope != null)
                                        add = false;
                                    if (eduGroup.getScope() != null && !eduGroup.getScope().equals(currentScope))
                                        add = false;
                                }
                                if (add) {
                                    result.add(eduGroup);
                                }
                            }
                        }
                        int count = memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS) ? eduGroups.getNodeCount() : result.size();
                        return new SearchResult<>(result, skipCount, count);
                    });

                } catch (Throwable t) {
                    throw new Exception(t);
                }
            });
        } catch (Throwable t) {
            throw t;
        }
    }

    public SearchResultNodeRef searchByProperty(
            SearchToken searchToken,
            SearchService.CombineMode combineMode,
            List<String> properties,
            List<String> value,
            List<String> comparator) throws IOException {

        BoolQuery.Builder b = QueryBuilders.bool();
        for(int i = 0; i < properties.size(); i++){
            int finalI = i;
            String comp = "=";
            if (comparator != null)
                comp = comparator.get(i);

            QueryVariant query;
            if (comp.equals("<=")) {
                RangeQuery.Builder r = QueryBuilders.range();
                r.number(n -> n.field("properties." + properties.get(finalI)+".number").to(Double.valueOf(value.get(finalI))));
                query = r.build();
            }else if (comp.equals(">=")) {
                RangeQuery.Builder r = QueryBuilders.range();
                r.number(n -> n.field("properties." + properties.get(finalI)+".number").from(Double.valueOf(value.get(finalI))));
                query = r.build();
            }else{
                TermQuery.Builder t = QueryBuilders.term();
                t.field("properties." + properties.get(finalI)).value(value.get(finalI));
                query = t.build();
            }

            if(CombineMode.AND.equals(combineMode)){
                if(query instanceof RangeQuery){
                    b.must(m -> m.range((RangeQuery)query));
                }else if(query instanceof TermQuery){
                    b.must(m -> m.term((TermQuery)query));
                }
            }else{
                if(query instanceof RangeQuery){
                    b.should(m -> m.range((RangeQuery)query));
                }else if(query instanceof TermQuery){
                    b.should(m -> m.term((TermQuery)query));
                }
            }
        }
        BoolQuery.Builder globalConditions = getGlobalConditions(null, null, null, null,true);
        globalConditions.must(m -> m.bool(b.build()));

        if((searchToken.getMaxResult() - searchToken.getFrom()) > 10000){
            return  searchAllByQuery(globalConditions.build(), searchToken.getSortDefinition(), WORKSPACE_INDEX,null);
        }else {
            return searchByQuery(globalConditions.build(), searchToken.getFrom(), searchToken.getMaxResult(), searchToken.getSortDefinition());
        }
    }

    @Override
    public SearchResultNodeRef search(SearchToken searchToken, boolean scoped) {
        if(searchToken.getElasticQuery() == null && searchToken.getLuceneString() != null){
            return super.search(searchToken,scoped);
        }
        try {
            StoreRef storeRef = (searchToken.getStoreProtocol() != null && searchToken.getStoreName() != null)
                    ? new StoreRef(searchToken.getStoreProtocol(),searchToken.getStoreName())
                    : null;
            BoolQuery.Builder globalConditions = getGlobalConditions(searchToken.getAuthorityScope(), null, null,storeRef,scoped);
            globalConditions.must(searchToken.getElasticQuery()._toQuery());

            Map<String,Aggregation> aggregations = null;
            if(searchToken.getFacets() != null && !searchToken.getFacets().isEmpty()){
                aggregations = searchToken.getFacets()
                        .stream()
                        .collect(Collectors.toMap(s -> s,s ->
                                AggregationBuilders.terms()
                                        .field((MetadataElasticSearchHelper.nonKeywordFacets.contains(s)) ? "properties."+s : "properties."+s+".keyword")
                                        .size(searchToken.getFacetLimit())
                                        .minDocCount(searchToken.getFacetsMinCount())
                                        .build()._toAggregation()));

            }
            return searchByQuery(globalConditions.build(), searchToken.getFrom(), searchToken.getMaxResult(), searchToken.getSortDefinition(),searchToken.getElasticIndex(),aggregations);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchResultNodeRef searchByDisplayPath(String path, String index) throws IOException {
        BoolQuery.Builder globalConditions = getGlobalConditions(null, null, null, null,true);
        globalConditions.must(m -> m.wildcard(QueryBuilders.wildcard()
                .field("fulldisplaypath")
                .value(path)
                .build()
        ));
        return searchAllByQuery(globalConditions.build(),null,index,null);
    }

    @Data
    @AllArgsConstructor
    private static class ResultData {
        private Map<String, InnerHitsResult> innerHits;
        private NodeRef nodeRef;
    }
}
