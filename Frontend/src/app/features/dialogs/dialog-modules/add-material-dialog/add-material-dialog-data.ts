import { Node } from 'ngx-edu-sharing-api';

export class AddMaterialDialogData {
    /**
     * The storage location node under which to save the newly added material.
     */
    parent: Node | null;
    /**
     * Show a breadcrumbs bar of the storage location and allow the user to change it.
     */
    chooseParent: boolean;
    /**
     * Allow the user to upload multiple files.
     */
    multiple: boolean;
    /**
     * Show the LTI option and support generation of LTI files
     */
    showLti: boolean;
}

export interface FileData {
    files: FileList;
    parent: Node;
}

export interface LinkData {
    link: string;
    parent: Node;
    lti?: {
        consumerKey: string;
        sharedSecret: string;
    };
}

interface FileResult extends FileData {
    kind: 'file';
}

interface LinkResult extends LinkData {
    kind: 'link';
}

export type AddMaterialDialogResult = FileResult | LinkResult | null;
