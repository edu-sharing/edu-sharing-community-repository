import { NgModule } from '@angular/core';
import { LicenseDialogModule } from '../../features/dialogs/dialog-modules/license-dialog/license-dialog.module';
import { MdsModule } from '../../features/mds/mds.module';
import { SharedModule } from '../../shared/shared.module';
import { EmbedPageRoutingModule } from './embed-page-routing.module';
import { EmbedPageComponent } from './embed-page.component';

@NgModule({
    declarations: [EmbedPageComponent],
    imports: [SharedModule, EmbedPageRoutingModule, MdsModule, LicenseDialogModule],
})
export class EmbedPageModule {}
