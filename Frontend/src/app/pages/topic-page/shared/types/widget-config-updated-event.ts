import { Node } from 'ngx-edu-sharing-api';

export interface WidgetConfigUpdatedEvent {
    // the page variant node owning the widget node
    pageVariantNode: Node;
    widgetNodeId: string;
    // the written properties with their values before and after the write (null if absent)
    changes: { property: string; before: string[] | null; after: string[] | null }[];
}
