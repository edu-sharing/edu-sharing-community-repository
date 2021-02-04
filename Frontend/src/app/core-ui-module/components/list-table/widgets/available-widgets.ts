import {ListCollectionInfoComponent} from './list-collection-info/list-collection-info.component';
import {ListWidgetClass} from './list-widget';
import {ListTextComponent} from './list-text/list-text.component';
import {NativeWidgetType} from '../../../../common/ui/mds-editor/types';
import {ListNodeLicenseComponent} from './list-node-license/list-node-license.component';
import {ListNodeWorkflowComponent} from './list-node-workflow/list-node-workflow.component';
import {ListCountsComponent} from './list-counts/list-counts.component';

export enum ListWidgetType {
    CollectionInfo = 'CollectionInfo',
    Text = 'Text',
    NodeLicense = 'NodeLicense',
    NodeWorkflow = 'NodeWorkflow',
    NodeCounts = 'NodeCounts',
}
export const AVAILABLE_LIST_WIDGETS: {
    [widgetType in ListWidgetType]: ListWidgetClass
} = {
    CollectionInfo: ListCollectionInfoComponent,
    NodeLicense: ListNodeLicenseComponent,
    NodeWorkflow: ListNodeWorkflowComponent,
    NodeCounts: ListCountsComponent,

    // use the widgets with wildcards as last ones
    Text: ListTextComponent,
};
