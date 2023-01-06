import { Component, Input } from '@angular/core';
import { ListItem, Node } from '../../../core-module/core.module';
import { NodeEntriesDataType } from '../node-entries.component';

@Component({
    selector: 'es-drag-preview',
    templateUrl: './drag-preview.component.html',
    styleUrls: ['./drag-preview.component.scss'],
})
export class DragPreviewComponent<T extends NodeEntriesDataType> {
    @Input() node: Node;
    @Input() selected: T[];
    @Input() item: ListItem;
}
