import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { EditorialPageComponent } from './editorial-page.component';
import { EditorialPageRoutingModule } from './editorial-page-routing.module';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ResizableSidenavDirective } from './resizable-sidenav.directive';
import { EditorialBreadcrumbComponent } from './editorial-breadcrumb/editorial-breadcrumb.component';
import { MdsModule } from '../../features/mds/mds.module';
import { EditorialSidebarComponent } from './editorial-sidebar/editorial-sidebar.component';
import { BorderBoxObserverDirective, EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';

@NgModule({
    declarations: [EditorialPageComponent],
    imports: [
        ResizableSidenavDirective,
        EditorialBreadcrumbComponent,
        EditorialSidebarComponent,
        MatToolbarModule,
        EditorialPageRoutingModule,
        EduSharingUiCommonModule,
        SharedModule,
        MdsModule,
    ],
})
export class EditorialPageModule {}
