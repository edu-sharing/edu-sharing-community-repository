import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { PdfMetadataPageComponent } from './pdf-metadata-page.component';

const routes: Routes = [
    {
        path: ':nodeId',
        component: PdfMetadataPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class PdfMetadataPageRoutingModule {}
