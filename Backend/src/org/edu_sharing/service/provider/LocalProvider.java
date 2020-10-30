package org.edu_sharing.service.provider;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.nodeservice.NodeServiceWSImpl;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceImpl;
import org.edu_sharing.service.permission.PermissionServiceWSImpl;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.SearchServiceImpl;
import org.edu_sharing.service.search.SearchServiceWSImpl;

public class LocalProvider extends Provider{

    public LocalProvider(String appId){
        super(appId);
    }
    @Override
    public NodeService getNodeService(){ return NodeServiceFactory.getLocalService(); }
    @Override
    public PermissionService getPermissionService(){
        return PermissionServiceFactory.getLocalService();
    }
    @Override
    public SearchService getSearchService(){
        return SearchServiceFactory.getLocalService();
    }
}
