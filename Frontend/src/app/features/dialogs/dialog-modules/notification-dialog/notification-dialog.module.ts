import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NotificationDialogComponent } from './notification-dialog.component';

export { NotificationDialogComponent };

@NgModule({
    declarations: [NotificationDialogComponent],
    imports: [SharedModule],
})
export class NotificationDialogModule {}
