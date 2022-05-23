package org.edu_sharing.service.provider;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceDDBImpl;
import org.edu_sharing.service.nodeservice.NodeServiceYouTube;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceCCPublish;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceDDBImpl;
import org.edu_sharing.service.search.SearchServiceYouTubeImpl;

public class DDBProvider extends Provider{

    public DDBProvider(String appId){
        super(appId);
    }
    @Override
    public NodeService getNodeService(){
        return new NodeServiceDDBImpl(appId);
    }
    @Override
    public PermissionService getPermissionService(){
        return new PermissionServiceCCPublish(appId);
    }
    @Override
    public SearchService getSearchService(){
        return new SearchServiceDDBImpl(appId);
    }
}
