import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { EditorialPageComponent } from './editorial-page.component';
import { EditorialPageRoutingModule } from './editorial-page-routing.module';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ResizableSidenavDirective } from './resizable-sidenav.directive';
import { EditorialBreadcrumbComponent } from './editorial-breadcrumb/editorial-breadcrumb.component';
import { MdsModule } from '../../features/mds/mds.module';
import { EduSharingUiCommonModule, InfoMessageComponent } from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';
import { ManageAssignmentComponent } from './manage-assignment/manage-assignment.component';
import { ManageAssignmentNodesComponent } from './manage-assignment-nodes/manage-assignment-nodes.component';
import { AssignmentSubmissionComponent } from './assignment-submission/assignment-submission.component';
import { EditorialBreadcrumbService } from './editorial-breadcrumb/editorial-breadcrumb.service';
import { SubmitAssignmentComponent } from './submit-assignment/submit-assignment.component';
import { EditorialSidebarModule } from '../../features/editorial-sidebar/editorial-sidebar.module';
import { EdgeToggleComponent } from '../../shared/components/edge-toggle/edge-toggle.component';
import { ScopeStatusCellComponent } from './scope-status-cell/scope-status-cell.component';
import { ScopeSettingsComponent } from './scope-settings/scope-settings.component';

@NgModule({
    declarations: [EditorialPageComponent],
    imports: [
        ResizableSidenavDirective,
        EdgeToggleComponent,
        EditorialBreadcrumbComponent,
        EditorialSidebarModule,
        ManageAssignmentComponent,
        AssignmentSubmissionComponent,
        ManageAssignmentNodesComponent,
        SubmitAssignmentComponent,
        ScopeStatusCellComponent,
        ScopeSettingsComponent,
        InfoMessageComponent,
        MatToolbarModule,
        EditorialPageRoutingModule,
        EduSharingUiCommonModule,
        SharedModule,
        MdsModule,
        TranslateModule,
    ],
    providers: [EditorialBreadcrumbService],
})
export class EditorialPageModule {}
