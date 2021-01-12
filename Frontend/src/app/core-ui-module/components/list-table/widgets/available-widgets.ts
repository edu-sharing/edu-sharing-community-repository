import {ListCollectionInfoComponent} from './list-collection-info/list-collection-info.component';
import {ListWidgetClass} from './list-widget';
import {ListTextComponent} from './list-text/list-text.component';
import {NativeWidgetType} from '../../../../common/ui/mds-editor/types';
import {ListNodeLicenseComponent} from './list-node-license/list-node-license.component';
import {ListNodeWorkflowComponent} from './list-node-workflow/list-node-workflow.component';

export enum ListWidgetType{
    CollectionInfo = 'CollectionInfo',
    Text = 'Text',
    NodeLicense = 'NodeLicense',
    NodeWorkflow = 'NodeWorkflow',
}
export const AVAILABLE_LIST_WIDGETS: {
    [widgetType in ListWidgetType]: ListWidgetClass
} = {
    CollectionInfo: ListCollectionInfoComponent,
    NodeLicense: ListNodeLicenseComponent,
    NodeWorkflow: ListNodeWorkflowComponent,

    // use the widgets with wildcars as last ones
    Text: ListTextComponent,
};