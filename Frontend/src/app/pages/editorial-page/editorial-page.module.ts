import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { EditorialPageComponent } from './editorial-page.component';
import { EditorialPageRoutingModule } from './editorial-page-routing.module';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ResizableSidenavDirective } from './resizable-sidenav.directive';
import { EditorialBreadcrumbComponent } from './editorial-breadcrumb/editorial-breadcrumb.component';
import { MdsModule } from '../../features/mds/mds.module';
import { EditorialSidebarComponent } from './editorial-sidebar/editorial-sidebar.component';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';
import { ManageAssignmentComponent } from './manage-assignment/manage-assignment.component';
import { ManageAssignmentNodesComponent } from './manage-assignment-nodes/manage-assignment-nodes.component';
import { AssignmentSubmissionComponent } from './assignment-submission/assignment-submission.component';
import { EditorialBreadcrumbService } from './editorial-breadcrumb/editorial-breadcrumb.service';

@NgModule({
    declarations: [EditorialPageComponent],
    imports: [
        ResizableSidenavDirective,
        EditorialBreadcrumbComponent,
        EditorialSidebarComponent,
        ManageAssignmentComponent,
        AssignmentSubmissionComponent,
        ManageAssignmentNodesComponent,
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
