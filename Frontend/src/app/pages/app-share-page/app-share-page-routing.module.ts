import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AppSharePageComponent } from './app-share-page.component';

const routes: Routes = [
    {
        path: '',
        component: AppSharePageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class AppSharePageRoutingModule {}
