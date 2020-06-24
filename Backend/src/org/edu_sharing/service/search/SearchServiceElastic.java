package org.edu_sharing.service.search;

import com.sun.star.lang.IllegalArgumentException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.SearchCriterias;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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


        if(query.equals("collections")) return super.searchV2(mds,query,criterias,searchToken);


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


            QueryBuilder ioQuery = QueryBuilders.termQuery("type", "ccm:io");
            QueryBuilder metadataQueryBuilder = (ngsearchword != null) ? QueryBuilders.boolQuery().must(ioQuery)
                    .must(QueryBuilders.wildcardQuery("properties.cm:name",
                            (ngsearchword.contains("*") ? ngsearchword.toLowerCase() : "*"+ngsearchword.toLowerCase()+"*")))
                    : ioQuery;
            BoolQueryBuilder audienceQueryBuilder = QueryBuilders.boolQuery();
            audienceQueryBuilder.minimumShouldMatch(1);
            for (String a : authorities) {
                audienceQueryBuilder.should(QueryBuilders.matchQuery("permissions.read", a));
            }
            audienceQueryBuilder.should(QueryBuilders.matchQuery("owner", serviceRegistry.getAuthenticationService().getCurrentUserName()));
            QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(metadataQueryBuilder).must(audienceQueryBuilder);

            searchSourceBuilder.query(queryBuilder);
            searchSourceBuilder.from(searchToken.getFrom());
            searchSourceBuilder.size(searchToken.getMaxResult());
            searchSourceBuilder.trackTotalHits(true);
            searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));

            searchRequest.source(searchSourceBuilder);



            RestHighLevelClient client = getClient();
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);


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
            sr.setStartIDX(searchToken.getFrom());
            sr.setNodeCount((int)hits.getTotalHits().value);
            client.close();
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }



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
