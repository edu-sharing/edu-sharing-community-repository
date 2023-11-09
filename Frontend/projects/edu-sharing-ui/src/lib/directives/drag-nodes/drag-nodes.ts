import { Node } from 'ngx-edu-sharing-api';
import { DropAction } from '../../types/drag-drop';

export interface DragDataLegacy {
    event?: DragEvent;
    nodes?: Node[];
    dropAction?: DropAction;
}
export type DropActionLegacy = 'copy' | 'move' | 'link';

export interface DropDataLegacy extends DragDataLegacy {
    target: DragNodeTarget;
}

export type DragNodeTarget = Node | 'HOME';

export const dragNodesTransferType = 'application/nodes';

const storageKey = 'app-drag-nodes';

export function readDraggedNodes(): Node[] {
    const json = window.localStorage.getItem(storageKey);
    return JSON.parse(json);
}

export function saveDraggedNodes(nodes: Node[]): void {
    const json = JSON.stringify(nodes);
    window.localStorage.setItem(storageKey, json);
}

export function clearDraggedNodes(): void {
    window.localStorage.removeItem(storageKey);
}
