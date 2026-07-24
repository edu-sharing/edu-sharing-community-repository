import { NgModule } from '@angular/core';
import { EduSharingUiCommonModule, NodeEntriesModule } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../shared/shared.module';
import { MdsModule } from '../mds/mds.module';
import { CommonModule } from '@angular/common';
import { RenderWrapperComponent } from '../../pages/render2-page/render-wrapper-component/render-wrapper.component';
import { PreviewSidebarComponent } from './preview-sidebar/preview-sidebar.component';
import { PreviewSidebarWrapperComponent } from './preview-sidebar/preview-sidebar-wrapper/preview-sidebar-wrapper.component';
import { PreviewContentComponent } from './preview-sidebar/preview-content/preview-content.component';
import { EditorialSidebarComponent } from './editorial-sidebar.component';
import { CdkMonitorFocus } from '@angular/cdk/a11y';
import { MatButtonModule } from '@angular/material/button';
import { EdgeToggleComponent } from '../../shared/components/edge-toggle/edge-toggle.component';
import { TranslateModule } from '@ngx-translate/core';
import { SubmissionSidebarComponent } from '../../pages/editorial-page/submission-sidebar/submission-sidebar.component';
import { NodesSelectorComponent } from '../../pages/editorial-page/nodes-selector/nodes-selector.component';
import { MetadataSidebarComponent } from '../../pages/workspace-page/metadata/metadata-sidebar.component';
import { UsagesPreviewComponent } from '../../pages/workspace-page/metadata/usages/usages-preview.component';
import { AssignmentSidebarComponent } from '../../pages/editorial-page/assignment-sidebar/assignment-sidebar.component';
import { ResizableSidenavDirective } from '../../pages/editorial-page/resizable-sidenav.directive';

@NgModule({
    declarations: [
        EditorialSidebarComponent,
        PreviewSidebarComponent,
        PreviewSidebarWrapperComponent,
        PreviewContentComponent,
    ],
    imports: [
        EduSharingUiCommonModule,
        CdkMonitorFocus,
        EdgeToggleComponent,
        CommonModule,
        MatButtonModule,
        TranslateModule,
        AssignmentSidebarComponent,
        SubmissionSidebarComponent,
        NodesSelectorComponent,
        MetadataSidebarComponent,
        UsagesPreviewComponent,
        CommonModule,
        SharedModule,
        NodeEntriesModule,
        MdsModule,
        RenderWrapperComponent,
        ResizableSidenavDirective,
    ],
    exports: [EditorialSidebarComponent, PreviewSidebarComponent, PreviewSidebarWrapperComponent],
})
export class EditorialSidebarModule {}
