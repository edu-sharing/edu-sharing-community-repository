import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EditorialPageComponent } from './editorial-page.component';
import { EditorialPageService } from './editorial-page.service';

const routes: Routes = [
    {
        path: '',
        component: EditorialPageComponent,
    },
    {
        path: ':primaryMode',
        component: EditorialPageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    providers: [EditorialPageService],
    exports: [RouterModule],
})
export class EditorialPageRoutingModule {}
