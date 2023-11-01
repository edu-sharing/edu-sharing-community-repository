import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { InputDialogComponent } from './input-dialog.component';

export { InputDialogComponent };

@NgModule({
    declarations: [InputDialogComponent],
    imports: [SharedModule],
})
export class InputDialogModule {}
