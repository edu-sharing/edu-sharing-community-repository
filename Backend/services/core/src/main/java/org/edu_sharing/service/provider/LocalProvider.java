package org.edu_sharing.service.provider;

import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.comment.CommentServiceFactory;
import org.edu_sharing.service.feedback.FeedbackService;
import org.edu_sharing.service.feedback.FeedbackServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;

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

    @Override
    public CommentService getCommentService() {
        return CommentServiceFactory.getLocalService();
    }

    @Override
    public FeedbackService getFeedbackService() {
        return FeedbackServiceFactory.getLocalService();
    }
}
