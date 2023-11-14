import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { MdsModule } from '../mds/mds.module';
import { AddStreamComponent } from './add-stream/add-stream.component';
import { WorkspaceManagementDialogsComponent } from './management-dialogs.component';
import { ViewMaterialFeedbackComponent } from './view-material-feedback/view-material-feedback.component';

@NgModule({
    declarations: [
        WorkspaceManagementDialogsComponent,
        AddStreamComponent,
        ViewMaterialFeedbackComponent,
    ],
    imports: [SharedModule, MdsModule],
    exports: [WorkspaceManagementDialogsComponent],
})
export class ManagementDialogsModule {}
