import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Node } from '../core-module/core.module';
import * as rxjs from 'rxjs';
import {
    CanDrop,
    NodesDropTargetDirective,
} from '../shared/directives/nodes-drop-target.directive';
import { distinctUntilChanged, map, pairwise, scan } from 'rxjs/operators';
import { Toast } from '../core-ui-module/toast';

export type DropAction = 'move' | 'copy' | 'link';

export interface DragData<T = unknown> {
    draggedNodes: Node[];
    action: DropAction;
    target: T;
}

@Injectable({
    providedIn: 'root',
})
export class NodesDragDropService {
    /** The node(s) currently being dragged. */
    private draggedNodesSubject = new BehaviorSubject<Node[]>(null);
    /** The drop target that something is currently dragged above. */
    private dropTargetSubject = new BehaviorSubject<NodesDropTargetDirective>(null);
    /** Whether the current drop target allows dropping the dragged node(s). */
    private canDropSubject = new BehaviorSubject<CanDrop | null>(null);
    /** A target being hovered with the cursor. Does not mean anything is dragged yet. */
    private hoveredTargetSubject = new BehaviorSubject<NodesDropTargetDirective>(null);
    /** Current drop action based on pressed modifier keys. Updated outside ngZone. */
    private dropActionSubjectOutsideZone = new BehaviorSubject<DropAction>('move');
    private dropActionSubject = new BehaviorSubject<DropAction>('move');
    /** The current cursor style. */
    private curserSubject = new BehaviorSubject<string>(null);

    set draggedNodes(nodes: Node[]) {
        this.draggedNodesSubject.next(nodes);
    }

    get canDrop() {
        return this.canDropSubject.value;
    }

    constructor(private ngZone: NgZone, private toast: Toast) {
        this.registerModifierState();
        this.registerDropTarget();
        this.registerCanDrop();
        this.registerDropActionSubject();
        this.registerCursor();
        this.registerActiveDropTargetStyle();
        // this.draggedNodesSubject.subscribe((draggedNodes) =>
        //     console.log('draggedNodes', draggedNodes),
        // );
        // this.dropTargetSubject.subscribe((dropTarget) => console.log('dropTarget', dropTarget));
        // this.canDropSubject.subscribe((canDrop) => console.log('canDrop', canDrop));
    }

    /** Call when the cursor enters a possible drop target. Runs outside ngZone. */
    onMouseEnter(target: NodesDropTargetDirective) {
        this.hoveredTargetSubject.next(target);
    }

    /** Call when the cursor leaves a possible drop target. Runs outside ngZone. */
    onMouseLeave(target: NodesDropTargetDirective) {
        if (this.hoveredTargetSubject.value === target) {
            this.hoveredTargetSubject.next(null);
        }
    }

    onDropped(nodes: Node[]) {
        if (this.canDrop?.accept) {
            this.dropTargetSubject.value?.nodeDropped.emit({
                draggedNodes: nodes,
                action: this.dropActionSubject.value,
                target: this.dropTargetSubject.value.target,
            });
        } else if (this.canDrop?.denyReason) {
            this.toast.error(null, this.canDrop.denyReason);
        }
    }

    private registerModifierState() {
        this.ngZone.runOutsideAngular(() => {
            document.addEventListener('keydown', this.onKeyDown);
            document.addEventListener('keyup', this.onKeyUp);
        });
    }

    private registerDropTarget() {
        rxjs.combineLatest([this.draggedNodesSubject, this.hoveredTargetSubject])
            .pipe(
                map(([draggedNodes, hoveredNode]) => draggedNodes && hoveredNode),
                distinctUntilChanged(),
            )
            .subscribe((target) => {
                this.ngZone.run(() => {
                    this.dropTargetSubject.next(target);
                });
            });
    }

    private registerCanDrop() {
        rxjs.combineLatest([this.dropTargetSubject, this.dropActionSubject])
            .pipe(
                map(([dropTarget, dropAction]) =>
                    dropTarget?.canDropNodes({
                        draggedNodes: this.draggedNodesSubject.value,
                        action: dropAction,
                        target: dropTarget.target,
                    }),
                ),
                // FIXME: this won't filter equal objects
                distinctUntilChanged(),
            )
            .subscribe((canDrop) => {
                this.ngZone.run(() => this.canDropSubject.next(canDrop));
            });
    }

    private registerCursor() {
        rxjs.combineLatest([
            this.draggedNodesSubject,
            this.canDropSubject,
            this.dropActionSubject,
        ]).subscribe(([draggedNodes, canDrop, dropAction]) => {
            this.curserSubject.next(this.getCursor(draggedNodes, canDrop, dropAction));
        });
        let style: HTMLStyleElement;
        this.curserSubject.subscribe((cursor) => {
            if (cursor && !style) {
                style = document.createElement('style');
                document.body.appendChild(style);
            } else if (style && !cursor) {
                document.body.removeChild(style);
                style = null;
            }
            if (cursor) {
                style.innerHTML = `* {cursor: ${cursor} !important; }`;
            }
        });
    }

    private registerActiveDropTargetStyle() {
        rxjs.combineLatest([this.dropTargetSubject, this.canDropSubject, this.dropActionSubject])
            .pipe(pairwise())
            .subscribe(([[previous], [current, canDrop, action]]) => {
                // console.log('can drop', canDrop);
                previous?._setActiveDropTarget(null);
                current?._setActiveDropTarget({ canDrop, action });
            });
    }

    private onKeyDown = (e: KeyboardEvent) => {
        if (e.key === 'Control' || e.keyCode == 91 || e.keyCode == 93) {
            this.dropActionSubjectOutsideZone.next('copy');
        }
    };

    private onKeyUp = (e: KeyboardEvent) => {
        if (e.key === 'Control' || e.keyCode == 91 || e.keyCode == 93) {
            this.dropActionSubjectOutsideZone.next('move');
        }
    };

    private registerDropActionSubject() {
        this.dropActionSubjectOutsideZone
            .pipe(distinctUntilChanged())
            .subscribe((dropAction) =>
                this.ngZone.run(() => this.dropActionSubject.next(dropAction)),
            );
    }

    private getCursor(draggedNodes: Node[], canDrop: CanDrop, dropAction: DropAction): string {
        if (!draggedNodes) {
            return null;
        } else if (canDrop?.denyExplicit) {
            return 'no-drop';
        } else if (dropAction === 'copy') {
            return 'copy';
        } else {
            return 'grabbing';
        }
    }
}
