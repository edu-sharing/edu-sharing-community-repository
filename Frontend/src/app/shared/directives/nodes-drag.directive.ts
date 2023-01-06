import { CdkDrag } from '@angular/cdk/drag-drop';
import { Directive } from '@angular/core';
import { Node } from '../../core-module/core.module';
import { NodesDragDropService } from '../../services/nodes-drag-drop.service';

/**
 * A draggable node.
 *
 * Use in combination with `cdkDrag`, setting `cdkDragData` to the array of nodes to be dragged.
 */
@Directive({
    selector: '[esNodesDrag]',
})
export class NodesDragDirective {
    constructor(private cdkDrag: CdkDrag<Node[]>, private nodesDragDrop: NodesDragDropService) {
        this.cdkDrag.started.subscribe((event) => {
            this.nodesDragDrop.draggedNodes = event.source.data;
            // Position the preview element (the one being dragged around) next to the cursor to
            // avoid covering possible drop targets with the preview.
            event.source._dragRef['_pickupPositionInElement'] = { x: 0, y: 0 };
        });
        this.cdkDrag.released.subscribe(() => {
            if (this.nodesDragDrop.canDrop?.accept) {
                this.inhibitPreviewAnimation();
            }
        });
        this.cdkDrag.dropped.subscribe(() => {
            this.nodesDragDrop.onDropped(this.cdkDrag.data);
            this.nodesDragDrop.draggedNodes = null;
        });
    }

    private inhibitPreviewAnimation() {
        const style = document.createElement('style');
        document.body.appendChild(style);
        style.innerHTML = `.cdk-drag-preview { transition: none !important; }`;
        // Don't need to go outside ng zone because `cdkDrag.released` already runs outside the
        // zone.
        setTimeout(() => {
            document.body.removeChild(style);
        });
    }
}
