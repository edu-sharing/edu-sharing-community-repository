import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RenderPageComponent } from './render-page.component';

const routes: Routes = [
    {
        path: ':node',
        component: RenderPageComponent,
    },
    {
        path: ':node/:version',
        component: RenderPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class RenderPageRoutingModule {}
