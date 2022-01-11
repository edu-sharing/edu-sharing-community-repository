package org.edu_sharing.service.provider;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceBrockhausImpl;
import org.edu_sharing.service.nodeservice.NodeServiceLAppsImpl;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceCCPublish;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceBrockhausImpl;
import org.edu_sharing.service.search.SearchServiceLAppsImpl;

public class LearningAppsProvider extends Provider{

    public LearningAppsProvider(String appId){
        super(appId);
    }
    @Override
    public NodeService getNodeService(){
        return new NodeServiceLAppsImpl(appId);
    }
    @Override
    public PermissionService getPermissionService(){
        return new PermissionServiceCCPublish(appId);
    }
    @Override
    public SearchService getSearchService(){
        return new SearchServiceLAppsImpl(appId);
    }
}
