package org.edu_sharing.service.nodeservice;


import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.QueryUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.admin.RepositoryConfigFactory;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.search.ReadableWrapperQueryBuilder;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.edu_sharing.service.search.SearchServiceElastic.WORKSPACE_INDEX;

public class NodeFrontpage {

    private static final String STATISTIC_SORT_SCRIPT = SearchServiceElastic.loadScript("frontpage-ratings.properties");

    private final SearchServiceElastic searchServiceElastic;
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
    private final CollectionService collectionService;

    public NodeFrontpage(){
        this(ApplicationContextFactory.getApplicationContext().getBean(SearchServiceElastic.class),
                CollectionServiceFactory.getInstance().getLocalService());
    }

    NodeFrontpage(SearchServiceElastic searchServiceElastic, CollectionService collectionService){
        this.searchServiceElastic = searchServiceElastic;
        this.collectionService = collectionService;
    }


    public Collection<NodeRef> getNodesForCurrentUserAndConfig() throws Throwable {

        RepositoryConfig.Frontpage config = RepositoryConfigFactory.getConfig().getFrontpage();
        if(config.getMode().equals(RepositoryConfig.Frontpage.Mode.collection)){
            if(config.getCollection()==null){
                throw new RuntimeException("Frontpage mode "+RepositoryConfig.Frontpage.Mode.collection+" requires a collection id to be defined");
            }
            // only return io's
            SortDefinition sortDefinition=new SortDefinition();
            sortDefinition.addSortDefinitionEntry(
                    new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_COLLECTION_ORDERED_POSITION),true),0);
            return collectionService.getChildren(config.getCollection(), null,sortDefinition, Collections.singletonList("files"));
        }

        BoolQuery.Builder query = buildQuery(config);

        boolean randomMode = RepositoryConfig.Frontpage.Mode.random.equals(config.getMode());
        SortOptions sortOptions = buildSortOptions(config);
        // in random mode elastic already shuffles the whole pool, so there is no need to fetch more
        // than the elements we're going to display
        int fetchCount = randomMode ? config.getDisplayCount() : config.getTotalCount();

        SearchRequest searchRequest = SearchRequest.of(req->req
                .index(WORKSPACE_INDEX)
                .from(0)
                // fetch more because we might need buffer for invalid permissions
                .size(fetchCount)
                .trackTotalHits(track->track.enabled(true))
                .query(q -> q.bool(query.build()))
                .sort(sortOptions)
        );

        SearchResponse<Map> searchResult = searchServiceElastic.searchNative(searchRequest);
        List<NodeRef> result=new ArrayList<>();
        Set<String> authorities = searchServiceElastic.getUserAuthorities();
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        boolean isAdmin = AuthorityServiceHelper.isAdmin();
        for(Hit<Map> hit : searchResult.hits().hits()){
            result.add(searchServiceElastic.transformSearchHit(isAdmin, authorities, user,hit.source(),false));
        }
        result = result.subList(0, Math.min(result.size(), fetchCount));
        if(!randomMode && config.getDisplayCount()<config.getTotalCount()) {
            Set<NodeRef> randoms = new HashSet<>();
            // grab a random count of elements (equals displayCount) of the whole array
            while (randoms.size() < config.getDisplayCount() && randoms.size()<result.size()) {
                randoms.add(result.get(new Random().nextInt(result.size())));
            }
            return randoms;
        }
        return result;
    }

    /**
     * builds the elastic query for all modes except {@link RepositoryConfig.Frontpage.Mode#collection}:
     * the base restrictions (readable io's in the workspace store, no collection references), the
     * unconditional {@link RepositoryConfig.Frontpage#getGlobalQuery()} and all configured queries whose
     * condition currently matches - all combined via "must"
     */
    BoolQuery.Builder buildQuery(RepositoryConfig.Frontpage config){
        BoolQuery.Builder query = new BoolQuery.Builder()
                .must(
                        m -> m.bool(searchServiceElastic::getReadPermissionsQuery))
                .must(
                        m -> m.term(t -> t.field("type").value("ccm:io"))
                )
                .must(
                        m -> m.term(t -> t.field("nodeRef.storeRef.protocol").value("workspace"))
                )
                .mustNot(
                        m -> m.term(t -> t.field("aspects").value("ccm:collection_io_reference"))
                );

        if(StringUtils.isNotBlank(config.getGlobalQuery())) {
            String globalQuery = QueryUtils.replaceCommonQueryParams(config.getGlobalQuery(),QueryUtils.replacerFromSyntax(MetadataReader.QUERY_SYNTAX_DSL));
            query.must(must->must.wrapper(new ReadableWrapperQueryBuilder(globalQuery).build()));
        }

        if(config.getQueries()!=null && !config.getQueries().isEmpty()) {
            // filter all queries with matching toolpermissions, than concat them via "must"
            config.getQueries().stream().filter((q)->{
                if(q.getCondition().getType().equals(RepositoryConfig.Condition.Type.TOOLPERMISSION)){
                    // should return true if query is launching
                    // so toolpermission == true && negate ? false : true -> toolpermission!=negate
                    return ToolPermissionServiceFactory.getInstance().hasToolPermission(q.getCondition().getValue()) != q.getCondition().isNegate();
                }
                return false;
            }).forEach((q)-> {
                //@TODO check config queries in extensions and fit for new index
                String queryString = QueryUtils.replaceCommonQueryParams(q.getQuery(),QueryUtils.replacerFromSyntax(MetadataReader.QUERY_SYNTAX_DSL));
                query.must(must->must.wrapper(new ReadableWrapperQueryBuilder(queryString).build()));
            });
        }
        return query;
    }

    /**
     * random mode is sorted by a plain random script, all other modes are sorted by the accumulated
     * statistic fields of the given timespan
     */
    SortOptions buildSortOptions(RepositoryConfig.Frontpage config){
        if(RepositoryConfig.Frontpage.Mode.random.equals(config.getMode())){
            return SortOptions.of(so -> so.script(
                    s -> s.type(ScriptSortType.Number).script(
                            script -> script.lang("painless").source("Math.random()"))
            ));
        }
        Script sortingScriptInline = new Script.Builder()
                .lang("painless")
                .source(STATISTIC_SORT_SCRIPT)
                .params("fields", getFieldNames(config))
                .build();
        return SortOptions.of(so -> so.script(
                s -> s.mode(SortMode.Max).type(ScriptSortType.Number).order(SortOrder.Desc).script(sortingScriptInline)
        ));
    }

    private JsonData getFieldNames(RepositoryConfig.Frontpage config){
        List<String> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        String prefix = "";
        if(RepositoryConfig.Frontpage.Mode.rating.equals(config.getMode()) ){
            prefix = "statistic_RATING_";
        }else if(RepositoryConfig.Frontpage.Mode.views.equals(config.getMode())){
            prefix = "statistic_VIEW_MATERIAL_";
        }else if(RepositoryConfig.Frontpage.Mode.downloads.equals(config.getMode())){
            prefix = "statistic_DOWNLOAD_MATERIAL_";
        }

        if(config.isTimespanAll()){
            String fieldName = prefix + "null";
            result.add(fieldName);
        }else {
            for (int i = 0; i < config.getTimespan(); i++) {
                if(i > 0){
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                }
                String dateStr = sdfDate.format(cal.getTime());
                String fieldName = prefix + dateStr;
                result.add(fieldName);
            }
        }
        return JsonData.of(result);
    }
}
