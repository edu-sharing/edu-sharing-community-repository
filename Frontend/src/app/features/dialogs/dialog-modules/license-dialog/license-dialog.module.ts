import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { LicenseDialogComponent } from './license-dialog.component';
import { LicenseDialogContentComponent } from './license-dialog-content.component';
import { MdsModule } from '../../../mds/mds.module';

export { LicenseDialogComponent };

@NgModule({
    declarations: [LicenseDialogComponent, LicenseDialogContentComponent],
    imports: [SharedModule, MdsModule],
    exports: [LicenseDialogContentComponent],
})
export class LicenseDialogModule {}
