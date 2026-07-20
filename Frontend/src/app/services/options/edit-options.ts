import {
    AssignmentPipe,
    Constrain,
    DefaultGroups,
    ElementType,
    HideMode,
    NodesRightMode,
    OptionItem,
    Scope,
} from 'ngx-edu-sharing-ui';
import { Assignment } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { OK_OR_CANCEL } from '../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { ToastType } from '../toast';
import { OptionsContext } from './options-context';
import { first } from 'rxjs/operators';

export function createEditOptions({
    service,
    management,
    components,
    data,
}: OptionsContext): OptionItem[] {
    const editRevocation = new OptionItem('OPTIONS.EDIT_REVOCATION', 'edit', async (object) => {
        await service.revokeNode(object, data);
    });
    editRevocation.constrains = [
        Constrain.Files,
        Constrain.NoBulk,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    editRevocation.scopes = [Scope.Render];
    editRevocation.elementType = [ElementType.NodeRevoked];
    editRevocation.group = DefaultGroups.Edit;
    editRevocation.priority = 10;
    editRevocation.permissions = [RestConstants.ACCESS_WRITE];
    editRevocation.permissionsRightMode = NodesRightMode.Effective;
    editRevocation.permissionsMode = HideMode.Hide;

    const inviteNode = new OptionItem('OPTIONS.INVITE', 'group_add', async (object) =>
        service.dialogs.openShareDialog({
            nodes: await service.getObjectsAsync(object, data, true),
        }),
    );
    inviteNode.elementType = [ElementType.Node, ElementType.SavedSearch];
    inviteNode.showAsAction = true;
    inviteNode.permissions = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
    inviteNode.permissionsMode = HideMode.Hide;
    inviteNode.permissionsRightMode = NodesRightMode.Effective;
    // inviteNode.key = 'S';
    inviteNode.constrains = [Constrain.HomeRepository, Constrain.User];
    inviteNode.toolpermissions = [RestConstants.TOOLPERMISSION_INVITE];
    inviteNode.group = DefaultGroups.Edit;
    inviteNode.priority = 10;
    // invite is not allowed for collections of type editorial
    inviteNode.customShowCallback = async (objects) =>
        objects[0].collection
            ? objects[0].collection.type !== RestConstants.COLLECTIONTYPE_EDITORIAL
            : objects[0].type !== RestConstants.SYS_TYPE_CONTAINER;

    const streamNode = new OptionItem(
        'OPTIONS.STREAM',
        'event',
        (object) => (management.addNodesStream = service.getObjects(object, data)),
    );
    streamNode.elementType = [ElementType.Node];
    streamNode.permissions = [RestConstants.ACCESS_CC_PUBLISH];
    streamNode.permissionsMode = HideMode.Hide;
    streamNode.constrains = [
        Constrain.Files,
        Constrain.NoCollectionReference,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    streamNode.toolpermissions = [RestConstants.TOOLPERMISSION_INVITE_STREAM];
    streamNode.group = DefaultGroups.Edit;
    streamNode.priority = 15;
    streamNode.customShowCallback = (objects) =>
        service.configService.get('stream.enabled', false).pipe(first()).toPromise();

    const licenseNode = new OptionItem('OPTIONS.LICENSE', 'copyright', (object) => {
        const nodes = service.getObjects(object, data);
        void service.dialogs.openLicenseDialog({ kind: 'nodes', nodes });
    });
    licenseNode.elementType = [ElementType.Node, ElementType.NodeChild];
    licenseNode.constrains = [
        Constrain.Files,
        Constrain.NoCollectionReference,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    licenseNode.permissions = [RestConstants.ACCESS_WRITE];
    licenseNode.permissionsMode = HideMode.Disable;
    licenseNode.toolpermissions = [RestConstants.TOOLPERMISSION_LICENSE];
    // licenseNode.key = 'L';
    licenseNode.group = DefaultGroups.Edit;
    licenseNode.priority = 30;

    const contributorNode = new OptionItem('OPTIONS.CONTRIBUTOR', 'group', (object) => {
        void service.dialogs.openContributorsDialog({
            node: service.getObjects(object, data)[0],
        });
    });
    contributorNode.constrains = [
        Constrain.Files,
        Constrain.NoCollectionReference,
        Constrain.HomeRepository,
        Constrain.NoBulk,
        Constrain.User,
    ];
    contributorNode.permissions = [RestConstants.ACCESS_WRITE];
    contributorNode.permissionsMode = HideMode.Disable;
    contributorNode.onlyDesktop = true;
    contributorNode.group = DefaultGroups.Edit;
    contributorNode.priority = 40;

    const workflowNode = new OptionItem('OPTIONS.WORKFLOW', 'swap_calls', (object) =>
        service.dialogs.openWorkflowDialog({ nodes: service.getObjects(object, data) }),
    );
    workflowNode.constrains = [
        Constrain.Files,
        Constrain.NoCollectionReference,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    workflowNode.permissions = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
    workflowNode.permissionsMode = HideMode.Disable;
    workflowNode.group = DefaultGroups.Edit;
    workflowNode.priority = 50;

    const simpleEditNode = new OptionItem(
        'OPTIONS.EDIT_SIMPLE',
        'edu-quick_edit',
        async (object) => {
            const nodes = await service.getObjectsAsync(object, data, true);
            void service.dialogs.openSimpleEditDialog({ nodes, fromUpload: false });
        },
    );
    simpleEditNode.constrains = [Constrain.Files, Constrain.HomeRepository, Constrain.User];
    simpleEditNode.permissions = [RestConstants.ACCESS_WRITE];
    simpleEditNode.permissionsRightMode = NodesRightMode.Effective;
    simpleEditNode.permissionsMode = HideMode.Disable;
    simpleEditNode.group = DefaultGroups.Edit;
    simpleEditNode.priority = 15;

    const editNode = new OptionItem('OPTIONS.EDIT', 'edit', async (object) => {
        const nodes = await service.getObjectsAsync(object, data, true);
        void service.dialogs.openMdsEditorDialogForNodes({ nodes });
    });
    editNode.elementType = [ElementType.Node, ElementType.NodeChild, ElementType.MapRef];
    editNode.constrains = [Constrain.FilesAndDirectories, Constrain.HomeRepository, Constrain.User];
    editNode.permissions = [RestConstants.ACCESS_WRITE];
    editNode.permissionsMode = HideMode.Disable;
    editNode.permissionsRightMode = NodesRightMode.Effective;
    editNode.group = DefaultGroups.Edit;
    editNode.priority = 20;

    const templateNode = new OptionItem('OPTIONS.TEMPLATE', 'assignment_turned_in', (object) =>
        service.dialogs.openNodeTemplateDialog({ node: service.getObjects(object, data)[0] }),
    );
    templateNode.constrains = [Constrain.NoBulk, Constrain.Directory, Constrain.User];
    templateNode.permissions = [RestConstants.ACCESS_WRITE];
    templateNode.permissionsMode = HideMode.Disable;
    templateNode.onlyDesktop = true;
    templateNode.group = DefaultGroups.Edit;

    const editCollection = new OptionItem('OPTIONS.COLLECTION_EDIT', 'edit', (object) =>
        service.uiService.goToCollection(service.getObjects(object, data)[0], 'edit'),
    );
    editCollection.constrains = [
        Constrain.HomeRepository,
        Constrain.Collections,
        Constrain.NoBulk,
        Constrain.User,
    ];
    editCollection.permissions = [RestConstants.ACCESS_WRITE];
    editCollection.permissionsMode = HideMode.Hide;
    editCollection.showAsAction = true;
    editCollection.group = DefaultGroups.Edit;
    editCollection.priority = 5;

    const pinCollection = new OptionItem('OPTIONS.COLLECTION_PIN', 'edu-pin', (object) =>
        service.dialogs.openPinnedCollectionsDialog({
            collection: service.getObjects(object, data)[0],
        }),
    );
    pinCollection.constrains = [
        Constrain.HomeRepository,
        Constrain.Collections,
        Constrain.NoBulk,
        Constrain.User,
    ];
    pinCollection.permissions = [RestConstants.ACCESS_WRITE];
    pinCollection.permissionsMode = HideMode.Hide;
    pinCollection.toolpermissions = [RestConstants.TOOLPERMISSION_COLLECTION_PINNING];
    pinCollection.group = DefaultGroups.Edit;
    pinCollection.priority = 20;

    const editAssignment = new OptionItem('OPTIONS.ASSIGNMENT_EDIT', 'edit', (object) =>
        service.uiService.goToAssignment(service.getObjects(object, data)[0], 'edit'),
    );
    editAssignment.elementType = [ElementType.Assignment];
    editAssignment.constrains = [Constrain.NoBulk, Constrain.User];
    editAssignment.customShowCallback = async (objects) => {
        const assignment = objects[0] as Assignment;
        // user has access to permissions => so it's a coordinator
        return (
            assignment.type === 'SUBMISSION' &&
            !['FINISHED', 'CANCELED'].includes(assignment.status) &&
            new AssignmentPipe().transform(assignment, {
                mode: 'permissions',
            }) === 'COORDINATOR'
        );
    };
    editAssignment.showAsAction = true;
    editAssignment.group = DefaultGroups.Edit;
    editAssignment.priority = 5;

    const assignAssignment = new OptionItem('OPTIONS.ASSIGNMENT_ASSIGN', 'group_add', (object) =>
        service.uiService.goToAssignment(service.getObjects(object, data)[0], 'assign'),
    );
    assignAssignment.elementType = [ElementType.Assignment];
    assignAssignment.constrains = [Constrain.NoBulk, Constrain.User];
    assignAssignment.customShowCallback = async (objects) => {
        const assignment = objects[0] as Assignment;
        // user has access to permissions => so it's a coordinator
        return (
            assignment.type === 'SUBMISSION' &&
            !['FINISHED', 'CANCELED'].includes(assignment.status) &&
            new AssignmentPipe().transform(assignment, {
                mode: 'permissions',
            }) === 'COORDINATOR'
        );
    };
    assignAssignment.group = DefaultGroups.Edit;
    assignAssignment.priority = 6;

    const finishAssignment = new OptionItem(
        'OPTIONS.ASSIGNMENT_FINISH',
        'done_all',
        async (object) => {
            const assignment = service.getObjects(object, data)[0] as Assignment;
            const dialogRef = await service.dialogs.openGenericDialog({
                title: 'OPTIONS.ASSIGNMENT_FINISH',
                message: 'OPTIONS.ASSIGNMENT_FINISH_CONFIRM',
                buttons: OK_OR_CANCEL,
            });
            dialogRef.afterClosed().subscribe((response) => {
                if (response === 'OK') {
                    service.assignmentV1Service
                        .createOrUpdateAssignment1({
                            assignmentId: assignment.ref.id,
                            status: 'FINISHED',
                        })
                        .subscribe((updated) => {
                            service.toast.show({
                                type: 'info',
                                subtype: ToastType.InfoSimple,
                                message: 'TOAST.ASSIGNMENT_FINISH',
                            });
                            service.localEvents.nodesChanged.emit([updated as any]);
                        });
                }
            });
        },
    );
    finishAssignment.elementType = [ElementType.Assignment];
    finishAssignment.constrains = [Constrain.NoBulk, Constrain.User];
    finishAssignment.customShowCallback = async (objects) => {
        const assignment = objects[0] as Assignment;
        return (
            assignment.type === 'SUBMISSION' &&
            !['FINISHED', 'CANCELED'].includes(assignment.status) &&
            new AssignmentPipe().transform(assignment, { mode: 'permissions' }) === 'COORDINATOR'
        );
    };
    finishAssignment.group = DefaultGroups.Edit;
    finishAssignment.priority = 10;

    const unblockNode = new OptionItem('OPTIONS.UNBLOCK_IMPORT', 'sync', async (object) => {
        const objects = await service.getObjectsAsync(object, data, true);
        const dialogRef = await service.dialogs.openGenericDialog({
            title: 'WORKSPACE.UNBLOCK_TITLE',
            message: 'WORKSPACE.UNBLOCK_MESSAGE',
            buttons: OK_OR_CANCEL,
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'OK') {
                service.unblockImportedNodes(objects);
            }
        });
    });
    unblockNode.elementType = [ElementType.NodeBlockedImport];
    unblockNode.constrains = [Constrain.HomeRepository, Constrain.User];
    unblockNode.permissions = [RestConstants.PERMISSION_DELETE];
    unblockNode.permissionsMode = HideMode.Hide;
    unblockNode.group = DefaultGroups.Edit;
    unblockNode.priority = 10;

    const relationNode = new OptionItem('OPTIONS.RELATIONS', 'swap_horiz', async (node) => {
        const nodes = await service.getObjectsAsync(node, data, true);
        void service.dialogs.openNodeRelationsDialog({ node: nodes[0] });
    });
    relationNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    relationNode.constrains = [Constrain.NoBulk, Constrain.User];
    relationNode.scopes = [Scope.Render];
    relationNode.toolpermissions = [RestConstants.TOOLPERMISSION_MANAGE_RELATIONS];
    relationNode.permissions = [RestConstants.PERMISSION_WRITE];
    relationNode.permissionsRightMode = NodesRightMode.Effective;
    relationNode.group = DefaultGroups.Edit;
    relationNode.priority = 70;

    return [
        editRevocation,
        inviteNode,
        streamNode,
        licenseNode,
        contributorNode,
        workflowNode,
        simpleEditNode,
        editNode,
        templateNode,
        editCollection,
        pinCollection,
        editAssignment,
        assignAssignment,
        finishAssignment,
        unblockNode,
        relationNode,
    ];
}
