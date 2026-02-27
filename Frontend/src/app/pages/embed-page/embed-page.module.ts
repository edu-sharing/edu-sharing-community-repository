import { NgModule } from '@angular/core';
import { LicenseDialogModule } from '../../features/dialogs/dialog-modules/license-dialog/license-dialog.module';
import { MdsModule } from '../../features/mds/mds.module';
import { SharedModule } from '../../shared/shared.module';
import { EmbedPageRoutingModule } from './embed-page-routing.module';
import { EmbedPageComponent } from './embed-page.component';
import { GenericWidgetComponent } from '../topic-page/widgets/generic-widget/generic-widget.component';
import { AdminPageModule } from '../admin-page/admin-page.module';

@NgModule({
    declarations: [EmbedPageComponent],
    imports: [
        SharedModule,
        GenericWidgetComponent,
        EmbedPageRoutingModule,
        MdsModule,
        LicenseDialogModule,
        AdminPageModule,
    ],
})
export class EmbedPageModule {}
