import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { QRCodeComponent } from 'angularx-qrcode';
import { QrDialogComponent } from './qr-dialog.component';
import { QrContentComponent } from './qr-content/qr-content.component';

export { QrDialogComponent };

@NgModule({
    declarations: [QrDialogComponent, QrContentComponent],
    imports: [CommonModule, QRCodeComponent],
    exports: [
        // Export for embed app
        QrContentComponent,
    ],
})
export class QrDialogModule {}
