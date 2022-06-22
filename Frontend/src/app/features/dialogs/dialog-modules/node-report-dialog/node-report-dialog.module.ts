import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeReportComponent } from './node-report/node-report.component';

export { NodeReportComponent };

@NgModule({
    declarations: [NodeReportComponent],
    imports: [SharedModule],
})
export class NodeReportDialogModule {}
