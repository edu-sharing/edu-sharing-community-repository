import { Node, RestConstants } from '../../core-module/core.module';
import { DragData } from '../../services/nodes-drag-drop.service';
import { CanDrop } from '../../shared/directives/nodes-drop-target.directive';

export function canDropOnNode(dragData: DragData<Node>): CanDrop {
    if (!dragData.target.isDirectory) {
        return { accept: false, denyExplicit: false, denyReason: 'WORKSPACE.TARGET_NO_DIRECTORY' };
    } else if (dragData.draggedNodes.some((node) => node.ref.id === dragData.target.ref.id)) {
        return {
            accept: false,
            denyExplicit: false,
            denyReason: 'WORKSPACE.SOURCE_TARGET_IDENTICAL',
        };
    } else if (dragData.draggedNodes.some((node) => node.parent?.id === dragData.target.ref.id)) {
        return {
            accept: false,
            denyExplicit: false,
            denyReason: 'WORKSPACE.SOURCE_TARGET_IDENTICAL',
        };
    } else if (!dragData.target.access.includes(RestConstants.ACCESS_WRITE)) {
        return {
            accept: false,
            denyExplicit: true,
            denyReason: 'WORKSPACE.TARGET_NO_WRITE_PERMISSIONS',
        };
    } else {
        return canDragDrop(dragData);
    }
}

export function canDragDrop(dragData: DragData): CanDrop {
    if (
        dragData.action === 'move' &&
        dragData.draggedNodes.some((node) => !node.access.includes(RestConstants.ACCESS_WRITE))
    ) {
        return {
            accept: false,
            denyExplicit: true,
            denyReason: 'WORKSPACE.SOURCE_NO_WRITE_PERMISSIONS',
        };
    } else if (dragData.action === 'link') {
        return {
            accept: false,
            denyExplicit: true,
            denyReason: 'WORKSPACE.FEATURE_NOT_IMPLEMENTED',
        };
    } else {
        return { accept: true };
    }
}
