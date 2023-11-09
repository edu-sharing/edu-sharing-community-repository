import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { OerPageComponent } from './oer-page.component';

const routes: Routes = [
    {
        path: '',
        component: OerPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class OerPageRoutingModule {}
