import {Directive, ElementRef, HostListener} from '@angular/core';
import {dragNodesTransferType, readDraggedNodes} from './drag-nodes/drag-nodes';
import {ConfigurationService} from '../../core-module/rest/services/configuration.service';

@Directive({
    selector: '[appNodesEntriesDrag]',
})
export class NodeEntriesDragDirective {
    static last: NodeEntriesDragDirective;
    private canDropCurrent: boolean;
    private element: ElementRef;
    constructor(element: ElementRef) {
        this.element = element;
        console.log(this.element);
        this.element.nativeElement.draggable = true;
    }
    @HostListener('dragenter', ['$event']) onDragEnter(event: DragEvent) {
        if (!event.dataTransfer.types.includes(dragNodesTransferType)) {
            return;
        }
        NodeEntriesDragDirective.last = this;
        this.setActive(true);
        event.preventDefault();
    }

    @HostListener('dragleave', ['$event']) onDragLeave(event: DragEvent) {
        this.setActive(false);
    }

    @HostListener('drop', ['$event']) onDrop(event: DragEvent) {

    }

    private setActive(active: boolean) {
        if(active) {
            this.element.nativeElement.className.add('node-entries-drag-target');
        } else {
            this.element.nativeElement.className.remove('node-entries-drag-target');
        }
    }

    }

