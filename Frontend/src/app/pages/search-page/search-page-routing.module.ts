import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SearchPageResultsAllComponent } from './search-page-results-all.component';
import { SearchPageResultsComponent } from './search-page-results.component';

import { SearchPageComponent } from './search-page.component';

const routes: Routes = [
    {
        path: '',
        component: SearchPageComponent,
        children: [
            {
                path: '',
                component: SearchPageResultsComponent,
            },
            {
                path: 'all',
                component: SearchPageResultsAllComponent,
            },
        ],
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class SearchPageRoutingModule {}
