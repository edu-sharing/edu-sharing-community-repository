package org.edu_sharing.service.provider;

import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;

public class ElasticSearchProvider extends LocalProvider {

    public ElasticSearchProvider(String appId){
        super(appId);

    }

    @Override
    public SearchService getSearchService() {
        return new SearchServiceElastic(appId);
    }
}
