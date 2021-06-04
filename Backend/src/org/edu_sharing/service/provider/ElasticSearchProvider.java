package org.edu_sharing.service.provider;

import org.edu_sharing.service.rating.RatingService;
import org.edu_sharing.service.rating.RatingServiceFactory;
import org.edu_sharing.service.rating.RatingServiceImpl;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceElastic;
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

    @Override
    public RatingService getRatingService() {
        return RatingServiceFactory.getLocalService();
    }

    @Override
    public CollectionService getCollectionService() {
        return CollectionServiceElastic.build(appId);
    }
}
