import { NgModule } from '@angular/core';
import { EditorialSidebarModule } from '../../features/editorial-sidebar/editorial-sidebar.module';
import { ImprintPrivacyComponent } from '../../shared/components/imprint-privacy-footer/imprint-privacy.component';
import { ResizableSidenavDirective } from '../editorial-page/resizable-sidenav.directive';
import { RenderWrapperComponent } from '../render2-page/render-wrapper-component/render-wrapper.component';
import { SharedModule } from '../../shared/shared.module';
import { SharingPageRoutingModule } from './sharing-page-routing.module';
import { SharingPageComponent } from './sharing-page.component';

@NgModule({
    declarations: [SharingPageComponent],
    imports: [
        SharedModule,
        SharingPageRoutingModule,
        RenderWrapperComponent,
        ResizableSidenavDirective,
        ImprintPrivacyComponent,
        EditorialSidebarModule,
    ],
})
export class SharingPageModule {}
