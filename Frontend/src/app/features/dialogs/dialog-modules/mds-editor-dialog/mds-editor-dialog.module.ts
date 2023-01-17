import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { MdsModule } from '../../../mds/mds.module';
import { InputFillProgressComponent } from './input-fill-progress/input-fill-progress.component';
import { MdsEditorDialogComponent } from './mds-editor-dialog.component';

export { MdsEditorDialogComponent };

@NgModule({
    declarations: [MdsEditorDialogComponent, InputFillProgressComponent],
    imports: [SharedModule, MdsModule],
})
export class MdsEditorDialogModule {}
