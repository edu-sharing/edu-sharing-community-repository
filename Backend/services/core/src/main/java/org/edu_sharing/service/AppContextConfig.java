package org.edu_sharing.service;

import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.AuthenticationToolYouTube;
import org.edu_sharing.repository.server.appcontext.AppContextRegistry;
import org.edu_sharing.service.archive.ArchiveService;
import org.edu_sharing.service.archive.ArchiveServiceImpl;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceElastic;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.comment.CommentServiceAdapter;
import org.edu_sharing.service.comment.CommentServiceImpl;
import org.edu_sharing.service.dashboard.DashboardConfigService;
import org.edu_sharing.service.feedback.FeedbackService;
import org.edu_sharing.service.feedback.FeedbackServiceAdapter;
import org.edu_sharing.service.feedback.FeedbackServiceImpl;
import org.edu_sharing.service.nodeservice.*;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.organization.OrganizationService;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceAdapter;
import org.edu_sharing.service.permission.PermissionServiceCCPublish;
import org.edu_sharing.service.rating.RatingService;
import org.edu_sharing.service.rating.RatingServiceAdapter;
import org.edu_sharing.service.relations.NodeRelationTraceService;
import org.edu_sharing.service.relations.RelationService;
import org.edu_sharing.service.relations.RelationServiceAdapter;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.search.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class AppContextConfig {

    @Bean
    public AppContextRegistry appContextRegistry() {
        return new AppContextRegistry.Builder()
                .fallbackAppContext()
                .defineBean(NodeService.class, NodeServiceAdapter.class)
                .defineBean(PermissionService.class, PermissionServiceAdapter.class)
                .defineBean(SearchService.class, SearchServiceAdapter.class)
                .defineBean(CommentService.class, CommentServiceAdapter.class)
                .defineBean(FeedbackService.class, FeedbackServiceAdapter.class)
                .defineBean(RatingService.class, RatingServiceAdapter.class)
                .defineBean(RelationService.class, RelationServiceAdapter.class)
                .defineBean(NotificationService.class, NotificationService.class)
                .defineBean(RenderingService.class, RenderingService.class)
                .defineBean(CollectionService.class, CollectionService.class)
                .done()

                .localAppContext()
                .defineBean(NodeService.class, "nodeService")
                .defineBean(PermissionService.class, "permissionService")
                .defineBean(SearchService.class, SearchServiceElastic.class)
                .defineBean(CommentService.class, CommentServiceImpl.class)
                .defineBean(FeedbackService.class, FeedbackServiceImpl.class)
                .defineBean(RatingService.class, "ratingService")
                .defineBean(RelationService.class, "relationService")
                .defineBean(NodeRelationTraceService.class, NodeRelationTraceService.class)
                .defineBean(CollectionService.class, CollectionServiceElastic.class)
                .defineBean(AuthenticationTool.class, AuthenticationToolAPI.class)
                .defineBean(AuthorityService.class, "authorityServiceImpl")
                .defineBean(DashboardConfigService.class, "dashboardConfigServiceImpl")
                .defineBean(OrganizationService.class, OrganizationService.class)
                .defineBean(ArchiveService.class, ArchiveServiceImpl.class)
                .done()

                .addAppContext("org.edu_sharing.service.provider.ElasticSearchProvider")
                .defineBean(NodeService.class, "nodeService")
                .defineBean(PermissionService.class, "permissionService")
                .defineBean(SearchService.class, SearchServiceElastic.class)
                .defineBean(CommentService.class, CommentServiceImpl.class)
                .defineBean(FeedbackService.class, FeedbackServiceImpl.class)
                .defineBean(RatingService.class, "ratingService")
                .defineBean(RelationService.class, "relationService")
                .defineBean(NodeRelationTraceService.class, NodeRelationTraceService.class)
                .defineBean(CollectionService.class, CollectionServiceElastic.class)
                .defineBean(AuthenticationTool.class, AuthenticationToolAPI.class)
                .defineBean(AuthorityService.class,  "authorityServiceImpl")
                .defineBean(DashboardConfigService.class, "dashboardConfigServiceImpl")
                .defineBean(OrganizationService.class, OrganizationService.class)
                .defineBean(ArchiveService.class, ArchiveServiceImpl.class)
                .done()

                .addAppContext("org.edu_sharing.service.provider.BrockhausProvider")
                .defineBean(NodeService.class, NodeServiceBrockhausImpl.class)
                .defineBean(PermissionService.class, PermissionServiceCCPublish.class)
                .defineBean(SearchService.class, SearchServiceBrockhausImpl.class)
                .done()

                .addAppContext("org.edu_sharing.service.provider.DDBProvider")
                .defineBean(NodeService.class, NodeServiceDDBImpl.class)
                .defineBean(PermissionService.class, PermissionServiceCCPublish.class)
                .defineBean(SearchService.class, SearchServiceDDBImpl.class)
                .done()

                .addAppContext("org.edu_sharing.service.provider.LearningAppsProvider")
                .defineBean(NodeService.class, NodeServiceLAppsImpl.class)
                .defineBean(PermissionService.class, PermissionServiceCCPublish.class)
                .defineBean(SearchService.class, SearchServiceLAppsImpl.class)
                .done()

                .addAppContext("org.edu_sharing.service.provider.PixabayProvider")
                .defineBean(NodeService.class, NodeServicePixabayImpl.class)
                .defineBean(PermissionService.class, PermissionServiceCCPublish.class)
                .defineBean(SearchService.class, SearchServicePixabayImpl.class)
                .done()

                .addAppContext("org.edu_sharing.service.provider.TutoryProvider")
                .defineBean(NodeService.class, NodeServiceTutoryImpl.class)
                .defineBean(PermissionService.class, PermissionServiceCCPublish.class)
                .defineBean(SearchService.class, SearchServiceTutoryImpl.class)
                .done()

                .addAppContext("org.edu_sharing.service.provider.YoutubeProvider")
                .defineBean(NodeService.class, NodeServiceYouTube.class)
                .defineBean(PermissionService.class, PermissionServiceCCPublish.class)
                .defineBean(SearchService.class, SearchServiceYouTubeImpl.class)
                .defineBean(AuthenticationTool.class, AuthenticationToolYouTube.class)
                .done()

                .addAppContext("org.edu_sharing.service.provider.OersiProvider")
                .defineBean(NodeService.class, NodeServiceOersiImpl.class)
                .defineBean(PermissionService.class, PermissionServiceCCPublish.class)
                .defineBean(SearchService.class, SearchServiceOersiImpl.class)
                .done()

                // TODO is this obsolete?
//                .addAppContext("commons.wikimedia.org")
//                .defineBean(AuthenticationTool.class, AuthenticationToolWikimedia.class)
//                .done()

                .build();
    }

}
