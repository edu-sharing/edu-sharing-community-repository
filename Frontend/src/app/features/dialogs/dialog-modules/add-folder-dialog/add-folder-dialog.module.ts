import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { AddFolderDialogComponent } from './add-folder-dialog.component';

export { AddFolderDialogComponent };

@NgModule({
    declarations: [AddFolderDialogComponent],
    imports: [SharedModule],
})
export class AddFolderDialogModule {}
