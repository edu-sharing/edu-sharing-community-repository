import {
    Component,
    Input,
    Output,
    EventEmitter,
    ChangeDetectionStrategy,
    ApplicationRef,
    ChangeDetectorRef,
    ViewChild,
    ElementRef,
    HostListener,
    ViewEncapsulation,
} from '@angular/core';
import { BrowserModule, DomSanitizer } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { trigger } from '@angular/animations';
import { Node } from '../../../core-module/core.module';

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
