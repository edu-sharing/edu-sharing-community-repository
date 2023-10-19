import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { CreateVariantDialogComponent } from './create-variant-dialog.component';

export { CreateVariantDialogComponent };

@NgModule({
    declarations: [CreateVariantDialogComponent],
    imports: [SharedModule],
})
export class CreateVariantDialogModule {}
