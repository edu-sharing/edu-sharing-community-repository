import { NgModule } from '@angular/core';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { MdsModule } from '../../features/mds/mds.module';
import { SharedModule } from '../../shared/shared.module';
import { AdminPageRoutingModule } from './admin-page-routing.module';
import { AdminPageComponent } from './admin-page.component';
import { AutocompleteComponent } from './autocomplete/autocomplete.component';
import { AdminConfigComponent } from './config/config.component';
import { AdminFrontpageComponent } from './frontpage/frontpage.component';
import { LtiAdminComponent } from './lti-admin/lti-admin.component';
import { LtitoolAdminComponent } from './ltitool-admin/ltitool-admin.component';
import { LuceneTemplateMemoryComponent } from './lucene-template-memory/lucene-template-memory.component';
import { AdminMediacenterComponent } from './mediacenter/mediacenter.component';
import { AdminContributorsComponent } from './contributors/contributors.component';
import { AdminPluginsComponent } from './plugins/plugins.component';
import { AdminStatisticsComponent } from './statistics/statistics.component';
import { CodeEditorComponent } from './code-editor/code-editor';
import { AdminContextComponent } from './context/context.component';
import { AdminMessagesComponent } from './messages/messages.component';

@NgModule({
    declarations: [
        AdminConfigComponent,
        AdminFrontpageComponent,
        AdminMediacenterComponent,
        AdminContributorsComponent,
        AdminPageComponent,
        AdminPluginsComponent,
        AdminContextComponent,
        AdminStatisticsComponent,
        AutocompleteComponent,
        CodeEditorComponent,
        LtiAdminComponent,
        LtitoolAdminComponent,
        LuceneTemplateMemoryComponent,
    ],
    imports: [
        SharedModule,
        AdminPageRoutingModule,
        AdminMessagesComponent,
        MdsModule,
        MonacoEditorModule.forRoot({
            baseUrl: window.location.origin + '/edu-sharing/assets/monaco/min/vs',
        }),
    ],
    exports: [CodeEditorComponent],
})
export class AdminPageModule {}
