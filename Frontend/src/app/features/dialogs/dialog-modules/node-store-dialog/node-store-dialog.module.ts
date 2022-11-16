import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeEntriesModule } from '../../../node-entries/node-entries.module';
import { SearchNodeStoreComponent } from './node-store.component';

export { SearchNodeStoreComponent };

@NgModule({
    declarations: [SearchNodeStoreComponent],
    imports: [SharedModule, NodeEntriesModule],
})
export class NodeStoreDialogModule {}
