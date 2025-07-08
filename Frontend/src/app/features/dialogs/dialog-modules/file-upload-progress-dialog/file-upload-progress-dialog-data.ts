import { Node } from '../../../../core-module/core.module';

export class FileUploadProgressDialogData {
    parent: Node;
    files: FileList;
}

export type FileUploadProgressDialogResult = {
    status: 'FINISHED' | 'CANCELED';
    nodes: Node[] | null;
};
