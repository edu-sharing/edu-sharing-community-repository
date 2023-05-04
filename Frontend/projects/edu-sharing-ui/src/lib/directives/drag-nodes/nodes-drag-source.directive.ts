import {
    Directive,
    EventEmitter,
    HostListener,
    Input,
    Output,
    ElementRef,
    OnChanges,
    SimpleChanges,
} from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { clearDraggedNodes, dragNodesTransferType, saveDraggedNodes } from './drag-nodes';

/**
 * Handle dragging and dropping of node elements.
 *
 * Use in combination with `NodesDropTargetDirective`.
 *
 * When nodes that set this directive are dragged, we save information about
 * these nodes that can be used by `NodesDropTargetDirective`.
 */
@Directive({
    selector: '[esNodesDragSource]',
})
export class NodesDragSourceDirective implements OnChanges {
    /**
     * The nodes to be dragged.
     *
     * When not set, this directive is effectively disabled.
     */
    @Input('esNodesDragSource') nodes?: Node[];

    /**
     * Triggered when processing the dragstart event.
     *
     * Changes to `nodes` performed by event handlers of `nodesDragStart` will
     * be taken into account for the drag operation.
     */
    @Output() nodesDragStart = new EventEmitter<DragEvent>(false);
    /**
     * Triggered when processing the dragend event.
     */
    @Output() nodesDragEnd = new EventEmitter<DragEvent>();

    constructor(private elementRef: ElementRef<Element>) {}

    ngOnChanges(changes: SimpleChanges) {
        // Set the `draggable` attribute when this directive is active.
        if (changes.nodes) {
            if (!!changes.nodes.currentValue !== !!changes.nodes.previousValue) {
                this.elementRef.nativeElement.setAttribute('draggable', (!!this.nodes).toString());
            }
        }
    }

    @HostListener('dragstart', ['$event']) onDragStart(event: DragEvent) {
        if (!this.nodes) {
            return;
        }
        this.nodesDragStart.emit(event);
        // Use the transfer-data type to identify a node-drag operation.
        event.dataTransfer.setData(dragNodesTransferType, '');
        // Chrome doesn't provide drag transfer data to dragover event listeners
        // for security reasons, so we provide the data via localStorage.
        //
        // Wait for updates by event handlers of `nodesDragStart` to propagate.
        setTimeout(() => {
            saveDraggedNodes(this.nodes);
        });
    }

    @HostListener('dragend', ['$event']) onDragEnd(event: DragEvent) {
        if (!this.nodes) {
            return;
        }
        this.nodesDragEnd.emit(event);
        clearDraggedNodes();
    }
}
