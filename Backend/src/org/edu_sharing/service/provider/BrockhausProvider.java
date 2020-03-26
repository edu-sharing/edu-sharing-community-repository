package org.edu_sharing.service.provider;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceBrockhausImpl;
import org.edu_sharing.service.nodeservice.NodeServiceDDBImpl;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceCCPublish;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceNotSupported;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceBrockhausImpl;
import org.edu_sharing.service.search.SearchServiceDDBImpl;

public class BrockhausProvider extends Provider{

    public BrockhausProvider(String appId){
        super(appId);
    }
    @Override
    public NodeService getNodeService(){
        return new NodeServiceBrockhausImpl(appId);
    }
    @Override
    public PermissionService getPermissionService(){
        return new PermissionServiceCCPublish(appId);
    }
    @Override
    public SearchService getSearchService(){
        return new SearchServiceBrockhausImpl(appId);
    }
}
