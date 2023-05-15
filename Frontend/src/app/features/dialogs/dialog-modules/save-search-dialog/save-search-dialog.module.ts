import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { SaveSearchDialogComponent } from './save-search-dialog.component';

export { SaveSearchDialogComponent };

@NgModule({
    declarations: [SaveSearchDialogComponent],
    imports: [SharedModule],
})
export class SaveSearchDialogModule {}
