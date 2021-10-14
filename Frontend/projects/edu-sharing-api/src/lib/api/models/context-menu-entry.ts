/* tslint:disable */
/* eslint-disable */
export interface ContextMenuEntry {
    ajax?: boolean;
    changeStrategy?: 'update' | 'remove';
    group?: string;
    icon?: string;
    isDirectory?: boolean;
    isDisabled?: boolean;
    isSeparate?: boolean;
    isSeparateBottom?: boolean;
    mode?: string;
    multiple?: boolean;
    name?: string;
    onlyDesktop?: boolean;
    onlyWeb?: boolean;
    openInNew?: boolean;
    permission?: string;
    position?: number;
    scopes?: Array<
        | 'Render'
        | 'Search'
        | 'CollectionsReferences'
        | 'CollectionsCollection'
        | 'WorkspaceList'
        | 'WorkspaceTree'
        | 'Oer'
        | 'CreateMenu'
    >;
    showAsAction?: boolean;
    toolpermission?: string;
    url?: string;
}
