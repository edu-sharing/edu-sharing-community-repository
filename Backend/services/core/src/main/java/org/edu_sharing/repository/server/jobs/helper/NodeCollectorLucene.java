package org.edu_sharing.repository.server.jobs.helper;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeCollectorLucene {


    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    SearchService searchService = serviceRegistry.getSearchService();

    private final StoreRef storeRef;
    private final String lucene;
    private Logger logger = Logger.getLogger(NodeCollectorLucene.class);

    int PAGE_SIZE = 100;

    public NodeCollectorLucene(String lucene, StoreRef storeRef){
        this.lucene = lucene;
        this.storeRef = storeRef;
    }

    List<NodeRef> getNodes(){
        logger.info("query:" + lucene);
        Set<NodeRef> set = new HashSet<>();
        execute(0,lucene,set);
        return new ArrayList<>(set);
    }

    private void execute(int page, String query, Set<NodeRef> collect) {
        logger.info("page:" + page);
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.addStore(storeRef);
        sp.setSkipCount(page);
        sp.setMaxItems(PAGE_SIZE);
        sp.setQuery(query);

        ResultSet resultSet = searchService.query(sp);

        for (NodeRef nodeRef : resultSet.getNodeRefs()) {
            collect.add(nodeRef);
        }
        if(resultSet.hasMore()) {
            execute(page + PAGE_SIZE,query,collect);
        }
    }
}
