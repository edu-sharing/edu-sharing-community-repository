package org.edu_sharing.service.provider;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.nodeservice.NodeServiceWSImpl;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceImpl;
import org.edu_sharing.service.permission.PermissionServiceWSImpl;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceImpl;
import org.edu_sharing.service.search.SearchServiceWSImpl;

public class LocalProvider extends Provider{

    public LocalProvider(String appId){
        super(appId);
    }
    @Override
    public NodeService getNodeService(){
        return new NodeServiceImpl(appId);
    }
    @Override
    public PermissionService getPermissionService(){
        return new PermissionServiceImpl(appId);
    }
    @Override
    public SearchService getSearchService(){
        return new SearchServiceImpl(appId);
    }
}
