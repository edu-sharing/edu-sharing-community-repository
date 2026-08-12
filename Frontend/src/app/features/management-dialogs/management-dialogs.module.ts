import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { MdsModule } from '../mds/mds.module';
import { AddStreamComponent } from './add-stream/add-stream.component';
import { WorkspaceManagementDialogsComponent } from './management-dialogs.component';

@NgModule({
    declarations: [WorkspaceManagementDialogsComponent, AddStreamComponent],
    imports: [SharedModule, MdsModule],
    exports: [WorkspaceManagementDialogsComponent],
})
export class ManagementDialogsModule {}
