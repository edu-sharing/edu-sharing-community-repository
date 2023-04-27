import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeEntriesModule } from '../../../node-entries/node-entries.module';
import { SavedSearchesDialogComponent } from './saved-searches-dialog.component';

export { SavedSearchesDialogComponent };

@NgModule({
    declarations: [SavedSearchesDialogComponent],
    imports: [SharedModule, NodeEntriesModule],
})
export class SavedSearchesDialogModule {}
