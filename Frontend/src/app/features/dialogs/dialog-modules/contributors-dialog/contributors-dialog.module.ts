import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ContributorsDialogComponent } from './contributors-dialog.component';

export { ContributorsDialogComponent };

@NgModule({
    declarations: [ContributorsDialogComponent],
    imports: [SharedModule],
})
export class ContributorDialogModule {}
