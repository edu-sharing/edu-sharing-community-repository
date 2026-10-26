import { PageStructure } from './page-structure';

/**
 * One reversible write of the topic page editor: either the whole page structure of the variant
 * or a single property of a node. Undo writes `before`, redo writes `after`.
 */
export type HistoryOperation = StructureOperation | NodePropertyOperation;

export interface StructureOperation {
    kind: 'structure';
    before: PageStructure;
    after: PageStructure;
}

export interface NodePropertyOperation {
    kind: 'nodeProperty';
    nodeId: string;
    property: string;
    // null stands for a property the node does not carry
    before: string[] | null;
    after: string[] | null;
}
