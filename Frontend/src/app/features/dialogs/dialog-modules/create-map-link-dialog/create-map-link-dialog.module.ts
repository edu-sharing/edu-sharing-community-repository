import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { CreateMapLinkDialogComponent } from './create-map-link-dialog.component';

export { CreateMapLinkDialogComponent };

@NgModule({
    declarations: [CreateMapLinkDialogComponent],
    imports: [SharedModule],
})
export class CreateMapLinkDialogModule {}
