import { Node } from '../../../../core-module/core.module';

export interface NodeRelationsDialogData {
    node: Node;
}

/** `true` when relations were changed. */
export type NodeRelationsDialogResult = true | null;
