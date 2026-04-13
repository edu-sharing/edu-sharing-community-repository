import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { WorkspaceExplorerComponent } from './explorer/explorer.component';
import { RecycleMainComponent } from './recycle/recycle.component';
import { WorkspaceSubTreeComponent } from './sub-tree/sub-tree.component';
import { WorkspaceTreeComponent } from './tree/tree.component';
import { WorkspacePageRoutingModule } from './workspace-page-routing.module';
import { WorkspacePageComponent } from './workspace-page.component';
import { ResizableSidenavDirective } from '../editorial-page/resizable-sidenav.directive';
import { EditorialSidebarModule } from '../../features/editorial-sidebar/editorial-sidebar.module';

@NgModule({
    declarations: [
        RecycleMainComponent,
        WorkspaceExplorerComponent,
        WorkspacePageComponent,
        WorkspaceSubTreeComponent,
        WorkspaceTreeComponent,
    ],
    imports: [
        SharedModule,
        WorkspacePageRoutingModule,
        EditorialSidebarModule,
        ResizableSidenavDirective,
    ],
    exports: [],
})
export class WorkspacePageModule {}
