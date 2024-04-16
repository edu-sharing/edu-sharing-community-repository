import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EmbedPageComponent } from './embed-page.component';

const routes: Routes = [
    {
        path: ':component',
        component: EmbedPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class EmbedPageRoutingModule {}
