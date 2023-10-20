import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { JoinGroupDialogComponent } from './join-group-dialog.component';

export { JoinGroupDialogComponent };

@NgModule({
    declarations: [JoinGroupDialogComponent],
    imports: [SharedModule],
})
export class JoinGroupDialogModule {}
