import { Node } from 'ngx-edu-sharing-api';
import { DroppedFolder } from '../../../../shared/directives/file-drop';

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
     * Is this dialog for selecting a childobject (i.e. in context of creating a series object)
     */
    childobject: boolean;
    /**
     * Allow the user to upload multiple files.
     */
    multiple: boolean;
    /**
     * Show the LTI option and support generation of LTI files
     */
    showLti: boolean;
    /**
     * Show the list of uploaded files and require confirmation (default false)
     */
    showFiles?: boolean;
    /**
     * Files and folders to preselect (e.g. dropped somewhere on the page). They are shown to the
     * user, who can pick the storage location and has to confirm the upload.
     */
    preselection?: DroppedFolder;
}

export interface FileData {
    files: File[];
    parent: Node;
    /**
     * Folders to recreate (including their contents) below `parent`, in addition to `files`
     */
    folders?: DroppedFolder[];
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
