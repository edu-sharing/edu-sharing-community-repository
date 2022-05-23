import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { QrRoutingModule } from './qr-routing.module';
import { QrComponent } from './qr.component';
import { QrDialogModule } from '../../../../../src/app/features/dialogs/dialog-modules/qr-dialog/qr-dialog.module';

@NgModule({
    declarations: [QrComponent],
    imports: [CommonModule, QrRoutingModule, QrDialogModule],
})
export class QrModule {}
