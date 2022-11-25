import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { NodeEntriesModule } from '../../../node-entries/node-entries.module';
import { SearchNodeStoreDialogComponent } from './node-store-dialog.component';

export { SearchNodeStoreDialogComponent as SearchNodeStoreComponent };

@NgModule({
    declarations: [SearchNodeStoreDialogComponent],
    imports: [SharedModule, NodeEntriesModule],
})
export class NodeStoreDialogModule {}
