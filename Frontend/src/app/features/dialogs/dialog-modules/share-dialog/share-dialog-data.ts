import { LocalPermissions, Node } from '../../../../core-module/core.module';

export class ShareDialogData {
    /** Provide either a node objects or the node ids. */
    nodes: Node[] | string[];
    sendMessages? = true;
    sendToApi? = true;
    currentPermissions?: LocalPermissions = null;
}

export interface ShareDialogResult {
    permissions: LocalPermissions;
    notify: boolean;
    notifyMessage: string;
}
