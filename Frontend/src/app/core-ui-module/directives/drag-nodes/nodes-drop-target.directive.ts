import { Directive, EventEmitter, HostListener, Input, Output } from '@angular/core';
import {
    DragData,
    dragNodesTransferType,
    readDraggedNodes,
    DropAction,
    DropData,
} from './drag-nodes';
import { UIService } from '../../../core-module/core.module';

/**
 * Handle dragging and dropping of nodes onto an element.
 *
 * Use in combination with `NodesDragSourceDirective`.
 *
 * When an element that sets `NodesDragSourceDirective` is dragged onto an
 * element that sets this directive, drag events are passed as output events.
 * Some of these output events provide data about the dragged nodes. Passed
 * events are filtered to not trigger unnecessarily.
 */
@Directive({
    selector: '[esLegacyNodesDropTarget]',
})
export class NodesDropTargetDirective {
    /**
     * The last node that some (other) node(s) were dragged over.
     *
     * One of two mechanisms to determine if we entered the element, i.e., if
     * the cursor -- while dragging node(s) -- is moved from outside onto this
     * node.
     *
     * If the cursor was over another node before, we know it just entered this
     * one.
     *
     * This fails when the cursor is moved from this node to a place other than
     * a node and back again.
     */
    private static last: NodesDropTargetDirective;

    /**
     * A function to determine whether this element is a valid drop target for
     * the dragged nodes.
     *
     * Output events are *only* fired, when this function evaluates to `true`.
     */
    @Input('esLegacyNodesDropTarget') canDrop: boolean | ((dragData: DragData) => boolean);
    @Input() nodesDragAllowedActions: DropAction[] = ['move', 'copy'];

    @Output() nodesDragEnter = new EventEmitter<DragEvent>();
    @Output() nodesDragLeave = new EventEmitter<DragEvent>();
    /**
     * Indicates whether the cursor is hovering over the element, holding one or more nodes.
     */
    @Output() nodesHoveringChange = new EventEmitter<boolean>();
    /**
     * Triggered when one or more nodes are dropped onto the element.
     */
    @Output() nodesDrop = new EventEmitter<DragData>();

    /**
     * Difference of enter- and leave events.
     *
     * One of two mechanisms to determine if we entered the element, i.e., if
     * the cursor -- while dragging node(s) -- is moved from outside onto this
     * node.
     *
     * If we enter this node a second time without having left it once, we
     * probably just stayed on it.
     *
     * This fails when a leave event fails to fire, e.g., when the DOM element
     * was repositioned while the curser was dragged over it.
     */
    private enterCount = 0;
    private canDropCurrent: boolean;
    private dropAction: DropAction;

    constructor(private ui: UIService) {}

    @HostListener('dragenter', ['$event']) onDragEnter(event: DragEvent) {
        if (!event.dataTransfer.types.includes(dragNodesTransferType)) {
            return;
        }
        if (this.enterCount === 0 || NodesDropTargetDirective.last !== this) {
            NodesDropTargetDirective.last = this;
            this.enterCount = 0;
            this.canDropCurrent = this.getCanDrop(event);
            if (this.canDropCurrent) {
                this.nodesHoveringChange.emit(true);
                this.nodesDragEnter.emit(event);
            }
        }
        if (this.canDropCurrent) {
            event.preventDefault();
        }
        this.enterCount++;
    }

    @HostListener('dragleave', ['$event']) onDragLeave(event: DragEvent) {
        if (!event.dataTransfer.types.includes(dragNodesTransferType)) {
            return;
        }
        this.enterCount--;
        if (this.canDropCurrent) {
            if (this.enterCount === 0) {
                this.nodesDragLeave.emit(event);
                this.nodesHoveringChange.emit(false);
            }
        }
    }

    @HostListener('dragover', ['$event']) onDragOver(event: DragEvent) {
        if (!event.dataTransfer.types.includes(dragNodesTransferType)) {
            return;
        }
        if (this.canDropCurrent) {
            event.preventDefault();
            this.dropAction = this.getDropAction(event);
            event.dataTransfer.dropEffect = this.dropAction;
        }
    }

    @HostListener('drop', ['$event']) onDrop(event: DragEvent) {
        if (!event.dataTransfer.types.includes(dragNodesTransferType)) {
            return;
        }
        if (this.canDropCurrent) {
            event.preventDefault();
            const nodes = readDraggedNodes();
            this.nodesDrop.emit({
                event,
                nodes,
                dropAction: this.getDropAction(event),
            });
            this.nodesHoveringChange.emit(false);
        }
        this.enterCount = 0;
        NodesDropTargetDirective.last = null;
    }

    private getCanDrop(event: DragEvent): boolean {
        if (typeof this.canDrop === 'function') {
            const nodes = readDraggedNodes();
            return this.canDrop({ event, nodes, dropAction: this.getDropAction(event) });
        }
        return this.canDrop;
    }

    private getDropAction(event: DragEvent): DropAction {
        if (this.nodesDragAllowedActions.includes('copy') && event.ctrlKey) {
            return 'copy';
        } else if (this.nodesDragAllowedActions.includes('link') && event.altKey) {
            return 'link';
        } else {
            return 'move';
        }
    }
}
