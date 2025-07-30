import { NgModule } from '@angular/core';
import { PdfMetadataPageComponent } from './pdf-metadata-page.component';
import { CommonModule } from '@angular/common';
import { PdfMetadataPageRoutingModule } from './pdf-metadata-page-routing.module';

@NgModule({
    declarations: [PdfMetadataPageComponent],
    imports: [CommonModule, PdfMetadataPageRoutingModule],
})
export class PdfMetadataPageModule {}
