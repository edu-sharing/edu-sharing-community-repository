import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { LicenseAgreementDialogComponent } from './license-agreement-dialog.component';

export { LicenseAgreementDialogComponent };

@NgModule({
    declarations: [LicenseAgreementDialogComponent],
    imports: [SharedModule],
})
export class LicenseAgreementDialogModule {}
