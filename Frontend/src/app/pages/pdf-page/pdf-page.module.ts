import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { PdfPageComponent } from './pdf-page.component';
import { PdfPageRoutingModule } from './pdf-page-routing.module';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';

@NgModule({
    declarations: [PdfPageComponent],
    imports: [SharedModule, PdfPageRoutingModule, NgxExtendedPdfViewerModule],
})
export class PdfPageModule {}
