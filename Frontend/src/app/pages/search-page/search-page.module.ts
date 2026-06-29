import { NgModule } from '@angular/core';
import { MdsModule } from '../../features/mds/mds.module';
import { SharedModule } from '../../shared/shared.module';
import { RepositoryIconPipe } from './repository-icon.pipe';
import { SearchPageFiltersSidebarComponent } from './search-page-filters-sidebar.component';
import { SearchPageResultsAllComponent } from './search-page-results-all.component';
import { SearchPageResultsComponent } from './search-page-results.component';
import { SearchPageRoutingModule } from './search-page-routing.module';
import { SearchPageComponent } from './search-page.component';
import { SearchPageToolbarComponent } from './search-page-toolbar.component';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { SearchPageFilterBarComponent } from './search-page-filter-bar.component';
import { ResizableSidenavDirective } from '../editorial-page/resizable-sidenav.directive';
import { EditorialSidebarModule } from '../../features/editorial-sidebar/editorial-sidebar.module';

@NgModule({
    declarations: [
        SearchPageComponent,
        SearchPageToolbarComponent,
        SearchPageFilterBarComponent,
        SearchPageFiltersSidebarComponent,
        RepositoryIconPipe,
        SearchPageToolbarComponent,
        SearchPageResultsComponent,
        SearchPageResultsAllComponent,
    ],
    imports: [
        SearchPageRoutingModule,
        EditorialSidebarModule,
        SharedModule,
        EduSharingUiModule,
        MdsModule,
        FooterComponent,
        ResizableSidenavDirective,
    ],
    // This module is lazy-loaded and should not export anything.
    exports: [],
})
export class SearchPageModule {}
