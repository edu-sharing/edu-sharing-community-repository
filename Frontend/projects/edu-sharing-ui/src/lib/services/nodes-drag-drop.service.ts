import { Injectable, NgZone } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, map, pairwise } from 'rxjs/operators';
import { CanDrop, DropAction } from '../types/drag-drop';
import { NodesDropTargetLegacyDirective } from '../directives/nodes-drop-target-legacy.directive';
import { NodesDropTargetDirective } from '../directives/drag-nodes/nodes-drop-target.directive';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from './abstract/toast.service';
import { UIService } from './ui.service';

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
    /** Current drop action based on pressed modifier keys. */
    private dropActionSubject = new BehaviorSubject<DropAction>('move');
    /** The current cursor style. */
    private curserSubject = new BehaviorSubject<string>(null);

    set draggedNodes(nodes: Node[]) {
        this.draggedNodesSubject.next(nodes);
    }

    get canDrop() {
        return this.canDropSubject.value;
    }

    constructor(private ngZone: NgZone, private toast: Toast, private uiService: UIService) {
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

    private registerDropActionSubject() {
        this.uiService
            .observeCtrlOrCmdKeyPressedOutsideZone()
            .pipe(map((ctrlOrCmdPressed) => this.getDropAction(ctrlOrCmdPressed)))
            .subscribe((dropAction) =>
                this.ngZone.run(() => this.dropActionSubject.next(dropAction)),
            );
    }

    private getDropAction(ctrlOrCmdPressed: boolean): DropAction {
        if (ctrlOrCmdPressed) {
            return 'copy';
        } else {
            return 'move';
        }
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
