import { Node } from '../../../../core-module/core.module';

export interface CreateMapLinkDialogData {
    node: Node;
}

export type CreateMapLinkDialogResult = Node | null;
