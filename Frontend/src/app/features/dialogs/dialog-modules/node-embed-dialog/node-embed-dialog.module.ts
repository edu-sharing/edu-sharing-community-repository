import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeEmbedDialogComponent } from './node-embed-dialog.component';

export { NodeEmbedDialogComponent };

@NgModule({
    declarations: [NodeEmbedDialogComponent],
    imports: [SharedModule],
})
export class NodeEmbedDialogModule {}
