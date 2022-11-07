import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { GenericDialogComponent } from './generic-dialog.component';

export { GenericDialogComponent };

@NgModule({
    declarations: [GenericDialogComponent],
    imports: [SharedModule],
})
export class GenericDialogModule {}
