package org.edu_sharing.service.collection;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.stream.Collectors;

public class CollectionServiceElastic extends CollectionServiceImpl {

    private final SearchServiceElastic searchServiceElastic;
    private final RestHighLevelClient esClient;

    public static CollectionServiceElastic build(String appId) {
        CollectionServiceConfig config = (CollectionServiceConfig) ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
        return new CollectionServiceElastic(appId, config.getPattern(), config.getPath());
    }
    public CollectionServiceElastic(String appId, String pattern, String path) {
        super(appId,  pattern, path);
        this.searchServiceElastic = new SearchServiceElastic(appId);
        try {
            esClient = searchServiceElastic.getClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void addCollectionCountProperties(NodeRef nodeRef, Collection collection) {
        try {
            String path = AuthenticationUtil.runAsSystem(() ->
                    StringUtils.join(NodeServiceHelper.getParentPath(nodeRef).stream().map(NodeRef::getId).collect(Collectors.toList()), '/')
            );
            QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(
                searchServiceElastic.getReadPermissionsQuery()
            ).must(
                QueryBuilders.matchQuery("nodeRef.storeRef.protocol","workspace")
            ).must(
                QueryBuilders.wildcardQuery("fullpath", path + "/*")
            ).mustNot(
                QueryBuilders.matchQuery("aspects", CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT))
            );

            AggregationBuilder aggregation = AggregationBuilders.terms("type").field("type");
            SearchRequest searchRequest = new SearchRequest("workspace");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(queryBuilder);
            searchSourceBuilder.aggregation(aggregation);
            searchSourceBuilder.size(0);
            searchRequest.source(searchSourceBuilder);
            SearchResponse result = esClient.search(searchRequest, RequestOptions.DEFAULT);
            ParsedStringTerms types = result.getAggregations().get("type");
            for(Terms.Bucket bucket : types.getBuckets()){
                if(bucket.getKeyAsString().equals("ccm:io")){
                    collection.setChildReferencesCount((int) bucket.getDocCount());
                } else if(bucket.getKeyAsString().equals("ccm:map")){
                    collection.setChildCollectionsCount((int) bucket.getDocCount());
                }
            }
        }catch(Exception e){
            logger.warn("Error fetching collection counts for " + nodeRef, e);
    }


    }
}

