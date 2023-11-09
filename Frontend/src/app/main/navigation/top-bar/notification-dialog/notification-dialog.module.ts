import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NotificationDialogComponent } from './notification-dialog.component';
import { NotificationListComponent } from './notification-list/notification-list.component';
import { NotificationListEntryComponent } from './notification-list-entry/notification-list-entry.component';

export { NotificationDialogComponent };

@NgModule({
    declarations: [
        NotificationDialogComponent,
        NotificationListComponent,
        NotificationListEntryComponent,
    ],
    imports: [SharedModule],
    exports: [NotificationDialogComponent, NotificationListComponent],
})
export class NotificationDialogModule {}
