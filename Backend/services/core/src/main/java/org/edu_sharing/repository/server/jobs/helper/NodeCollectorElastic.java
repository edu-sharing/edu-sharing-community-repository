package org.edu_sharing.repository.server.jobs.helper;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;

import java.util.*;

public class NodeCollectorElastic {


    private final StoreRef storeRef;
    private final String elastic;
    private Logger logger = Logger.getLogger(NodeCollectorElastic.class);

    public NodeCollectorElastic(String elastic, StoreRef storeRef){
        this.elastic = elastic;
        this.storeRef = storeRef;
    }

    public List<NodeRef> getNodes(){
        logger.info("query:" + elastic);
        Set<NodeRef> set = new HashSet<>();
        execute(elastic,set);
        return new ArrayList<>(set);
    }

    private void execute( String query, Set<NodeRef> collect) {

        SearchToken searchToken = new SearchToken();
        searchToken.setFrom(0);
        searchToken.setMaxResult(Integer.MAX_VALUE);
        searchToken.setElasticQuery(QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(query.getBytes()))).build());
        searchToken.setStoreProtocol(storeRef.getProtocol());
        searchToken.setStoreName(storeRef.getIdentifier());

        SearchService searchService = SearchServiceFactory.getLocalService();
        SearchResultNodeRef search = searchService.search(searchToken);
        search.getData().forEach(n -> {
            NodeRef nodeRef = new NodeRef(new StoreRef(n.getStoreProtocol(),n.getStoreId()),n.getNodeId());
            collect.add(nodeRef);
        });
    }
}
