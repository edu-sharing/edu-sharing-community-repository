import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { QRCodeComponent } from 'angularx-qrcode';
import { SharedModule } from '../../../../shared/shared.module';
import { QrDialogComponent } from './qr-dialog.component';
import { QrContentComponent } from './qr-content/qr-content.component';

export { QrDialogComponent };

@NgModule({
    declarations: [QrDialogComponent, QrContentComponent],
    imports: [CommonModule, QRCodeComponent, TranslateModule, SharedModule],
    exports: [
        // Export for embed app
        QrContentComponent,
    ],
})
export class QrDialogModule {}
