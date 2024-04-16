package org.edu_sharing.service.provider;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServicePixabayImpl;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceCCPublish;
import org.edu_sharing.service.permission.PermissionServiceReadOnly;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServicePixabayImpl;

public class PixabayProvider extends Provider{

    public PixabayProvider(String appId){
        super(appId);
    }
    @Override
    public NodeService getNodeService(){
        return new NodeServicePixabayImpl(appId);
    }
    @Override
    public PermissionService getPermissionService(){
        return new PermissionServiceCCPublish(appId);
    }
    @Override
    public SearchService getSearchService(){
        return new SearchServicePixabayImpl(appId);
    }
}
