import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ContributorsDialogComponent } from './contributors-dialog.component';
import { ContributorsListComponent } from './contributors-list.component';

export { ContributorsDialogComponent };

@NgModule({
    declarations: [ContributorsDialogComponent, ContributorsListComponent],
    imports: [SharedModule],
})
export class ContributorDialogModule {}
