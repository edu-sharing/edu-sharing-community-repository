import { ListCollectionInfoComponent } from './list-collection-info/list-collection-info.component';
import { ListCountsComponent } from './list-counts/list-counts.component';
import { ListNodeLicenseComponent } from './list-node-license/list-node-license.component';
import { ListNodeReplicationSourceComponent } from './list-node-replication-source/list-node-replication-source.component';
import { ListNodeWorkflowComponent } from './list-node-workflow/list-node-workflow.component';
import { ListTextComponent } from './list-text/list-text.component';
import { ListWidgetClass } from './list-widget';

export enum ListWidgetType {
    CollectionInfo = 'CollectionInfo',
    Text = 'Text',
    Custom = 'Custom',
    NodeLicense = 'NodeLicense',
    NodeReplicationSource = 'NodeReplicationSource',
    NodeWorkflow = 'NodeWorkflow',
    NodeCounts = 'NodeCounts',
}
export const AVAILABLE_LIST_WIDGETS: {
    [widgetType in ListWidgetType]: ListWidgetClass;
} = {
    CollectionInfo: ListCollectionInfoComponent,
    NodeLicense: ListNodeLicenseComponent,
    NodeReplicationSource: ListNodeReplicationSourceComponent,
    NodeWorkflow: ListNodeWorkflowComponent,
    NodeCounts: ListCountsComponent,
    Custom: null,

    // use the widgets with wildcards as last ones
    Text: ListTextComponent,
};
