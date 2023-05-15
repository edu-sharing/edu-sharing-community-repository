import { Node } from '../../../../core-module/core.module';

export interface ContributorsDialogData {
    /** Provide either a node object or the node id. */
    node: Node | string;
}

export type ContributorsDialogResult = Node | null;
