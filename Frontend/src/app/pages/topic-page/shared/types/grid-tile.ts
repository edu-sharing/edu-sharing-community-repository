import { WIDGET_TYPE } from './custom-definitions';

export class GridTile {
    item?: WIDGET_TYPE | '';
    cols: number;
    rows: number;
    nodeId?: string;
    propagatedNodeId?: string;
    hasHits?: boolean;
    searchCount?: number;

    constructor(cols: number, rows: number) {
        this.cols = cols;
        this.rows = rows;
    }
}
