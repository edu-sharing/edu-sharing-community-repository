import { Node } from 'ngx-edu-sharing-api';

export interface ContributorsDialogData {
    /**
     * Nodes to edit, as objects or ids. More than one node puts the dialog into bulk mode, where
     * contributors that exist only on some of the nodes are shown as indeterminate and are left
     * untouched unless the user acts on them.
     */
    nodes: (Node | string)[];
}

export type ContributorsDialogResult = Node[] | null;
