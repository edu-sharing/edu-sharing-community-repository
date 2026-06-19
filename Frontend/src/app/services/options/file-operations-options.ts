import {
    Constrain,
    DefaultGroups,
    ElementType,
    HideMode,
    OptionItem,
    Scope,
} from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { OptionsContext } from './options-context';

export function createFileOperationsOptions({
    service,
    management,
    components,
    data,
}: OptionsContext): OptionItem[] {
    const linkMap = new OptionItem('OPTIONS.LINK_MAP', 'link', (node) =>
        service.dialogs.openCreateMapLinkDialog({ node: service.getObjects(node, data)[0] }),
    );
    linkMap.constrains = [
        Constrain.NoBulk,
        Constrain.HomeRepository,
        Constrain.User,
        Constrain.Directory,
    ];
    linkMap.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_MAP_LINK];
    linkMap.toolpermissionsMode = HideMode.Hide;
    linkMap.scopes = [Scope.WorkspaceList, Scope.WorkspaceTree];
    linkMap.permissionsMode = HideMode.Hide;
    linkMap.group = DefaultGroups.FileOperations;
    linkMap.priority = 5;

    const cutNodes = new OptionItem('OPTIONS.CUT', 'content_cut', (node) =>
        service.cutCopyNode(data, node, false),
    );
    cutNodes.elementType = [ElementType.Node, ElementType.SavedSearch, ElementType.MapRef];
    cutNodes.constrains = [Constrain.HomeRepository, Constrain.User];
    cutNodes.scopes = [Scope.WorkspaceList, Scope.WorkspaceTree];
    cutNodes.permissions = [RestConstants.ACCESS_DELETE];
    cutNodes.permissionsMode = HideMode.Disable;
    cutNodes.keyboardShortcut = {
        keyCode: 'KeyX',
        modifiers: ['Ctrl/Cmd'],
    };
    cutNodes.group = DefaultGroups.FileOperations;
    cutNodes.priority = 10;

    const copyNodes = new OptionItem('OPTIONS.COPY', 'content_copy', (node) =>
        service.cutCopyNode(data, node, true),
    );
    // do not allow copy of map links if tp is missing
    copyNodes.customEnabledCallback = async (node) =>
        node.every((n) => !n.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION)) &&
        (node?.some((n) => service.getTypeSingle(n) === ElementType.MapRef)
            ? service.connector.hasToolPermissionInstant(
                  RestConstants.TOOLPERMISSION_CREATE_MAP_LINK,
              )
            : true);
    copyNodes.elementType = [ElementType.Node, ElementType.SavedSearch, ElementType.MapRef];
    copyNodes.constrains = [Constrain.HomeRepository, Constrain.User];
    copyNodes.scopes = [Scope.WorkspaceList, Scope.WorkspaceTree];
    copyNodes.keyboardShortcut = {
        keyCode: 'KeyC',
        modifiers: ['Ctrl/Cmd'],
    };
    copyNodes.group = DefaultGroups.FileOperations;
    copyNodes.priority = 20;

    const pasteNodes = new OptionItem('OPTIONS.PASTE', 'content_paste', (node) =>
        service.pasteNode(components, data),
    );
    pasteNodes.elementType = [ElementType.NoneOrUnknown];
    pasteNodes.constrains = [
        Constrain.NoSelection,
        Constrain.ClipboardContent,
        Constrain.AddObjects,
        Constrain.User,
    ];
    pasteNodes.toolpermissions = [
        RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS,
        RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
    ];
    pasteNodes.scopes = [Scope.WorkspaceList];
    pasteNodes.keyboardShortcut = {
        keyCode: 'KeyV',
        modifiers: ['Ctrl/Cmd'],
    };
    pasteNodes.group = DefaultGroups.FileOperations;

    return [linkMap, cutNodes, copyNodes, pasteNodes];
}
