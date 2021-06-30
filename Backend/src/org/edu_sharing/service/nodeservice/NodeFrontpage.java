package org.edu_sharing.service.nodeservice;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.QueryUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.admin.RepositoryConfigFactory;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class NodeFrontpage {
    private Logger logger= Logger.getLogger(NodeFrontpage.class);
    private static final String INDEX_NAME = "frontpage_cache";
    private static final String TYPE_NAME = "_doc";
    private SearchService searchService= SearchServiceFactory.getLocalService();
    private NodeService nodeService=NodeServiceFactory.getLocalService();
    private PermissionService permissionService= PermissionServiceFactory.getLocalService();
    private HashMap<String, Date> APPLY_DATES;

    SearchServiceElastic searchServiceElastic = new SearchServiceElastic(ApplicationInfoList.getHomeRepository().getAppId());

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

    public NodeFrontpage(){
    }


    public Collection<NodeRef> getNodesForCurrentUserAndConfig() throws Throwable {
        RepositoryConfig.Frontpage config = RepositoryConfigFactory.getConfig().frontpage;
        if(config.mode.equals(RepositoryConfig.Frontpage.Mode.collection)){
            if(config.collection==null){
                throw new RuntimeException("Frontpage mode "+RepositoryConfig.Frontpage.Mode.collection+" requires a collection id to be defined");
            }
            // only return io's
            SortDefinition sortDefinition=new SortDefinition();
            sortDefinition.addSortDefinitionEntry(
                    new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_COLLECTION_ORDERED_POSITION),true),0);
            return CollectionServiceFactory.getLocalService().getChildren(config.collection, null,sortDefinition, Collections.singletonList("files"));
        }
        //initElastic rasuschmeißen (frontpage_cache nicht mehr benötigt)

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(searchServiceElastic.getReadPermissionsQuery());
        query.must(QueryBuilders.termQuery("type","ccm:io"));
        query.must(QueryBuilders.termQuery("nodeRef.storeRef.protocol","workspace"));

        if(config.queries!=null && !config.queries.isEmpty()) {
            // filter all queries with matching toolpermissions, than concat them via "must"
            config.queries.stream().filter((q)->{
                if(q.condition.type.equals(RepositoryConfig.Condition.Type.TOOLPERMISSION)){
                    // should return true if query is launching
                    // so toolpermission == true && negate ? false : true -> toolpermission!=negate
                    return ToolPermissionServiceFactory.getInstance().hasToolPermission(q.condition.value)!=q.condition.negate;
                }
                return false;
            }).forEach((q)-> {
                //@TODO check config queries in extensions and fit for new index
                query.must(QueryBuilders.wrapperQuery(QueryUtils.replaceCommonQueryParams(q.query,QueryUtils.replacerFromSyntax(MetadataReaderV2.QUERY_SYNTAX_DSL))));
            });
        }


        //InputStream is = NodeFrontpage.class.getClassLoader().getResourceAsStream("frontpage-ratings.properties");
        InputStream is = NodeFrontpage.class.getClassLoader().getResource("frontpage-ratings.properties").openStream();
        String sortingScript = IOUtils.toString(is, StandardCharsets.UTF_8.name());

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);

        Map<String,Object> params = new HashMap<>();

        params.put("fields",getFieldNames(config));

        Script script = new Script(Script.DEFAULT_SCRIPT_TYPE, "painless", sortingScript,Collections.emptyMap(),params);
        ScriptSortBuilder sb = SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER).order(SortOrder.DESC);
        sb.sortMode(SortMode.MAX);
        searchSourceBuilder.sort(sb);


        searchSourceBuilder.size(config.totalCount);
        searchSourceBuilder.from(0);
        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
        searchRequest.indices("workspace");
        SearchResponse searchResult = searchServiceElastic.getClient().search(searchRequest,RequestOptions.DEFAULT);
        List<NodeRef> result=new ArrayList<>();
        for(SearchHit hit : searchResult.getHits().getHits()){
            logger.info("score:"+hit.getScore() +" id:"+hit.getId() + " "+ ((Map)hit.getSourceAsMap().get("properties")).get("cm:name"));
            if(permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),hit.getId(),CCConstants.PERMISSION_READ)){
                Map nodeRef = (Map) hit.getSourceAsMap().get("nodeRef");
                String nodeId = (String) nodeRef.get("id");
                result.add(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId));
            }
        }
        if(config.displayCount<config.totalCount) {
            Set<NodeRef> randoms = new HashSet<>();
            // grab a random count of elements (equals displayCount) of the whole array
            while (randoms.size() < config.displayCount && randoms.size()<result.size()) {
                randoms.add(result.get(new Random().nextInt(result.size())));
            }
            return randoms;
        }
        return result;

    }

    private List<String> getFieldNames(RepositoryConfig.Frontpage config){
        List<String> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        String prefix = "";
        if(RepositoryConfig.Frontpage.Mode.rating.equals(config.mode) ){
           prefix = "statistic_RATING_";
        }else if(RepositoryConfig.Frontpage.Mode.views.equals(config.mode)){
            prefix = "statistic_VIEW_MATERIAL_";
        }else if(RepositoryConfig.Frontpage.Mode.downloads.equals(config.mode)){
            prefix = "statistic_DOWNLOAD_MATERIAL_";
        }

        if(config.timespanAll == true){
            String fieldName = prefix + "null";
            result.add(fieldName);
        }else {
            for (int i = 0; i < config.timespan; i++) {
                if(i > 0){
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                }
                String dateStr = sdfDate.format(cal.getTime());
                String fieldName = prefix + dateStr;
                result.add(fieldName);
            }
        }
        return result;
    }
}
