import { Node } from 'ngx-edu-sharing-api';

export class FileUploadProgressDialogData {
    parent: Node;
    /**
     * behaviour for duplicate nodes
     * if unset, the default is 'ask-user'
     */
    duplicateBehaviour?: 'ask-user' | 'unique' | 'replace';
    files: File[];
}

export type FileUploadProgressDialogResult = {
    status: 'FINISHED' | 'CANCELED';
    nodes: Node[] | null;
};
