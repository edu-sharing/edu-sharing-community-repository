import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RenderMainPageComponent } from './render-main-page.component';

const routes: Routes = [
    {
        path: ':node',
        component: RenderMainPageComponent,
    },
    {
        path: ':node/:version',
        component: RenderMainPageComponent,
    },
];

@NgModule({
    imports: [RenderMainPageComponent, RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class RenderMainPageRoutingModule {}
