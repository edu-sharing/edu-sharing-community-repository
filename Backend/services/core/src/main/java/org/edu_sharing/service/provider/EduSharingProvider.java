package org.edu_sharing.service.provider;

import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCBaseClient;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceWSImpl;
import org.edu_sharing.service.nodeservice.NodeServiceYouTube;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceCCPublish;
import org.edu_sharing.service.permission.PermissionServiceWSImpl;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceWSImpl;
import org.edu_sharing.service.search.SearchServiceYouTubeImpl;

import java.util.HashMap;

public class EduSharingProvider extends Provider{

    public EduSharingProvider(String appId){
        super(appId);
    }
    @Override
    public NodeService getNodeService(){
        return new NodeServiceWSImpl(appId);
    }
    @Override
    public PermissionService getPermissionService(){
        return new PermissionServiceWSImpl(appId);
    }
    @Override
    public SearchService getSearchService(){
        return new SearchServiceWSImpl(appId);
    }
}
