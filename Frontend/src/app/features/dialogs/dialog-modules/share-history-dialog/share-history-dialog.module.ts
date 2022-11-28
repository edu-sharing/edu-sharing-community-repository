import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ShareHistoryDialogComponent } from './share-history-dialog.component';

export { ShareHistoryDialogComponent };

@NgModule({
    declarations: [ShareHistoryDialogComponent],
    imports: [SharedModule],
})
export class ShareHistoryDialogModule {}
