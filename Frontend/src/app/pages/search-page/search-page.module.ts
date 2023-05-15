import { NgModule } from '@angular/core';
import { ListItemsModule } from '../../features/list-items/list-items.module';
import { MdsModule } from '../../features/mds/mds.module';
import { NodeEntriesModule } from '../../features/node-entries/node-entries.module';
import { SharedModule } from '../../shared/shared.module';
import { RepositoryIconPipe } from './repository-icon.pipe';
import { SearchPageFilterBarComponent } from './search-page-filter-bar.component';
import { SearchPageResultsAllComponent } from './search-page-results-all.component';
import { SearchPageResultsComponent } from './search-page-results.component';
import { SearchPageRoutingModule } from './search-page-routing.module';
import { SearchPageToolbarComponent } from './search-page-toolbar.component';
import { SearchPageComponent } from './search-page.component';
import { SmallCollectionComponent } from './small-collection/small-collection.component';

@NgModule({
    declarations: [
        SmallCollectionComponent,
        SearchPageComponent,
        SearchPageToolbarComponent,
        SearchPageFilterBarComponent,
        RepositoryIconPipe,
        SearchPageResultsComponent,
        SearchPageResultsAllComponent,
    ],
    imports: [SearchPageRoutingModule, SharedModule, ListItemsModule, NodeEntriesModule, MdsModule],
    exports: [SmallCollectionComponent],
})
export class SearchPageModule {}
