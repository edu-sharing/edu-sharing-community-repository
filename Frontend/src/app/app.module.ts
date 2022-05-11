import { DragDropModule } from '@angular/cdk/drag-drop';
import { NgModule } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from "@angular/material/form-field";
import { MatTooltipDefaultOptions, MAT_TOOLTIP_DEFAULT_OPTIONS } from "@angular/material/tooltip";
import { ResizableModule } from 'angular-resizable-element';
import { EduSharingApiConfigurationParams, EduSharingApiModule, EDU_SHARING_API_CONFIG } from 'ngx-edu-sharing-api';
import { NodeEmbedComponent } from './common/ui/node-embed/node-embed.component';
import { ErrorHandlerService } from './core-ui-module/error-handler.service';
import { DECLARATIONS } from './declarations';
import { extensionDeclarations } from './extension/extension-declarations';
import { extensionImports } from './extension/extension-imports';
import { extensionProviders } from './extension/extension-providers';
import { MdsModule } from './features/mds/mds.module';
import { IMPORTS } from './imports';
import { MainModule } from './main/main.module';
import { DECLARATIONS_ADMIN } from './modules/admin/declarations';
import { LtiAdminComponent } from './modules/admin/lti-admin/lti-admin.component';
import { LuceneTemplateMemoryComponent } from './modules/admin/lucene-template-memory/lucene-template-memory.component';
import { DECLARATIONS_COLLECTIONS } from './modules/collections/declarations';
import { DECLARATIONS_FILE_UPLOAD } from './modules/file-upload/declarations';
import { DECLARATIONS_LOGINAPP } from './modules/login-app/declarations';
import { DECLARATIONS_LOGIN } from './modules/login/declarations';
import { LtiComponent } from './modules/lti/lti.component';
import { DECLARATIONS_MANAGEMENT_DIALOGS } from './modules/management-dialogs/declarations';
import { DECLARATIONS_MESSAGES } from './modules/messages/declarations';
import { DECLARATIONS_RECYCLE } from './modules/node-list/declarations';
import { DECLARATIONS_OER } from './modules/oer/declarations';
import { DECLARATIONS_PERMISSIONS } from './modules/permissions/declarations';
import { DECLARATIONS_PROFILES } from './modules/profiles/declarations';
import { DECLARATIONS_REGISTER } from './modules/register/declarations';
import { DECLARATIONS_SEARCH } from './modules/search/declarations';
import { PROVIDERS_SEARCH } from './modules/search/providers';
import { DECLARATIONS_SERVICES } from "./modules/services/declarations";
import { DECLARATIONS_SHARE_APP } from './modules/share-app/declarations';
import { DECLARATIONS_SHARING } from './modules/sharing/declarations';
import { DECLARATIONS_STARTUP } from './modules/startup/declarations';
import { DECLARATIONS_STREAM } from './modules/stream/declarations';
import { DECLARATIONS_WORKSPACE } from './modules/workspace/declarations';
import { PROVIDERS } from './providers';
import { RouterComponent } from './router/router.component';
import { SharedModule } from './shared/shared.module';
import { TranslationsModule } from './translations/translations.module';
import { MainModule } from './main/main.module';
import {ApolloClientOptions, InMemoryCache} from '@apollo/client';
import {HttpLink} from 'apollo-angular/http';
import {APOLLO_OPTIONS, ApolloModule} from 'apollo-angular';


// http://blog.angular-university.io/angular2-ngmodule/
// -> Making modules more readable using the spread operator

const matTooltipDefaultOptions: MatTooltipDefaultOptions = {
    showDelay: 500,
    hideDelay: 0,
    touchendHideDelay: 0,
}


@NgModule({
    declarations: [
        DECLARATIONS,
        DECLARATIONS_RECYCLE,
        DECLARATIONS_WORKSPACE,
        DECLARATIONS_SEARCH,
        DECLARATIONS_COLLECTIONS,
        DECLARATIONS_LOGIN,
        DECLARATIONS_REGISTER,
        DECLARATIONS_LOGINAPP,
        DECLARATIONS_FILE_UPLOAD,
        DECLARATIONS_STARTUP,
        DECLARATIONS_PERMISSIONS,
        DECLARATIONS_OER,
        DECLARATIONS_STREAM,
        DECLARATIONS_MANAGEMENT_DIALOGS,
        DECLARATIONS_ADMIN,
        DECLARATIONS_PROFILES,
        DECLARATIONS_MESSAGES,
        DECLARATIONS_SHARING,
        DECLARATIONS_SHARE_APP,
        DECLARATIONS_SERVICES,
        LuceneTemplateMemoryComponent,
        extensionDeclarations,
        LtiComponent,
        LtiAdminComponent,
        NodeEmbedComponent,
    ],
    imports: [
        IMPORTS,
        ApolloModule,
        SharedModule,
        MainModule,
        EduSharingApiModule.forRoot(),
        EduSharingGraphqlModule,
        TranslationsModule.forRoot(),
        DragDropModule,
        extensionImports,
        ResizableModule,
        MdsModule,
    ],
    providers: [
        {
            provide: EDU_SHARING_API_CONFIG,
            deps: [ErrorHandlerService],
            useFactory: (errorHandler: ErrorHandlerService) => ({
                onError: (err, req) => errorHandler.handleError(err, req),
            } as EduSharingApiConfigurationParams),
        },
        {
            provide: APOLLO_OPTIONS,
            useFactory: (httpLink: HttpLink) => {
                return {
                    link: httpLink.create({ uri: '/edu-sharing/graphql' }),
                    cache: new InMemoryCache(),
                };
            },
            deps: [HttpLink],
        },
        PROVIDERS,
        PROVIDERS_SEARCH,
        {provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: {appearance: 'outline'}},
        {provide: MAT_TOOLTIP_DEFAULT_OPTIONS, useValue: matTooltipDefaultOptions},
        extensionProviders,
        ErrorHandlerService,
    ],
    exports: [
        DECLARATIONS,
        DECLARATIONS_RECYCLE,
        DECLARATIONS_WORKSPACE,
        DECLARATIONS_SEARCH,
        DECLARATIONS_COLLECTIONS,
        DECLARATIONS_LOGIN,
        DECLARATIONS_REGISTER,
        DECLARATIONS_LOGINAPP,
        DECLARATIONS_FILE_UPLOAD,
        DECLARATIONS_STARTUP,
        DECLARATIONS_PERMISSIONS,
        DECLARATIONS_OER,
        DECLARATIONS_STREAM,
        DECLARATIONS_MANAGEMENT_DIALOGS,
        DECLARATIONS_ADMIN,
        DECLARATIONS_PROFILES,
        DECLARATIONS_MESSAGES,
        DECLARATIONS_SHARING,
        DECLARATIONS_SHARE_APP,
        DECLARATIONS_SERVICES
    ],
    bootstrap: [RouterComponent]
})
export class AppModule { }
