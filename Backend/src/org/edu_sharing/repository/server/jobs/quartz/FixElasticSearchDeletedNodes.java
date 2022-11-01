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
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;

@JobDescription(description = "checks for nodes removed in repository but still exist in elasticsearch. please check that tracker is 100% finished and tracker is disabled before running ths job.")
public class FixElasticSearchDeletedNodes extends AbstractJob{

    @JobFieldDescription(description = "if false (default) no changes will be done.")
    boolean execute;

    @JobFieldDescription(description = "query that delivers a result of nodes that have to be checked. optional. if not set all nodes will be searched.",sampleValue = "{\"query\":\"{\\\"term\\\":{\\\"type\\\":\\\"ccm:io\\\"}}\"}")
    String query;

    SearchServiceElastic searchServiceElastic = new SearchServiceElastic(ApplicationInfoList.getHomeRepository().getAppId());

    private static int pageSize = 100;

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
                    run();
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                }
                return null;
            });
    }

    private void run() throws IOException{
        QueryBuilder queryBuilder = (query == null) ? QueryBuilders.matchAllQuery() : QueryBuilders.wrapperQuery(query);
        SearchRequest searchRequest = new SearchRequest(INDEX_WORKSPACE);

        int page = 0;
        SearchHits searchHits = null;
        do{
            if(searchHits != null){
                page += pageSize;
            }
            searchHits = search(INDEX_WORKSPACE, queryBuilder, page, pageSize);
            for(SearchHit searchHit : searchHits.getHits()){
                logger.info("page:" + page +" of:" + searchHits.getTotalHits().value);
                handleSearchHit(searchHit);
            }

        }while(searchHits.getTotalHits().value > page);
    }

    private void handleSearchHit(SearchHit searchHit) throws IOException {

        Map nodeRef = (Map) searchHit.getSourceAsMap().get("nodeRef");
        String nodeId = (String) nodeRef.get("id");
        long dbid = Long.parseLong(searchHit.getId());

        Map properties = (Map)searchHit.getSourceAsMap().get("properties");
        String name = (String)properties.get("cm:name");
        String type = (String)searchHit.getSourceAsMap().get("type");

        Map storeRef = (Map) nodeRef.get("storeRef");
        String protocol = (String) storeRef.get("protocol");
        String identifier = (String) storeRef.get("identifier");

        NodeRef alfNodeRef = new NodeRef(new StoreRef(protocol,identifier),nodeId);
        if(!nodeService.exists(alfNodeRef)){
            logger.info(alfNodeRef +";dbid:"+dbid+ ";type:"+ type + ";name:"+name + ";does not longer exist in repo. will remove.");
            //tracker gets 2 events when a node is deleted from workspace: delete and create for archive store
            //so we can safely remove it here without checking for archive store
            if(execute){
                DeleteRequest request = new DeleteRequest(
                        INDEX_WORKSPACE,
                        Long.toString(dbid));
                searchServiceElastic.getClient().delete(request,RequestOptions.DEFAULT);
            }
        }
    }

    /**
     * @TODO put this method in searchServiceElastic
     *
     * @param index
     * @param queryBuilder
     * @param from
     * @param size
     * @return
     * @throws IOException
     */
    private SearchHits search(String index, QueryBuilder queryBuilder, int from, int size) throws IOException {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = searchServiceElastic.getClient().search(searchRequest, RequestOptions.DEFAULT);
        return searchResponse.getHits();
    }
}
