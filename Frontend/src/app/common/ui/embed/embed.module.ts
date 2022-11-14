import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LicenseDialogModule } from '../../../features/dialogs/dialog-modules/license-dialog/license-dialog.module';
import { MdsModule } from '../../../features/mds/mds.module';
import { SharedModule } from '../../../shared/shared.module';
import { EmbedComponent } from './embed.component';

@NgModule({
    declarations: [EmbedComponent],
    imports: [
        RouterModule.forChild([
            {
                path: '',
                component: EmbedComponent,
            },
        ]),
        SharedModule,
        MdsModule,
        LicenseDialogModule,
    ],
})
export class EmbedModule {}
