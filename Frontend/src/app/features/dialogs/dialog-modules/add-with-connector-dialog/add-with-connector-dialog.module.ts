import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { AddWithConnectorDialogComponent } from './add-with-connector-dialog.component';

export { AddWithConnectorDialogComponent };

@NgModule({
    declarations: [AddWithConnectorDialogComponent],
    imports: [SharedModule],
})
export class AddWithConnectorDialogModule {}
