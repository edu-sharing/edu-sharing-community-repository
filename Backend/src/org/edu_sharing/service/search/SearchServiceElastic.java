package org.edu_sharing.service.search;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.metadataset.v2.tools.MetadataElasticSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.util.AlfrescoDaoHelper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class SearchServiceElastic extends SearchServiceImpl {

    public SearchServiceElastic(String applicationId) {
        super(applicationId);
    }

    Logger logger = Logger.getLogger(SearchServiceElastic.class);

    ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();

    ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);


    @Override
    public SearchResultNodeRef searchV2(MetadataSetV2 mds, String query, Map<String,String[]> criterias,
                                        SearchToken searchToken) throws Throwable {

        MetadataQuery queryData;
        try{
            queryData = mds.findQuery(query, MetadataReaderV2.QUERY_SYNTAX_DSL);
        } catch(IllegalArgumentException e){
            logger.info("Query " + query + " is not defined within dsl language, switching to lucene...");
            return super.searchV2(mds,query,criterias,searchToken);
        }

        String[] searchword = criterias.get("ngsearchword");
        String ngsearchword = (searchword != null) ? searchword[0] : null;


        Set<String> authorities = serviceRegistry.getAuthorityService().getAuthorities();
        if(!authorities.contains(CCConstants.AUTHORITY_GROUP_EVERYONE))
            authorities.add(CCConstants.AUTHORITY_GROUP_EVERYONE);



        SearchResultNodeRef sr = new SearchResultNodeRef();
        List<NodeRef> data = new ArrayList<>();
        sr.setData(data);
        try {

            SearchRequest searchRequest = new SearchRequest("workspace");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            QueryBuilder metadataQueryBuilder = MetadataElasticSearchHelper.getElasticSearchQuery(queryData,criterias);
            BoolQueryBuilder audienceQueryBuilder = QueryBuilders.boolQuery();
            audienceQueryBuilder.minimumShouldMatch(1);
            for (String a : authorities) {
                audienceQueryBuilder.should(QueryBuilders.matchQuery("permissions.read", a));
            }
            audienceQueryBuilder.should(QueryBuilders.matchQuery("permissions.read", serviceRegistry.getAuthenticationService().getCurrentUserName()));
            audienceQueryBuilder.should(QueryBuilders.matchQuery("owner", serviceRegistry.getAuthenticationService().getCurrentUserName()));
            QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(metadataQueryBuilder).must(audienceQueryBuilder);

            for(String facette : searchToken.getFacettes()){
                searchSourceBuilder.aggregation(AggregationBuilders.terms(facette).field("properties." + facette + ".keyword"));
            }

            /**
             * add collapse builder
             */
            CollapseBuilder collapseBuilder = new CollapseBuilder("properties.ccm:original");
            searchSourceBuilder.collapse(collapseBuilder);
            /**
             * cardinality aggregation to get correct total count
             *
             * https://github.com/elastic/elasticsearch/issues/24130
             */
            searchSourceBuilder.aggregation(AggregationBuilders.cardinality("original_count").field("properties.ccm:original"));



            searchSourceBuilder.query(queryBuilder);
            searchSourceBuilder.from(searchToken.getFrom());
            searchSourceBuilder.size(searchToken.getMaxResult());
            searchSourceBuilder.trackTotalHits(true);
            searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));




            searchRequest.source(searchSourceBuilder);



            RestHighLevelClient client = getClient();
            logger.info("starting search elastic");
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            logger.info("finished search elastic");

            SearchHits hits = searchResponse.getHits();


            for (SearchHit hit : hits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Map<String, Serializable> properties = (Map) sourceAsMap.get("properties");

                Map nodeRef = (Map) sourceAsMap.get("nodeRef");
                String nodeId = (String) nodeRef.get("id");
                Map storeRef = (Map) nodeRef.get("storeRef");
                String protocol = (String) storeRef.get("protocol");
                String identifier = (String) storeRef.get("identifier");

                HashMap<String, Object> props = new HashMap<>();
                for (Map.Entry<String, Serializable> entry : properties.entrySet()) {

                    //String value = null;


                    props.put(CCConstants.getValidGlobalName(entry.getKey()), entry.getValue().toString());
                }

                NodeRef eduNodeRef = new NodeRefImpl(ApplicationInfoList.getHomeRepository().getAppId(),
                        protocol,
                        identifier,
                        nodeId);
                eduNodeRef.setProperties(props);
                data.add(eduNodeRef);
            }

            Map<String,Map<String,Integer>> facettesResult = new HashMap<String,Map<String,Integer>>();

            Long total = null;
           for(Aggregation a : searchResponse.getAggregations()){
               if(a instanceof  ParsedStringTerms) {
                   ParsedStringTerms pst = (ParsedStringTerms) a;
                   Map<String, Integer> f = new HashMap<>();
                   facettesResult.put(a.getName(), f);
                   for (Terms.Bucket b : pst.getBuckets()) {
                       String key = b.getKeyAsString();
                       long count = b.getDocCount();
                       f.put(key, (int) count);


                   }
               }else if (a instanceof ParsedCardinality){
                   ParsedCardinality pc = (ParsedCardinality)a;
                   if (a.getName().equals("original_count")) {
                       total = pc.getValue();
                   }else{
                       logger.error("unknown cardinality aggregation");
                   }

               }else{
                   logger.error("non supported aggreagtion "+a.getName());
               }
           }
           if(total == null){
               total = hits.getTotalHits().value;
           }
            sr.setCountedProps(facettesResult);
            sr.setStartIDX(searchToken.getFrom());
            sr.setNodeCount((int)total.longValue());
            client.close();
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }


        logger.info("returning");
        return sr;
    }

    RestHighLevelClient getClient(){
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                        //,new HttpHost("localhost", 9201, "http")
                ));
    };



}
