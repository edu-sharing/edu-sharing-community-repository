import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeInfoComponent } from './node-info/node-info.component';

export { NodeInfoComponent };

@NgModule({
    declarations: [NodeInfoComponent],
    imports: [SharedModule],
})
export class NodeInfoDialogModule {}
