import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { MdsModule } from '../../../mds/mds.module';
import { SendFeedbackDialogComponent } from './send-feedback-dialog.component';

export { SendFeedbackDialogComponent };

@NgModule({
    declarations: [SendFeedbackDialogComponent],
    imports: [SharedModule, MdsModule],
})
export class SendFeedbackDialogModule {}
