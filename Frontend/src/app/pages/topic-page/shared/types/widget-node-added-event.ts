import { Node } from 'ngx-edu-sharing-api';
import { WidgetConfigObject } from './widget-config-object';

export interface WidgetNodeAddedEvent {
    gridIndex: number;
    isBreadcrumbNode: boolean;
    isHeaderNode: boolean;
    pageVariantNode: Node;
    swimlaneIndex: number;
    widget: WidgetConfigObject;
}
