import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { DeleteNodesDialogComponent } from './delete-nodes-dialog.component';

export { DeleteNodesDialogComponent };

@NgModule({
    declarations: [DeleteNodesDialogComponent],
    imports: [SharedModule],
})
export class DeleteNodesDialogModule {}
