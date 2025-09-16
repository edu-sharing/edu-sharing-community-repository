import { NgModule } from '@angular/core';
import { QRCodeComponent } from 'angularx-qrcode';
import { SharedModule } from '../../../../shared/shared.module';
import { ShareLinkDialogComponent } from './share-link-dialog.component';

export { ShareLinkDialogComponent };

@NgModule({
    declarations: [ShareLinkDialogComponent],
    imports: [QRCodeComponent, SharedModule],
})
export class ShareLinkDialogModule {}
