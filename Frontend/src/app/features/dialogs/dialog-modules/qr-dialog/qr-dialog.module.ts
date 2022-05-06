import { NgModule } from '@angular/core';
import { QRCodeModule } from 'angularx-qrcode';
import { SharedModule } from '../../../../shared/shared.module';
import { QrDialogComponent } from './qr-dialog.component';

export { QrDialogComponent };

@NgModule({
    declarations: [QrDialogComponent],
    imports: [SharedModule, QRCodeModule],
})
export class QrDialogModule {}
