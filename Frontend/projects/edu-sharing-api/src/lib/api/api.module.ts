/* tslint:disable */
/* eslint-disable */
import { NgModule, ModuleWithProviders, SkipSelf, Optional } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiConfiguration, ApiConfigurationParams } from './api-configuration';

import { AboutService } from './services/about.service';
import { AdminV1Service } from './services/admin-v-1.service';
import { ArchiveV1Service } from './services/archive-v-1.service';
import { BulkV1Service } from './services/bulk-v-1.service';
import { ClientutilsV1Service } from './services/clientutils-v-1.service';
import { CollectionV1Service } from './services/collection-v-1.service';
import { CommentV1Service } from './services/comment-v-1.service';
import { ConfigV1Service } from './services/config-v-1.service';
import { ConnectorV1Service } from './services/connector-v-1.service';
import { FeedbackV1Service } from './services/feedback-v-1.service';
import { IamV1Service } from './services/iam-v-1.service';
import { KnowledgeV1Service } from './services/knowledge-v-1.service';
import { AuthenticationV1Service } from './services/authentication-v-1.service';
import { LtiV13Service } from './services/lti-v-13.service';
import { MdsV1Service } from './services/mds-v-1.service';
import { MediacenterV1Service } from './services/mediacenter-v-1.service';
import { NetworkV1Service } from './services/network-v-1.service';
import { NodeV1Service } from './services/node-v-1.service';
import { OrganizationV1Service } from './services/organization-v-1.service';
import { RatingV1Service } from './services/rating-v-1.service';
import { RegisterV1Service } from './services/register-v-1.service';
import { RelationV1Service } from './services/relation-v-1.service';
import { RenderingV1Service } from './services/rendering-v-1.service';
import { SearchV1Service } from './services/search-v-1.service';
import { SharingV1Service } from './services/sharing-v-1.service';
import { StatisticV1Service } from './services/statistic-v-1.service';
import { StreamV1Service } from './services/stream-v-1.service';
import { ToolV1Service } from './services/tool-v-1.service';
import { TrackingV1Service } from './services/tracking-v-1.service';
import { UsageV1Service } from './services/usage-v-1.service';

/**
 * Module that provides all services and configuration.
 */
@NgModule({
    imports: [],
    exports: [],
    declarations: [],
    providers: [
        AboutService,
        AdminV1Service,
        ArchiveV1Service,
        BulkV1Service,
        ClientutilsV1Service,
        CollectionV1Service,
        CommentV1Service,
        ConfigV1Service,
        ConnectorV1Service,
        FeedbackV1Service,
        IamV1Service,
        KnowledgeV1Service,
        AuthenticationV1Service,
        LtiV13Service,
        MdsV1Service,
        MediacenterV1Service,
        NetworkV1Service,
        NodeV1Service,
        OrganizationV1Service,
        RatingV1Service,
        RegisterV1Service,
        RelationV1Service,
        RenderingV1Service,
        SearchV1Service,
        SharingV1Service,
        StatisticV1Service,
        StreamV1Service,
        ToolV1Service,
        TrackingV1Service,
        UsageV1Service,
        ApiConfiguration,
    ],
})
export class ApiModule {
    static forRoot(params: ApiConfigurationParams): ModuleWithProviders<ApiModule> {
        return {
            ngModule: ApiModule,
            providers: [
                {
                    provide: ApiConfiguration,
                    useValue: params,
                },
            ],
        };
    }

    constructor(@Optional() @SkipSelf() parentModule: ApiModule, @Optional() http: HttpClient) {
        if (parentModule) {
            throw new Error('ApiModule is already loaded. Import in your base AppModule only.');
        }
        if (!http) {
            throw new Error(
                'You need to import the HttpClientModule in your AppModule! \n' +
                    'See also https://github.com/angular/angular/issues/20575',
            );
        }
    }
}
