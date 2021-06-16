package org.edu_sharing.service.search;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class SearchServiceHelper {
    public static List<NodeRef> queryAll(SearchParameters parameters, int limit) {
        ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
        org.alfresco.service.cmr.search.SearchService searchService = (org.alfresco.service.cmr.search.SearchService) alfApplicationContext
                .getBean("scopedSearchService");

        if(limit<=0)
            limit=Integer.MAX_VALUE;

        List<NodeRef> result=new ArrayList<>();
        int MAX_PER_PAGE=1000;
        for(int offset=0;;offset=result.size()) {
            parameters.setSkipCount(offset);
            parameters.setMaxItems(Math.min(MAX_PER_PAGE,limit-result.size()));
            ResultSet data = searchService.query(parameters);
            result.addAll(data.getNodeRefs());
            if(result.size()>=limit || data.getNodeRefs().size()<MAX_PER_PAGE)
                break;
        }
        return result;
    }
}
