import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ThirdPartyLicensesDialogComponent } from './third-party-licenses-dialog.component';

export { ThirdPartyLicensesDialogComponent };

@NgModule({
    declarations: [ThirdPartyLicensesDialogComponent],
    imports: [SharedModule],
})
export class ThirdPartyLicensesDialogModule {}
