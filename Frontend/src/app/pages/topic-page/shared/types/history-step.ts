import { HistoryOperation } from './history-operation';

/**
 * A single user-facing step of the topic page editor history, undone and redone as a whole.
 */
export interface HistoryStep {
    id: number;
    // i18n key naming the step
    labelKey: string;
    // i18n keys filled into the label, e.g. the name of the changed widget
    labelParams?: { [param: string]: string };
    // the page variant node the operations were applied to
    variantNodeId: string;
    // applied in order on redo, in reverse order on undo
    operations: HistoryOperation[];
    // consecutive steps sharing this key within a short time are merged into one
    coalesceKey?: string;
    // time of the latest change contained in the step
    at: number;
}
