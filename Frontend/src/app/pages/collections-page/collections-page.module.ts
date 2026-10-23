import { NgModule } from '@angular/core';
import { MdsModule } from '../../features/mds/mds.module';
import { SharedModule } from '../../shared/shared.module';
import { CollectionContentComponent } from './collection-content/collection-content.component';
import { CollectionHeaderBarComponent } from './collection-header-bar/collection-header-bar.component';
import { CollectionNewComponent } from './collection-new/collection-new.component';
import { CollectionsPageRoutingModule } from './collections-page-routing.module';
import { CollectionsPageComponent } from './collections-page.component';
import { InfobarComponent } from './infobar/infobar.component';
import { CollectionProposalsComponent } from './collection-proposals/collection-proposals.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { ShareDialogModule } from '../../features/dialogs/dialog-modules/share-dialog/share-dialog.module';
import { ResizableSidenavDirective } from '../editorial-page/resizable-sidenav.directive';
import { MatSidenavModule } from '@angular/material/sidenav';
import { ViewToggleGroupComponent } from '../../shared/components/view-toggle-group/view-toggle-group.component';
import { ScrollStartOverflowDirective } from '../../shared/directives/scroll-start-overflow.directive';
import { CollectionDesignFormComponent } from '../../features/collection-info/collection-design-form/collection-design-form.component';
import { CollectionEditorialGroupsFormComponent } from '../../features/collection-info/collection-editorial-groups-form/collection-editorial-groups-form.component';
import { EditorialSidebarModule } from '../../features/editorial-sidebar/editorial-sidebar.module';

@NgModule({
    declarations: [
        CollectionContentComponent,
        CollectionNewComponent,
        CollectionProposalsComponent,
        CollectionsPageComponent,
        InfobarComponent,
    ],
    imports: [
        SharedModule,
        ShareDialogModule,
        MdsModule,
        CollectionsPageRoutingModule,
        FooterComponent,
        MatSidenavModule,
        ResizableSidenavDirective,
        EditorialSidebarModule,
        CollectionHeaderBarComponent,
        CollectionDesignFormComponent,
        CollectionEditorialGroupsFormComponent,
        ViewToggleGroupComponent,
        ScrollStartOverflowDirective,
    ],
    exports: [CollectionContentComponent],
})
export class CollectionsPageModule {}
