import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { AltchaWidgetComponent } from './altcha-widget.component';
import { NodeReportDialogComponent } from './node-report-dialog.component';

export { NodeReportDialogComponent };

@NgModule({
    declarations: [NodeReportDialogComponent],
    imports: [SharedModule, AltchaWidgetComponent],
})
export class NodeReportDialogModule {}
