import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { GenericDialogComponent } from './generic-dialog.component';
import { SplitNewLinesPipe } from './split-new-lines.pipe';

export { GenericDialogComponent };

@NgModule({
    declarations: [GenericDialogComponent, SplitNewLinesPipe],
    imports: [SharedModule],
})
export class GenericDialogModule {}
