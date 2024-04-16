import { NgModule } from '@angular/core';
import { FormatDurationPipe } from './format-duration.pipe';
import { ListBaseComponent } from './list-base/list-base.component';
import { ListCollectionInfoComponent } from './list-collection-info/list-collection-info.component';
import { ListCountsComponent } from './list-counts/list-counts.component';
import { ListNodeLicenseComponent } from './list-node-license/list-node-license.component';
import { ListNodeReplicationSourceComponent } from './list-node-replication-source/list-node-replication-source.component';
import { ListNodeWorkflowComponent } from './list-node-workflow/list-node-workflow.component';
import { ListTextComponent } from './list-text/list-text.component';
import { NodeRowComponent } from './node-row/node-row.component';
import { NodeSourcePipe } from './node-source.pipe';
import { CommonModule } from '@angular/common';
import { EduSharingUiCommonModule } from '../common/edu-sharing-ui-common.module';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

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
        NodeRowComponent,
    ],
    imports: [CommonModule, EduSharingUiCommonModule, MatTooltipModule, TranslateModule],
    exports: [ListBaseComponent, ListTextComponent, FormatDurationPipe, NodeRowComponent],
})
export class ListItemsModule {}
