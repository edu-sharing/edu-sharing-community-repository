import { Node } from "../../../core-module/core.module";

export type DropAction = 'move' | 'copy' | 'link';

export interface DragData {
    event: DragEvent;
    nodes: Node[];
}

export interface DropData extends DragData {
    dropAction: DropAction;
}

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