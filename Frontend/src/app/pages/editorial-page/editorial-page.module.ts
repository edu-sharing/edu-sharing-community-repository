import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { EditorialPageComponent } from './editorial-page.component';
import { EditorialPageRoutingModule } from './editorial-page-routing.module';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ResizableSidenavDirective } from './resizable-sidenav.directive';
import { EditorialBreadcrumb } from './editorial-breadcrumb/editorial-breadcrumb.component';
import { MdsModule } from '../../features/mds/mds.module';

@NgModule({
    declarations: [EditorialPageComponent],
    imports: [
        ResizableSidenavDirective,
        EditorialBreadcrumb,
        MatToolbarModule,
        EditorialPageRoutingModule,
        SharedModule,
        MdsModule,
    ],
})
export class EditorialPageModule {}
