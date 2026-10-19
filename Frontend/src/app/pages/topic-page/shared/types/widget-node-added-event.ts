import { Node } from 'ngx-edu-sharing-api';
import { WidgetConfigObject } from './widget-config-object';

export interface WidgetNodeAddedEvent {
    gridIndex: number;
    isBreadcrumbNode: boolean;
    isHeaderNode: boolean;
    pageVariantNode: Node;
    swimlaneIndex: number;
    widget: WidgetConfigObject;
    // called once the event is handled, with the node now holding the config (null if none)
    nodeAvailable?: (widgetNodeId: string | null) => void;
}
