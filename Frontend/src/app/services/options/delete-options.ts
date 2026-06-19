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
import { ToastType } from '../toast';
import { OptionsContext } from './options-context';

export function createDeleteOptions({
    service,
    management,
    components,
    data,
}: OptionsContext): OptionItem[] {
    const revokeNode = new OptionItem('OPTIONS.REVOKE', 'delete', async (object) => {
        await service.revokeNode(object, data);
    });
    revokeNode.constrains = [
        Constrain.Files,
        Constrain.NoBulk,
        Constrain.HomeRepository,
        Constrain.User,
    ];
    revokeNode.scopes = [Scope.Render];
    revokeNode.elementType = [ElementType.NodePublishedCopy];
    revokeNode.group = DefaultGroups.Delete;
    revokeNode.color = 'warn';
    revokeNode.priority = 10;
    revokeNode.permissions = [RestConstants.ACCESS_DELETE];
    revokeNode.permissionsRightMode = NodesRightMode.Effective;
    revokeNode.permissionsMode = HideMode.Hide;

    const removeNodeRef = new OptionItem('OPTIONS.REMOVE_REF', 'do_not_disturb_on', (object) =>
        service.removeFromCollection(service.getObjects(object, data), data),
    );
    removeNodeRef.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
    removeNodeRef.constrains = [
        Constrain.HomeRepository,
        Constrain.CollectionReference,
        Constrain.User,
    ];
    removeNodeRef.permissions = [RestConstants.PERMISSION_DELETE];
    removeNodeRef.permissionsMode = HideMode.Disable;
    removeNodeRef.scopes = [Scope.CollectionsReferences, Scope.Render];
    removeNodeRef.group = DefaultGroups.Delete;
    removeNodeRef.color = 'warn';
    removeNodeRef.priority = 20;

    const deleteNode = new OptionItem('OPTIONS.DELETE', 'delete', (object) => {
        void service.dialogs.openDeleteNodesDialog({ nodes: service.getObjects(object, data) });
    });
    deleteNode.elementType = [ElementType.Node, ElementType.SavedSearch, ElementType.MapRef];
    deleteNode.constrains = [
        Constrain.HomeRepository,
        Constrain.NoCollectionReference,
        Constrain.User,
    ];
    deleteNode.permissions = [RestConstants.PERMISSION_DELETE];
    deleteNode.permissionsMode = HideMode.Hide;
    deleteNode.keyboardShortcut = {
        keyCode: 'Delete',
    };
    deleteNode.group = DefaultGroups.Delete;
    deleteNode.color = 'warn';
    deleteNode.priority = 10;

    const cancelAssignment = new OptionItem(
        'OPTIONS.ASSIGNMENT_CANCEL',
        'cancel',
        async (object) => {
            const assignment = service.getObjects(object, data)[0] as Assignment;
            const dialogRef = await service.dialogs.openGenericDialog({
                title: 'OPTIONS.ASSIGNMENT_CANCEL',
                message: 'OPTIONS.ASSIGNMENT_CANCEL_CONFIRM',
                buttons: [
                    { label: 'CANCEL', config: { color: 'standard' } },
                    {
                        label: 'OPTIONS.ASSIGNMENT_CANCEL',
                        config: { color: 'danger' },
                    },
                ],
            });
            dialogRef.afterClosed().subscribe((response) => {
                if (response === 'OPTIONS.ASSIGNMENT_CANCEL') {
                    service.assignmentV1Service
                        .createOrUpdateAssignment1({
                            assignmentId: assignment.ref.id,
                            status: 'CANCELED',
                        })
                        .subscribe((updated) => {
                            service.toast.show({
                                type: 'info',
                                subtype: ToastType.InfoSimple,
                                message: 'TOAST.ASSIGNMENT_CANCEL',
                            });
                            service.localEvents.nodesChanged.emit([updated as any]);
                        });
                }
            });
        },
    );
    cancelAssignment.elementType = [ElementType.Assignment];
    cancelAssignment.constrains = [Constrain.NoBulk, Constrain.User];
    cancelAssignment.customShowCallback = async (objects) => {
        const assignment = objects[0] as Assignment;
        return (
            assignment.type === 'SUBMISSION' &&
            !['FINISHED', 'CANCELED'].includes(assignment.status) &&
            new AssignmentPipe().transform(assignment, { mode: 'permissions' }) === 'COORDINATOR'
        );
    };
    cancelAssignment.group = DefaultGroups.Delete;
    cancelAssignment.color = 'warn';
    cancelAssignment.priority = 10;

    return [revokeNode, removeNodeRef, deleteNode, cancelAssignment];
}
