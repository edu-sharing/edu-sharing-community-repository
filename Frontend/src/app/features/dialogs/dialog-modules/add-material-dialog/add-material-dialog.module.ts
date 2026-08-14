import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { LocationPickerComponent } from '../../../location-picker/location-picker.component';
import { AddMaterialDialogComponent } from './add-material-dialog.component';

export { AddMaterialDialogComponent };

@NgModule({
    declarations: [AddMaterialDialogComponent],
    imports: [SharedModule, LocationPickerComponent],
    exports: [AddMaterialDialogComponent],
})
export class AddMaterialDialogModule {}
