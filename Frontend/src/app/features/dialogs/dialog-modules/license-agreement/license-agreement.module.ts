import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { LicenseAgreementComponent } from './license-agreement.component';

export { LicenseAgreementComponent };

@NgModule({
    declarations: [LicenseAgreementComponent],
    imports: [SharedModule],
})
export class LicenseAgreementModule {}
