import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ShareLinkDialogComponent } from './share-link-dialog.component';

export { ShareLinkDialogComponent };

@NgModule({
    declarations: [ShareLinkDialogComponent],
    imports: [SharedModule],
})
export class ShareLinkDialogModule {}
