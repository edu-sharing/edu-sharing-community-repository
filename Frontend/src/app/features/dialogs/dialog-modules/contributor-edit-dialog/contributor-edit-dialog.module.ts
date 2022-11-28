import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ContributorEditDialogComponent } from './contributor-edit-dialog.component';

export { ContributorEditDialogComponent };

@NgModule({
    declarations: [ContributorEditDialogComponent],
    imports: [SharedModule],
})
export class ContributorEditDialogModule {}
