import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ContributorRegistryEditDialogComponent } from './contributor-registry-edit-dialog.component';

export { ContributorRegistryEditDialogComponent };

@NgModule({
    declarations: [ContributorRegistryEditDialogComponent],
    imports: [SharedModule],
})
export class ContributorRegistryEditDialogModule {}
