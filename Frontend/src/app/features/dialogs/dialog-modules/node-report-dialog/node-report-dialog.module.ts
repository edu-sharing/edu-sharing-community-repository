import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeReportDialogComponent } from './node-report-dialog.component';

export { NodeReportDialogComponent };

@NgModule({
    declarations: [NodeReportDialogComponent],
    imports: [SharedModule],
})
export class NodeReportDialogModule {}
