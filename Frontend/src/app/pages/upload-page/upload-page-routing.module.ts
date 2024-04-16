import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UploadPageComponent } from './upload-page.component';

const routes: Routes = [
    {
        path: '',
        component: UploadPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class UploadPageRoutingModule {}
