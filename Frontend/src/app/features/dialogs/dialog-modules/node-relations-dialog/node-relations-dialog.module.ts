import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ListItemsModule } from '../../../list-items/list-items.module';
import { MdsModule } from '../../../mds/mds.module';
import { NodeRelationsDialogComponent } from './node-relations-dialog.component';
import { NodeSearchSelectorComponent } from './node-search-selector/node-search-selector.component';

export { NodeRelationsDialogComponent };

@NgModule({
    declarations: [NodeRelationsDialogComponent, NodeSearchSelectorComponent],
    imports: [SharedModule, ListItemsModule, MdsModule],
})
export class NodeRelationsDialogModule {}
