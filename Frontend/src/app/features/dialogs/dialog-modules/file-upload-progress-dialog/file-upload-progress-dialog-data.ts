import { Node } from 'ngx-edu-sharing-api';

export class FileUploadProgressDialogData {
    parent: Node;
    files: File[];
}

export type FileUploadProgressDialogResult = {
    status: 'FINISHED' | 'CANCELED';
    nodes: Node[] | null;
};
