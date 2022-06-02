import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { FormatDurationPipe } from './format-duration.pipe';
import { ListBaseComponent } from './list-base/list-base.component';
import { ListCollectionInfoComponent } from './list-collection-info/list-collection-info.component';
import { ListCountsComponent } from './list-counts/list-counts.component';
import { ListNodeLicenseComponent } from './list-node-license/list-node-license.component';
import { ListNodeReplicationSourceComponent } from './list-node-replication-source/list-node-replication-source.component';
import { ListNodeWorkflowComponent } from './list-node-workflow/list-node-workflow.component';
import { ListTextComponent } from './list-text/list-text.component';
import { NodeSourcePipe } from './node-source.pipe';

@NgModule({
    declarations: [
        ListBaseComponent,
        ListCollectionInfoComponent,
        ListNodeLicenseComponent,
        ListNodeReplicationSourceComponent,
        ListNodeWorkflowComponent,
        ListTextComponent,
        ListCountsComponent,
        NodeSourcePipe,
        FormatDurationPipe,
    ],
    imports: [SharedModule],
    exports: [ListBaseComponent, ListTextComponent],
})
export class ListItemsModule {}
