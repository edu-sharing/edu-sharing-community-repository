import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { FileUploadProgressDialogComponent } from './file-upload-progress-dialog.component';
import { TimePipe } from './time.pipe';

export { FileUploadProgressDialogComponent };

@NgModule({
    declarations: [FileUploadProgressDialogComponent, TimePipe],
    imports: [SharedModule],
})
export class FileUploadProgressDialogModule {}
