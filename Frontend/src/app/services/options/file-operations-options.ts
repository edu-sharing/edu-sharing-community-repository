import {
    Constrain,
    DefaultGroups,
    ElementType,
    HideMode,
    OptionItem,
    Scope,
} from 'ngx-edu-sharing-ui';
import { Assignment } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { ToastType } from '../toast';
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
    // collections and their references are moved into another collection
    cutNodes.scopes = [
        Scope.WorkspaceList,
        Scope.WorkspaceTree,
        Scope.CollectionsCollection,
        Scope.CollectionsReferences,
    ];
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
    // map links and collections each require their own creation toolpermission
    copyNodes.customEnabledCallback = async (node) =>
        (node?.some((n) => n.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION))
            ? service.connector.hasToolPermissionInstant(
                  RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
              )
            : true) &&
        (node?.some((n) => service.getTypeSingle(n) === ElementType.MapRef)
            ? service.connector.hasToolPermissionInstant(
                  RestConstants.TOOLPERMISSION_CREATE_MAP_LINK,
              )
            : true);
    copyNodes.elementType = [ElementType.Node, ElementType.SavedSearch, ElementType.MapRef];
    copyNodes.constrains = [Constrain.HomeRepository, Constrain.User];
    // collections and their references are copied into another collection
    copyNodes.scopes = [
        Scope.WorkspaceList,
        Scope.WorkspaceTree,
        Scope.CollectionsCollection,
        Scope.CollectionsReferences,
    ];
    copyNodes.keyboardShortcut = {
        keyCode: 'KeyC',
        modifiers: ['Ctrl/Cmd'],
    };
    copyNodes.group = DefaultGroups.FileOperations;
    copyNodes.priority = 20;

    // pastes the clipboard into the opened collection; offered in the bottom bar of the
    // references list, which stays visible as long as the clipboard has content
    const pasteIntoCollection = new OptionItem('OPTIONS.PASTE', 'content_paste', () =>
        service.pasteNode(components, { parent: data.parent, scope: null }, false),
    );
    pasteIntoCollection.elementType = [ElementType.NoneOrUnknown];
    pasteIntoCollection.constrains = [
        Constrain.NoSelection,
        Constrain.ClipboardContent,
        Constrain.User,
    ];
    pasteIntoCollection.customShowCallback = async () =>
        service.canPasteIntoCollection(data?.parent);
    // CollectionsCollection: the sub-collections list of the start page, which has no references
    pasteIntoCollection.scopes = [Scope.CollectionsReferences, Scope.CollectionsCollection];
    pasteIntoCollection.keyboardShortcut = {
        keyCode: 'KeyV',
        modifiers: ['Ctrl/Cmd'],
    };
    pasteIntoCollection.group = DefaultGroups.FileOperations;

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
    // collections and collection references can only be pasted into another collection
    pasteNodes.customShowCallback = async () => service.canPasteIntoFolder(data?.parent);
    pasteNodes.scopes = [Scope.WorkspaceList];
    pasteNodes.keyboardShortcut = {
        keyCode: 'KeyV',
        modifiers: ['Ctrl/Cmd'],
    };
    pasteNodes.group = DefaultGroups.FileOperations;

    const copyAssignment = new OptionItem('OPTIONS.ASSIGNMENT_COPY', 'content_copy', (object) => {
        const assignment = service.getObjects(object, data)[0] as Assignment;
        service.toast.showProgressSpinner();
        service.assignmentV1Service.copyAssignment({ assignmentId: assignment.ref.id }).subscribe({
            next: (copy) => {
                service.toast.show({
                    type: 'info',
                    subtype: ToastType.InfoSimple,
                    message: 'TOAST.ASSIGNMENT_COPY',
                });
                service.toast.closeProgressSpinner();
                service.uiService.goToAssignment(copy, 'edit');
            },
            error: (error) => {
                service.toast.closeProgressSpinner();
                service.toast.error(error);
            },
        });
    });
    copyAssignment.elementType = [ElementType.Assignment];
    copyAssignment.constrains = [Constrain.NoBulk, Constrain.User];
    copyAssignment.customShowCallback = async (objects) => {
        const assignment = objects[0] as Assignment;
        // only the owner/creator of the assignment may copy it
        return (
            assignment.type === 'SUBMISSION' &&
            !!assignment.creator?.authorityName &&
            assignment.creator.authorityName === service.connector.getCurrentLogin()?.authorityName
        );
    };
    copyAssignment.group = DefaultGroups.FileOperations;
    copyAssignment.priority = 30;

    return [linkMap, cutNodes, copyNodes, pasteNodes, pasteIntoCollection, copyAssignment];
}
