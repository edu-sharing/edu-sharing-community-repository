import { Node } from 'ngx-edu-sharing-api';

export class FileUploadProgressDialogData {
    parent: Node;
    /**
     * behaviour for duplicate nodes
     * if unset, the default is 'ask-user'
     */
    duplicateBehaviour?: 'ask-user' | 'unique' | 'replace';
    files: File[];
    /**
     * Optional target folder per file (same index as `files`), e.g. when uploading a folder tree.
     * Falls back to `parent` for files without an entry.
     */
    parents?: Node[];
    /**
     * Optional folder path per file (same index as `files`), shown next to the file name, e.g.
     * `Folder/Subfolder` when uploading a folder tree.
     */
    paths?: string[];
}

export type FileUploadProgressDialogResult = {
    status: 'FINISHED' | 'CANCELED';
    nodes: Node[] | null;
};
