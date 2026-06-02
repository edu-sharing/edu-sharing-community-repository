import { WIDGET_TYPE } from './custom-definitions';

export class GridTile {
    item?: WIDGET_TYPE | '';
    cols: number;
    rows: number;
    nodeId?: string;
    // render-only marker (never saved): renders this tile read-only from an inherited node
    propagatedNodeId?: string;
    // copy-source pointer (never saved): node to copy into a new variant, deleted after copying
    temporaryNodeId?: string;
    hasHits?: boolean;
    searchCount?: number;

    constructor(cols: number, rows: number) {
        this.cols = cols;
        this.rows = rows;
    }
}
