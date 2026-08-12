import { Node, Tool } from 'ngx-edu-sharing-api';

export class CreateLtiToolDialogData {
    /** the LTI tool the element should be created with */
    tool: Tool;
    /** the folder the element is created in (used for the deep-link flow) */
    parent: Node;
}

export interface CreateLtiToolDialogResult {
    /** nodes the deep-link flow has already created (empty for `customContentOption` tools) */
    nodes: Node[];
    name: string;
    /**
     * Popup window opened by the dialog for `customContentOption` tools. It has to be opened
     * within the user gesture, otherwise the popup blocker kills it.
     */
    window?: Window;
}
