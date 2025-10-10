import { Node } from 'ngx-edu-sharing-api';

export type DropAction = 'move' | 'copy' | 'link';

export interface DragData<T = unknown> {
    draggedNodes: Node[];
    action: DropAction;
    target: T;
    /**
     * contains true if the event was inside the own view (i.e. the same grid view), false if it was an other view
     * might be null if it is unknown
     */
    isFromOwnContainer?: boolean;
}
export interface DropTargetState {
    action: DropAction;
    canDrop: CanDrop;
}

export interface CanDrop {
    /** Whether the target is a valid drop target for the dragged nodes and the given action. */
    accept: boolean;
    /** When denied, whether to explicitly mark the target when hovered. */
    denyExplicit?: boolean;
    /** A message to show when tried to drop on a denied target. */
    denyReason?: string;
}
