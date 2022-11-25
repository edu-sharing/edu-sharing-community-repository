package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JobDescription(description = "checks for nodes removed in repository but still exist in elasticsearch. please check that tracker is 100% finished and tracker is disabled before running ths job.")
public class FixElasticSearchDeletedNodes extends AbstractJob{

    @JobFieldDescription(description = "if false (default) no changes will be done.")
    boolean execute;

    @JobFieldDescription(description = "query that delivers a result of nodes that have to be checked. optional. if not set all nodes will be searched.",sampleValue = "{\"query\":\"{\\\"term\\\":{\\\"type\\\":\\\"ccm:io\\\"}}\"}")
    String query;

    SearchServiceElastic searchServiceElastic = new SearchServiceElastic(ApplicationInfoList.getHomeRepository().getAppId());

    private static int pageSize = 1000;

    Logger logger = Logger.getLogger(FixElasticSearchDeletedNodes.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();

    public static String INDEX_WORKSPACE = "workspace";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        execute = new Boolean( (String) jobExecutionContext.getJobDetail().getJobDataMap().get("execute"));
        query =  (String)jobExecutionContext.getJobDetail().getJobDataMap().get("query");

            AuthenticationUtil.runAsSystem(()->{
                try {
                    QueryBuilder queryBuilder = (query == null) ? QueryBuilders.matchAllQuery() : QueryBuilders.wrapperQuery(query);
                    search(queryBuilder, new DeletedNodesHandler());
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                }
                return null;
            });
    }

    private void search(QueryBuilder queryBuilder, SearchResultHandler searchResultHandler) throws IOException{
        logger.info("search with handler: "+searchResultHandler.getClass().getName());

        final Scroll scroll = new Scroll(TimeValue.timeValueHours(4L));
        SearchResponse response = null;
        int page = 0;
        do{
            if(response == null) {
                response = search(INDEX_WORKSPACE, queryBuilder, scroll);
            }else {
                response = scroll(scroll,response.getScrollId());
            }
            SearchHits searchHits = response.getHits();
            logger.info("page:" + page + " with result size:" + searchHits.getHits().length + " of:" + searchHits.getTotalHits().value);
            for(SearchHit searchHit : searchHits.getHits()){
                searchResultHandler.handleSearchHit(searchHit);
            }
            page++;
        }while(response != null
                && response.getHits() != null
                && response.getHits().getHits().length > 0);

        boolean clearSuccess = clearScroll(response.getScrollId());
        if(clearSuccess) logger.info("cleared scroll successfully");
        else logger.error("clear of scroll "+ response.getScrollId() +" failed");
    }




    private SearchResponse search(String index, QueryBuilder queryBuilder, Scroll scroll) throws IOException {
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(pageSize);
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.fetchSource(null,new String[]{"preview"});
        searchRequest.source(searchSourceBuilder);
        return searchServiceElastic.searchNative(searchRequest);
    }

    private SearchResponse scroll(Scroll scroll, String scrollId) throws IOException {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(scroll);
        return searchServiceElastic.scrollNative(scrollRequest);
    }

    private boolean clearScroll(String scrollId) throws IOException {
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = searchServiceElastic.clearScrollNative(clearScrollRequest);
        return clearScrollResponse.isSucceeded();
    }

    public interface SearchResultHandler{
        public void handleSearchHit(SearchHit searchHit) throws IOException;
    }

    public class DeletedNodesHandler implements SearchResultHandler{
        @Override
        public void handleSearchHit(SearchHit searchHit) throws IOException {

            Map nodeRef = (Map) searchHit.getSourceAsMap().get("nodeRef");
            String nodeId = (String) nodeRef.get("id");
            String dbid = searchHit.getId();

            Map properties = (Map)searchHit.getSourceAsMap().get("properties");
            String name = (String)properties.get("cm:name");
            String type = (String)searchHit.getSourceAsMap().get("type");
            List<String> aspects = (List<String>)searchHit.getSourceAsMap().get("aspects");

            Map storeRef = (Map) nodeRef.get("storeRef");
            String protocol = (String) storeRef.get("protocol");
            String identifier = (String) storeRef.get("identifier");

            NodeRef alfNodeRef = new NodeRef(new StoreRef(protocol,identifier),nodeId);
            if(!nodeService.exists(alfNodeRef)){
                logger.info(alfNodeRef +";dbid:"+dbid+ ";type:"+ type + ";name:"+name + ";does not longer exist in repo. will remove.");
                //tracker gets 2 events when a node is deleted from workspace: delete and create for archive store
                //so we can safely remove it here without checking for archive store

                //cleanup replicated collections on ios
                if(type.equals("ccm:map") && aspects.contains("ccm:collection") ){
                    syncNestedCollections(dbid);
                }

                if(execute){
                    DeleteRequest request = new DeleteRequest(
                            INDEX_WORKSPACE,
                            dbid);
                    searchServiceElastic.deleteNative(request);
                }
            }

        }
    }

    /**
     * remove nested collection of ccm:io when ccm:collection is remove
     * @TODO test: io with one collection, io with more collections/diff before job
     * @param dbid from collection
     * @throws IOException
     */
    private void syncNestedCollections(String dbid) throws IOException {
        QueryBuilder ioCollectionQuery = QueryBuilders.termQuery("collections.dbid", dbid);

        search(ioCollectionQuery, searchHit -> {
            List<Map<String, Object>> collections = (List<Map<String, Object>>) searchHit.getSourceAsMap().get("collections");

            /**
             * cleanup collections
             */
            logger.info("will remove replicated collection with id:"+dbid
                    +" name:"+searchHit.getSourceAsMap().get("properties.cm:name")
                    +" from ccm:io with id:"+searchHit.getId());
            cleanupSubArray(dbid, searchHit, collections, "collections");

            /**
             * cleanup usages
             */
            List<Map<String, Object>> children = (List<Map<String, Object>>) searchHit.getSourceAsMap().get("children");
            //find usagedbid from collection object
            Long usageDbId = collections.stream()
                    .filter(m -> ((Number) m.get("dbid")).longValue() == Long.parseLong(dbid))
                    .map(m -> ((Number) m.get("usagedbid")).longValue()).findFirst().get();
            //cleanup usage
            logger.info("will remove replicated usage with id:"+usageDbId
                    +" from ccm:io with id:"+searchHit.getId());
            cleanupSubArray(new Long(usageDbId).toString(),searchHit,children,"children");
        });
    }

    private void cleanupSubArray(String dbid, SearchHit searchHit, List<Map<String, Object>> nestedObjectsArray, String subArrayName) throws IOException {
        if(nestedObjectsArray.size() == 1) {
            /**
             * remove all collections so that there is no empty collection object left over
             */
            HashMap params = new HashMap();
            params.put("value",subArrayName);
            Script inline = new Script(ScriptType.INLINE,
                    "painless",
                    "ctx._source.remove(params.get('value'))", params);
            if(execute) {
                UpdateRequest request = new UpdateRequest(INDEX_WORKSPACE, searchHit.getId());
                request.script(inline);
                update(request);
            }
        }else {

            /**
             * the other collections
             */
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startArray(subArrayName);
                for (Map<String, Object> nestedObject : nestedObjectsArray) {
                    if(!dbid.equals(nestedObject.get("dbid").toString())){
                        builder.startObject();
                        for(Map.Entry<String,Object> entry : nestedObject.entrySet()){
                            builder.field(entry.getKey(),entry.getValue());
                        }
                        builder.endObject();
                    }else{
                        logger.info("excluded " + dbid + " from subarray: " + subArrayName);
                    }
                }
                builder.endArray();
            }
            builder.endObject();

            if(execute) {
                UpdateRequest request = new UpdateRequest(
                        INDEX_WORKSPACE,
                        searchHit.getId()).doc(builder);
                update(request);
            }
        }
    }

    private void update(UpdateRequest request) throws IOException{
        UpdateResponse updateResponse = searchServiceElastic.updateNative(
                request
        );
        String index = updateResponse.getIndex();
        String id = updateResponse.getId();
        long version = updateResponse.getVersion();
        if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
            logger.error("object did not exist");
        } else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {

        } else if (updateResponse.getResult() == DocWriteResponse.Result.DELETED) {

        } else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {

        }
    }

}
