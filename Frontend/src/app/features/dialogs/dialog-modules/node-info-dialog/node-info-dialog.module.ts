import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeInfoDialogComponent } from './node-info-dialog.component';

export { NodeInfoDialogComponent as NodeInfoComponent };

@NgModule({
    declarations: [NodeInfoDialogComponent],
    imports: [SharedModule],
})
export class NodeInfoDialogModule {}
