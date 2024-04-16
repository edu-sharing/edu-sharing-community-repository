import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeEntriesModule } from 'ngx-edu-sharing-ui';
import { FileChooserDialogComponent } from './file-chooser-dialog.component';

export { FileChooserDialogComponent };

@NgModule({
    declarations: [FileChooserDialogComponent],
    imports: [SharedModule, NodeEntriesModule],
})
export class FileChooserDialogModule {}
