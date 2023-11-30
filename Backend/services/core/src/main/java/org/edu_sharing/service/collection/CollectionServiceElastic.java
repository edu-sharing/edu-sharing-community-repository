package org.edu_sharing.service.collection;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
//import org.eclipse.core.internal.localstore.Bucket;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CollectionServiceElastic extends CollectionServiceImpl {

    private final SearchServiceElastic searchServiceElastic;
    public static CollectionServiceElastic build(String appId) {
        CollectionServiceConfig config = (CollectionServiceConfig) ApplicationContextFactory.getApplicationContext().getBean("collectionServiceConfig");
        return new CollectionServiceElastic(appId, config.getPattern(), config.getPath());
    }
    public CollectionServiceElastic(String appId, String pattern, String path) {
        super(appId,  pattern, path);
        this.searchServiceElastic = new SearchServiceElastic(appId);
    }

    @Override
    protected void addCollectionCountProperties(NodeRef nodeRef, Collection collection) {
        try {
            /*String path = AuthenticationUtil.runAsSystem(() ->
                    StringUtils.join(NodeServiceHelper.getParentPath(nodeRef).stream().map(NodeRef::getId).collect(Collectors.toList()), '/')
            );*/
            QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(
                    searchServiceElastic.getReadPermissionsQuery()
            ).must(
                QueryBuilders.matchQuery("nodeRef.storeRef.protocol","workspace")
            ).must(
                QueryBuilders.wildcardQuery("fullpath", "*/" + nodeRef.getId() + "*")
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
            SearchResponse result = searchServiceElastic.searchNative(searchRequest);
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

    @Override
    public CollectionProposalInfo getCollectionsContainingProposals(CCConstants.PROPOSAL_STATUS status, Integer skipCount, Integer maxItems, SortDefinition sortDefinition) throws Throwable {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status.toString());
        // use score query since in script query access to params._source is not supported since Elastic > 6.4
        Script scoreScript = new Script(Script.DEFAULT_SCRIPT_TYPE, "painless", "if(!params._source.containsKey('children')) return 0; for(def proposal: params._source.children) { if(proposal['type'] == 'ccm:collection_proposal' && proposal.containsKey('properties') && proposal['properties'].containsKey('ccm:collection_proposal_status') && proposal['properties']['ccm:collection_proposal_status'] == params.status) return 1; } return 0;", params);
        Script fieldScript = new Script(Script.DEFAULT_SCRIPT_TYPE, "painless", "Map m = new HashMap();for(def proposal: params._source.children) { if(proposal['type'] == 'ccm:collection_proposal') {def value = proposal['properties']['ccm:collection_proposal_status']; if(m.containsKey(value)) { m.put(value, m.get(value) + 1); } else { m.put(value, 1); } } } return m;", Collections.emptyMap());
        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(
                QueryBuilders.matchQuery("nodeRef.storeRef.protocol","workspace")
        ).must(
                QueryBuilders.matchQuery("type", CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP))
        ).must(
                QueryBuilders.matchQuery("aspects",  CCConstants.getValidLocalName(CCConstants.CCM_ASPECT_COLLECTION))
        ).must(
                searchServiceElastic.getPermissionsQuery("permissions.Write")
        );
        queryBuilder = QueryBuilders.scriptScoreQuery(queryBuilder, scoreScript).setMinScore(1);
        SearchRequest searchRequest = new SearchRequest("workspace");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.scriptField("proposals", fieldScript);
        searchSourceBuilder.fetchSource(true);
        searchSourceBuilder.size(maxItems);
        searchSourceBuilder.from(skipCount);
        searchSourceBuilder.trackTotalHits(true);
        sortDefinition.applyToSearchSourceBuilder(searchSourceBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse result = searchServiceElastic.searchNative(searchRequest);
        List<CollectionProposalInfo.CollectionProposalData> dataList = new ArrayList<>();
        Set<String> authorities = searchServiceElastic.getUserAuthorities();
        String user = serviceRegistry.getAuthenticationService().getCurrentUserName();
        for(SearchHit hit: result.getHits().getHits()) {
            CollectionProposalInfo.CollectionProposalData data = new CollectionProposalInfo.CollectionProposalData();
            data.setNodeRef(searchServiceElastic.transformSearchHit(authorities, user, hit,false));
            for (Object v : hit.getFields().get("proposals").getValues()) {
                Map al = (Map)v;
                for(Object e : al.entrySet()){
                    Map.Entry entry = (Map.Entry) e;
                    data.getProposalCount().put(CCConstants.PROPOSAL_STATUS.valueOf((String) entry.getKey()), (Integer) entry.getValue());
                }
                logger.info(v);
            }
            dataList.add(data);
        }
        return new CollectionProposalInfo(dataList, result.getHits().getTotalHits().value);
    }

    @Override
    public List<AssociationRef> getChildrenProposal(String parentId) throws Exception {
        return super.getChildrenProposal(parentId);
    }

    @Override
    public void proposeForCollection(String collectionId, String originalNodeId, String sourceRepositoryId) throws DuplicateNodeException, Throwable {
        super.proposeForCollection(collectionId, originalNodeId, sourceRepositoryId);
    }
}

