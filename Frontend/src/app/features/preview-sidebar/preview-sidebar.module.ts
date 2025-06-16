import { NgModule } from '@angular/core';
import { NodeEntriesModule } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../shared/shared.module';
import { PreviewContentComponent } from './preview-content.component';
import { PreviewSidebarComponent } from './preview-sidebar.component';
import { MdsModule } from '../mds/mds.module';
import { CommonModule } from '@angular/common';
import { RenderWrapperComponent } from '../../pages/render2-page/render-wrapper-component/render-wrapper.component';

@NgModule({
    declarations: [PreviewSidebarComponent, PreviewContentComponent],
    imports: [CommonModule, SharedModule, NodeEntriesModule, MdsModule, RenderWrapperComponent],
    exports: [PreviewSidebarComponent],
})
export class PreviewSidebarModule {}
