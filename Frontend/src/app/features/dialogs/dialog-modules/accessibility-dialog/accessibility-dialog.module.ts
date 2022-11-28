import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { AccessibilityDialogComponent } from './accessibility-dialog.component';

export { AccessibilityDialogComponent };

@NgModule({
    declarations: [AccessibilityDialogComponent],
    imports: [SharedModule],
})
export class AccessibilityDialogModule {}
