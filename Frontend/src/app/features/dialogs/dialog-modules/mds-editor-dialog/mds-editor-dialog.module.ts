import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { MdsModule } from '../../../mds/mds.module';
import { MdsEditorDialogComponent } from './mds-editor-dialog.component';

export { MdsEditorDialogComponent };

@NgModule({
    declarations: [MdsEditorDialogComponent],
    imports: [SharedModule, MdsModule],
})
export class MdsEditorDialogModule {}
