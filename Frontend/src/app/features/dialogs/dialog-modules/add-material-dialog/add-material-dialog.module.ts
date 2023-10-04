import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { AddMaterialDialogComponent } from './add-material-dialog.component';

export { AddMaterialDialogComponent };

@NgModule({
    declarations: [AddMaterialDialogComponent],
    imports: [SharedModule],
})
export class AddMaterialDialogModule {}
