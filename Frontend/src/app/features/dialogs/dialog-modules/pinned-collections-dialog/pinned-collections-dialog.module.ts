import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { PinnedCollectionsDialogComponent } from './pinned-collections-dialog.component';

export { PinnedCollectionsDialogComponent };

@NgModule({
    declarations: [PinnedCollectionsDialogComponent],
    imports: [SharedModule],
})
export class PinnedCollectionsDialogModule {}
