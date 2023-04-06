import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { SavedSearchesDialogComponent } from './saved-searches-dialog.component';
import { NodeEntriesModule } from 'ngx-edu-sharing-ui';

export { SavedSearchesDialogComponent };

@NgModule({
    declarations: [SavedSearchesDialogComponent],
    imports: [SharedModule, NodeEntriesModule],
})
export class SavedSearchesDialogModule {}
