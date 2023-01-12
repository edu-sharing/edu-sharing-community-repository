import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { MdsModule } from '../../../mds/mds.module';
import { NodeTemplateDialogComponent } from './node-template-dialog.component';

export { NodeTemplateDialogComponent };

@NgModule({
    declarations: [NodeTemplateDialogComponent],
    imports: [SharedModule, MdsModule],
})
export class NodeTemplateDialogModule {}
