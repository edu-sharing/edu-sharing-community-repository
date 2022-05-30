package org.edu_sharing.service.provider;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceLAppsImpl;
import org.edu_sharing.service.nodeservice.NodeServiceTutoryImpl;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceCCPublish;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceLAppsImpl;
import org.edu_sharing.service.search.SearchServiceTutoryImpl;

public class TutoryProvider extends Provider{

    public TutoryProvider(String appId){
        super(appId);
    }
    @Override
    public NodeService getNodeService(){
        return new NodeServiceTutoryImpl(appId);
    }
    @Override
    public PermissionService getPermissionService(){
        return new PermissionServiceCCPublish(appId);
    }
    @Override
    public SearchService getSearchService(){
        return new SearchServiceTutoryImpl(appId);
    }
}
