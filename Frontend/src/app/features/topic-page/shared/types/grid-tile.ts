import { WIDGET_TYPE } from './custom-definitions';
import { WidgetConfig } from './widget-config/widget-config';

export class GridTile {
    item?: WIDGET_TYPE | '';
    cols: number;
    rows: number;
    nodeId?: string;
    // render-only marker (never saved): renders this tile read-only from an inherited node
    propagatedNodeId?: string;
    // copy-source pointer (never saved): node to copy into a new variant, deleted after copying
    temporaryNodeId?: string;
    // page templates only: values merged over the copied widget's config, so a repeated swimlane
    // binds each of its copies to its own item
    configPatch?: Partial<WidgetConfig>;
    // render-only marker (never saved): config a previewed tile renders from, having no node yet
    configOverwrite?: string;
    hasHits?: boolean;
    searchCount?: number;

    constructor(cols: number, rows: number) {
        this.cols = cols;
        this.rows = rows;
    }
}
