import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { MdsModule } from '../../../mds/mds.module';
import { SimpleEditDialogComponent } from './simple-edit-dialog.component';
import { SimpleEditInviteComponent } from './simple-edit-invite/simple-edit-invite.component';
import { SimpleEditLicenseComponent } from './simple-edit-license/simple-edit-license.component';
import { SimpleEditMetadataComponent } from './simple-edit-metadata/simple-edit-metadata.component';
import { UserTileComponent } from './user-tile/user-tile.component';
import { HasPermissionPipe } from './has-permission.pipe';

export { SimpleEditDialogComponent };

@NgModule({
    declarations: [
        SimpleEditDialogComponent,
        SimpleEditInviteComponent,
        SimpleEditLicenseComponent,
        SimpleEditMetadataComponent,
        UserTileComponent,
        HasPermissionPipe,
    ],
    imports: [SharedModule, MdsModule],
})
export class SimpleEditDialogModule {}
