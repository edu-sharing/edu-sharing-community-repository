import { Node } from 'ngx-edu-sharing-api';

export interface NodeReportDialogData {
    node: Node;
    mode: 'NODE_REPORT' | 'REVOKE_FEEDBACK';
    showOptions: boolean;
}

export abstract class DialogsService {
    async openNodeReportDialog(data: NodeReportDialogData): Promise<any> {}
}
