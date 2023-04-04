import { Component, EventEmitter, Input, Output } from '@angular/core';
import { trigger } from '@angular/animations';
import { Node } from 'ngx-edu-sharing-api';
import { UIAnimation } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-toolList',
    templateUrl: 'tool-list.component.html',
    styleUrls: ['tool-list.component.scss'],
    animations: [
        trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger(
            'openOverlayBottom',
            UIAnimation.openOverlayBottom(UIAnimation.ANIMATION_TIME_FAST),
        ),
    ],
})
/**
 * A provider to render multiple Nodes as a list
 */
export class ToolListComponent {
    public _nodes: Node[];
    /**
     * Set the current list of nodes to render
     * @param nodes
     */
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
    }

    /**
     * Shall a "click" cursor be shown?
     * @type {boolean}
     */
    @Input() clickable = true;
    @Output() onClick = new EventEmitter();
    public click(node: Node) {
        this.onClick.emit(node);
    }
}
