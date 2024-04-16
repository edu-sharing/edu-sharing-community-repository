import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CollectionNewComponent } from './collection-new/collection-new.component';
import { CollectionsPageComponent } from './collections-page.component';

const routes: Routes = [
    {
        path: '',
        component: CollectionsPageComponent,
    },
    {
        path: 'collection/:mode/:id',
        component: CollectionNewComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class CollectionsPageRoutingModule {}
