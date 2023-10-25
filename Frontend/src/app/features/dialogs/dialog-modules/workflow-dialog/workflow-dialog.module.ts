import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { WorkflowDialogComponent } from './workflow-dialog.component';
import { WorkflowListComponent } from './workflow-history/workflow-history.component';

export { WorkflowDialogComponent };

@NgModule({
    declarations: [WorkflowDialogComponent, WorkflowListComponent],
    imports: [SharedModule],
})
export class WorkflowDialogModule {}
