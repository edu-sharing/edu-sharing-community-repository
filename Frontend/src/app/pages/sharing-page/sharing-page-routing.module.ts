import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharingPageComponent } from './sharing-page.component';

const routes: Routes = [
    {
        path: '',
        component: SharingPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class SharingPageRoutingModule {}
